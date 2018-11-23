package com.kedacom.vconf.sdk.datacollaborate;

import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.os.Handler;

import com.kedacom.vconf.sdk.base.IResponseListener;
import com.kedacom.vconf.sdk.base.Msg;
import com.kedacom.vconf.sdk.base.MsgBeans;
import com.kedacom.vconf.sdk.base.RequestAgent;
import com.kedacom.vconf.sdk.base.CommonResultCode;
import com.kedacom.vconf.sdk.base.KLog;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCMember;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpClearScreen;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDeletePic;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawLine;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawPath;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawRect;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpErase;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpInsertPic;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpMatrix;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDragPic;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpUpdatePic;
import com.kedacom.vconf.sdk.datacollaborate.bean.PaintBoardInfo;
import com.kedacom.vconf.sdk.datacollaborate.bean.PaintCfg;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpPaint;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawOval;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpRedo;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpUndo;
import com.kedacom.vconf.sdk.datacollaborate.bean.TerminalType;

//import static com.kedacom.vconf.sdk.base.MsgBeans.*; // TODO 使用static import？

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;


public class DataCollaborateManager extends RequestAgent {

    // 是否正在接收批量上报的操作
    private boolean isRecvingBatchOps = false;

    /*批量操作缓存。
    批量上报的操作到达的时序可能跟操作的实际时序不相符，平台特意为此加了序列号字段，
    此处我们使用PriorityQueue结合该序列号字段来为我们自动排序。*/
    private PriorityQueue<OpPaint> batchOps = new PriorityQueue<>();

    //当前数据协作会议的e164
    private String curDcConfE164;

    private static final Msg[] boardOpNtfs = new Msg[]{
            Msg.DCCurrentPaintBoardNtf,
            Msg.DCNewPaintBoardNtf,
            Msg.DCSwitchPaintBoardNtf,
            Msg.DCDelPaintBoardNtf,
    };
    private static final Msg[] paintOpNtfs = new Msg[]{
            Msg.DCElementBeginNtf,
            Msg.DCLineDrawnNtf,
            Msg.DCOvalDrawnNtf,
            Msg.DCRectDrawnNtf,
            Msg.DCPathDrawnNtf,
            Msg.DCPicInsertedNtf,
            Msg.DCPicDraggedNtf,
            Msg.DCPicDeletedNtf,
            Msg.DCRectErasedNtf,
            Msg.DCFullScreenMatrixOpNtf,
            Msg.DCUndoneNtf,
            Msg.DCRedoneNtf,
            Msg.DCScreenClearedNtf,
            Msg.DCElementEndNtf,
    };

    @Override
    protected Map<Msg, RspProcessor> rspProcessors() {
        Map<Msg, RspProcessor> processorMap = new HashMap<>();
        processorMap.put(Msg.DCLogin, this::onSessionRsps);
        processorMap.put(Msg.DCLogout, this::onSessionRsps);

        processorMap.put(Msg.DCCreateConf, this::onDcConfLifecycleRsps);
        processorMap.put(Msg.DCReleaseConf, this::onDcConfLifecycleRsps);
        processorMap.put(Msg.DCQuitConf, this::onDcConfLifecycleRsps);

        processorMap.put(Msg.DCAddOperator, this::onChangeOperatorsRsps);
        processorMap.put(Msg.DCDelOperator, this::onChangeOperatorsRsps);
        processorMap.put(Msg.DCApplyOperator, this::onChangeOperatorsRsps);
        processorMap.put(Msg.DCCancelOperator, this::onChangeOperatorsRsps);

        processorMap.put(Msg.DCDownload, this::onDownloadRsp);
        processorMap.put(Msg.DCQueryPicUrl, this::onQueryPicUrlRsp);
        return processorMap;
    }

    @Override
    protected Map<Msg, NtfProcessor> ntfProcessors() {
        Map<Msg, NtfProcessor> processorMap = new HashMap<>();
        processorMap.put(Msg.DCApplyOperatorNtf, this::onOperatorsChangedNtfs);
        return processorMap;
    }

    @Override
    protected Map<Msg[], NtfProcessor> ntfsProcessors() {
        Map<Msg[], NtfProcessor> processorMap = new HashMap<>();
        processorMap.put(boardOpNtfs, this::onBoardNtfs);
        processorMap.put(paintOpNtfs, this::onPaintNtfs);
        return processorMap;
    }

    /**发送绘制操作*/
    public void postPaintOp(OpPaint op){
//        req();
    }

    /**登录数据协作*/
    public void login(String serverIp, int port, TerminalType terminalType, IResponseListener resultListener){
        req(Msg.DCLogin, new MsgBeans.DCLoginPara(serverIp, port, terminalType.convert()), resultListener);
    }

    /**注销数据协作*/
    public void logout(IResponseListener resultListener){
        req(Msg.DCLogout, null, resultListener);
    }

    private void onSessionRsps(Msg rspId, Object rspContent, IResponseListener listener){
        KLog.p("rspId=%s, rspContent=%s, listener=%s",rspId, rspContent, listener);
        if (Msg.DCBuildLink4LoginRsp.equals(rspId)){
            MsgBeans.CommonResult linkCreationResult = (MsgBeans.CommonResult) rspContent;
            if (!linkCreationResult.success
                    && null != listener){
                cancelReq(Msg.DCLogin, listener);  // 后续不会有DCLoginRsp上来，取消该请求以防等待超时。
                listener.onResponse(CommonResultCode.FAILED, null);
            }
        }else if (Msg.DCLoginRsp.equals(rspId)){
            MsgBeans.CommonResult loginRes = (MsgBeans.CommonResult) rspContent;
            if (null != listener){
                if (loginRes.success) {  // ??? 需要保存登录状态吗
                    listener.onResponse(CommonResultCode.SUCCESS, null);
                }else{
                    listener.onResponse(CommonResultCode.FAILED, null);
                }
            }
        }else if (Msg.DCLogoutRsp.equals(rspId)){
            MsgBeans.CommonResult result = (MsgBeans.CommonResult) rspContent;
            if (null != listener){
                if (result.success){
                    listener.onResponse(CommonResultCode.SUCCESS, rspContent);
                }else{
                    listener.onResponse(CommonResultCode.FAILED, rspContent);
                }
            }
        }
    }



    /**创建数据协作*/
    public void createDcConf(IResponseListener resultListener){
        req(Msg.DCCreateConf, new MsgBeans.DCCreateConfPara(), resultListener);
    }

    /**结束数据协作*/
    public void releaseDcConf(IResponseListener resultListener){
        req(Msg.DCReleaseConf, new MsgBeans.DCConfId(curDcConfE164), resultListener);
        curDcConfE164 = null;
    }

    /**退出数据协作。
     * 注：仅自己退出，协作仍存在，不影响其他人继续*/
    public void quitDcConf(IResponseListener resultListener){
        req(Msg.DCQuitConf, new MsgBeans.DCSQuitConf(curDcConfE164), resultListener);
        curDcConfE164 = null;
    }

    private void onDcConfLifecycleRsps(Msg rspId, Object rspContent, IResponseListener listener){
//        if (Msg.DCBuildLink4ConfRsp.equals(rspId)){
//            MsgBeans.CommonResult dcsConfResult = (MsgBeans.CommonResult) rspContent;
//            if (!dcsConfResult.success
//                    && null != listener){
//                cancelReq(Msg.DCCreateConf, listener);  // 后续不会有DCCreateConfRsp上来，取消该请求以防等待超时。
//                listener.onResponse(CommonResultCode.FAILED, null);
//            }
//        }else if (Msg.DCCreateConfRsp.equals(rspId)){
//            MsgBeans.DCCreateConfResult confResult = (MsgBeans.DCCreateConfResult) rspContent;
//            curDcConfE164 = confResult.confE164;
//            if (null != listener){
//                if (confResult.commonResult.success) {
//                    listener.onResponse(CommonResultCode.SUCCESS, null);
//                }else{
//                    listener.onResponse(CommonResultCode.FAILED, null);
//                }
//            }
//        }else if (Msg.DCReleaseConfRsp.equals(rspId)){
//            MsgBeans.CommonResult result = (MsgBeans.CommonResult) rspContent;
//            if (null != listener){
//                if (result.success){
//                    listener.onResponse(CommonResultCode.SUCCESS, rspContent);
//                }else{
//                    listener.onResponse(CommonResultCode.FAILED, rspContent);
//                }
//            }
//        }else if (Msg.DCQuitConfRsp.equals(rspId)){
//            MsgBeans.CommonResult result = (MsgBeans.CommonResult) rspContent;
//            if (null != listener){
//                if (result.success){
//                    listener.onResponse(CommonResultCode.SUCCESS, rspContent);
//                }else{
//                    listener.onResponse(CommonResultCode.FAILED, rspContent);
//                }
//            }
//        }
    }



    /**添加协作方*/
    public void addOperator(DCMember[] members, IResponseListener resultListener){
        if (null == curDcConfE164) {
            KLog.p(KLog.ERROR,"not in DC conf yet!");
            return;
        }
//        MsgBeans.TDCSConfUserInfo[] confUserInfos = new MsgBeans.TDCSConfUserInfo[members.length];
//        for (int i=0; i<members.length; ++i){
//            confUserInfos[i] = members[i].convert();
//        }
//        req(Msg.DCAddOperator, new MsgBeans.TDCSOperator(curDcConfE164, confUserInfos), resultListener);
    }

    /**删除协作方*/
    public void delOperator(DCMember[] members, IResponseListener resultListener){
        if (null == curDcConfE164) {
            KLog.p(KLog.ERROR,"not in DC conf yet!");
            return;
        }
//        MsgBeans.TDCSConfUserInfo[] confUserInfos = new MsgBeans.TDCSConfUserInfo[members.length];
//        for (int i=0; i<members.length; ++i){
//            confUserInfos[i] = members[i].convert();
//        }
//        req(Msg.DCDelOperator, new MsgBeans.TDCSOperator(curDcConfE164, confUserInfos), resultListener);
    }

    /**申请协作方*/
    public void applyForOperator(String e164, IResponseListener resultListener){
        if (null == curDcConfE164) {
            KLog.p(KLog.ERROR,"not in DC conf yet!");
            return;
        }
//        req(Msg.DCApplyOperator, new MsgBeans.DCSBriefConfInfo(e164), resultListener);
    }
    /**取消协作方*/
    public void cancelOperator(String e164, IResponseListener resultListener){
        if (null == curDcConfE164) {
            KLog.p(KLog.ERROR,"not in DC conf yet!");
            return;
        }
//        req(Msg.DCCancelOperator, new MsgBeans.DCSBriefConfInfo(e164), resultListener);
    }

    private void onChangeOperatorsRsps(Msg rspId, Object rspContent, IResponseListener listener){
        MsgBeans.CommonResult result = (MsgBeans.CommonResult) rspContent;
        if (null != listener){
            if (result.success){
                listener.onResponse(CommonResultCode.SUCCESS, rspContent);
            }else{
                listener.onResponse(CommonResultCode.FAILED, rspContent);
            }
        }
    }

    private void onOperatorsChangedNtfs(Msg ntfId, Object ntfContent, Set<Object> listeners){

        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
    }


    private void onDownloadRsp(Msg rspId, Object rspContent, IResponseListener listener){
//        MsgBeans.DownloadResult result = (MsgBeans.DownloadResult) rspContent;
//        if (!result.commonResult.success){
//            KLog.p(KLog.ERROR, "download file failed!");
//            return;
//        }
//        if (result.bElement){
//            /* 下载的是图元（而非图片本身）。
//            * 后续会批量上报当前画板已有的图元：
//            * DcsElementOperBegin_Ntf // 批量上报开始
//            * ...  // 批量图元，如画线、画圆、插入图片等等
//            *
//            * ...ElementShouldAfterFinal  // 应该在该批次图元之后出现的图元。
//            *                                NOTE: 批量上报过程中可能会混入画板中当前正在进行的操作，
//            *                                这会导致图元上报的时序跟实际时序不一致，需要自行处理。
//            *                                （实际时序应该是：在成功响应下载请求后的所有后续进行的操作都应出现在DcsElementOperFinal_Ntf之后）
//            * ...// 批量图元
//            * DcsElementOperFinal_Ntf // 批量结束
//            *
//            *
//            * */
//            return;
//        }
//
//        // 下载的是图片
//        OpPaint op = new OpUpdatePic(result.boardId, result.picId, BitmapFactory.decodeFile(result.picSavePath));
//        IOnPaintOpListener onPaintOpListener = ((DownloadListener)listener).onPaintOpListener;
//        KLog.p("download pic finished, onPaintOpListener=%s", onPaintOpListener);
//        if (null != onPaintOpListener){
//            onPaintOpListener.onPaintOp(op);  // 前面我们插入图片的操作并无实际效果，因为图片是“置空”的，此时图片已下载完成，我们更新之前置空的图片。
//        }
    }


    private void onQueryPicUrlRsp(Msg rspId, Object rspContent, IResponseListener listener){
//        MsgBeans.DCQueryPicUrlResult result = (MsgBeans.DCQueryPicUrlResult) rspContent;
//        if (!result.commonResult.success){
//            return;
//        }
//
//        // 下载图片文件
//        req(Msg.DCDownload,
//                new MsgBeans.DownloadPara(result.boardId, result.picId, "/data/local/tmp/"+result.picId+".jpg", result.url),
//                listener);
    }


    private void onBoardNtfs(Msg ntfId, Object ntfContent, Set<Object> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        IOnBoardOpListener boardLifecycleListener;
        for (Object listener : listeners) {
            boardLifecycleListener = (IOnBoardOpListener) listener;
            if (Msg.DCCurrentPaintBoardNtf.equals(ntfId)) {
                MsgBeans.DCPaintBoard boardInfo = (MsgBeans.DCPaintBoard) ntfContent;
                boardLifecycleListener.onBoardSwitched(boardInfo.id);
            } else if (Msg.DCNewPaintBoardNtf.equals(ntfId)) {
                MsgBeans.DCPaintBoard boardInfo = (MsgBeans.DCPaintBoard) ntfContent;
                boardLifecycleListener.onBoardCreated(new PaintBoardInfo(boardInfo.id, boardInfo.name));
            } else if (Msg.DCSwitchPaintBoardNtf.equals(ntfId)) {
                MsgBeans.DCPaintBoard boardInfo = (MsgBeans.DCPaintBoard) ntfContent;
                boardLifecycleListener.onBoardSwitched(boardInfo.id);
                // 下载当前画板已有的图元操作。
                /* NOTE:对于下载下来的图片相关的操作，如插入图片、删除图片等，并不包含图片文件本身。
                要获取图片文件本身，需在后续专门下载。*/
                req(Msg.DCDownload,
                        new MsgBeans.DownloadPara(boardInfo.id, boardInfo.elementUrl),
                        null);  // TODO DcsSwitch_Ntf可以有多次，而下载只应该一次。在DcsCurrentWhiteBoard_Ntf中做？实测抓消息。
            } else if (Msg.DCDelPaintBoardNtf.equals(ntfId)) {
                // TODO
            }
        }
    }

    private Handler handler = new Handler();
    private final Runnable batchOpTimeout = () -> {
        KLog.p(KLog.ERROR,"wait batch paint ops timeout <<<<<<<<<<<<<<<<<<<<<<<");
        Set<Object> listeners = getNtfListeners(Msg.DCElementBeginNtf);
        OpPaint op;
        while (!batchOps.isEmpty()) {
            op = batchOps.poll();
            for (Object listener : listeners) {
                ((IOnPaintOpListener)listener).onPaintOp(op);
            }
        }

        isRecvingBatchOps = false;
    };
    @SuppressWarnings("ConstantConditions")
    private void onPaintNtfs(Msg ntfId, Object ntfContent, Set<Object> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);

        for (Object listener : listeners) {
            OpPaint paintOp = null;
            if (Msg.DCElementBeginNtf.equals(ntfId)) {
                if (isRecvingBatchOps) {  // TODO 多个画板切换会有多个DcsElementOperBegin_Ntf，此处应该要分boardId处理；另外在DcsElementOperBegin_Ntf之前就有可能有新的操作过来，或许要在开始download的时候就开启isRecvingBatchOps
                    return;
                }
                KLog.p("batch paint ops >>>>>>>>>>>>>>>>>>>>>>");
                isRecvingBatchOps = true;
                handler.postDelayed(batchOpTimeout, 10000); // 起定时器防止final消息不到。
                return;
            }
//            else if (Msg.DCLineDrawnNtf.equals(ntfId)) {
//                MsgBeans.DCLineOp Op = (MsgBeans.DCLineOp) ntfContent;
//                paintOp = new OpDrawLine(Op.startX, Op.startY, Op.stopX, Op.stopY, Op.commonInfo.sn,
//                        new PaintCfg(Op.paintCfg.strokeWidth, Op.paintCfg.color), Op.commonInfo.boardId);
//            } else if (Msg.DCOvalDrawnNtf.equals(ntfId)) {
//                MsgBeans.DCOvalOp Op = (MsgBeans.DCOvalOp) ntfContent;
//                paintOp = new OpDrawOval(Op.left, Op.top, Op.right, Op.bottom,Op.commonInfo.sn,
//                        new PaintCfg(Op.paintCfg.strokeWidth, Op.paintCfg.color), Op.commonInfo.boardId);
//            }
//            else if (Msg.DcsOperRectangleOperInfo_Ntf.equals(ntfId)) {
//                MsgBeans.DcsOperRectangleOperInfo_Ntf opInfo = (MsgBeans.DcsOperRectangleOperInfo_Ntf) ntfContent;
//                MsgBeans.TDCSOperContent commonInfo = opInfo.MainParam;
//                MsgBeans.TDCSWbRectangle gp = opInfo.AssParam.tRectangle;
//                paintOp = new OpDrawRect(gp.tBeginPt.nPosx, gp.tBeginPt.nPosy, gp.tEndPt.nPosx, gp.tEndPt.nPosy,
//                        commonInfo.dwMsgSequence, new PaintCfg(gp.dwLineWidth, (int) gp.dwRgb), commonInfo.achTabId);
//            } else if (Msg.DcsOperPencilOperInfo_Ntf.equals(ntfId)) {
//                MsgBeans.DcsOperPencilOperInfo_Ntf opInfo = (MsgBeans.DcsOperPencilOperInfo_Ntf) ntfContent;
//                MsgBeans.TDCSOperContent commonInfo = opInfo.MainParam;
//                MsgBeans.TDCSWbPencil gp = opInfo.AssParam.tPencil;
//                MsgBeans.TDCSWbPoint[] pl = gp.atPList;
//                PointF[] points = new PointF[pl.length];
//                if (0 == points.length) {
//                    return;
//                }
//                for (int i = 0; i < points.length; ++i) {
//                    points[i] = new PointF(pl[i].nPosx, pl[i].nPosy);
//                }
//                paintOp = new OpDrawPath(points, commonInfo.dwMsgSequence, new PaintCfg(gp.dwLineWidth, (int) gp.dwRgb), commonInfo.achTabId);
//            } else if (Msg.DcsOperInsertPic_Ntf.equals(ntfId)) {
//                MsgBeans.DcsOperInsertPic_Ntf opInfo = (MsgBeans.DcsOperInsertPic_Ntf) ntfContent;
//                MsgBeans.TDCSOperContent commonInfo = opInfo.MainParam;
//                MsgBeans.TDCSWbInsertPicOperInfo gp = opInfo.AssParam;
//
//                paintOp = new OpInsertPic(gp.achImgId,
//                        null/*BitmapFactory.decodeFile("/data/local/tmp/wb.png")*/, // NOTE: 此时图片还未下载到本地，先置空，等下载完成后再更新
//                        gp.dwImgWidth, gp.dwImgHeight,
//                        gp.tPoint.nPosx, gp.tPoint.nPosy, gp.aachMatrixValue, commonInfo.dwMsgSequence, commonInfo.achTabId);
//
//                // 获取图片下载地址（然后再下载图片）
//                KLog.p("start download pic, onPaintOpListener=%s", listener);
//                req(Msg.DCQueryPicUrl,
//                        new MsgBeans.TDCSImageUrl(commonInfo.achConfE164, commonInfo.achTabId, gp.achImgId),
//                        new DownloadListener((IOnPaintOpListener) listener));
//
//            } else if (Msg.DcsOperPitchPicDrag_Ntf.equals(ntfId)) {
//                MsgBeans.DcsOperPitchPicDrag_Ntf opInfo = (MsgBeans.DcsOperPitchPicDrag_Ntf) ntfContent;
//                MsgBeans.TDCSOperContent commonInfo = opInfo.MainParam;
//                MsgBeans.TDCSWbPitchPicOperInfo gp = opInfo.AssParam;
//                Map<String, float[]> picsMatrix = new HashMap<>();
//                for (MsgBeans.TDCSWbGraphsInfo picInfo : gp.atGraphsInfo){
//                    float[] matrixValue = new float[9];
//                    for (int i=0; i<picInfo.aachMatrixValue.length; ++i){
//                        matrixValue[i] = Float.valueOf(picInfo.aachMatrixValue[i]);
//                    }
//                    picsMatrix.put(picInfo.achGraphsId, matrixValue);
//                }
//                paintOp = new OpDragPic(picsMatrix, commonInfo.dwMsgSequence, commonInfo.achTabId);
//            } else if (Msg.DcsOperPitchPicDel_Ntf.equals(ntfId)) {
//                MsgBeans.DcsOperPitchPicDel_Ntf opInfo = (MsgBeans.DcsOperPitchPicDel_Ntf) ntfContent;
//                MsgBeans.TDCSOperContent commonInfo = opInfo.MainParam;
//                MsgBeans.TDCSWbDelPicOperInfo gp = opInfo.AssParam;
//                paintOp = new OpDeletePic(gp.achGraphsId, commonInfo.dwMsgSequence, commonInfo.achTabId);
//            } else if (Msg.DcsOperEraseOperInfo_Ntf.equals(ntfId)) {
//                MsgBeans.DcsOperEraseOperInfo_Ntf opInfo = (MsgBeans.DcsOperEraseOperInfo_Ntf) ntfContent;
//                MsgBeans.TDCSWbEraseOperInfo gp = opInfo.AssParam;
//                MsgBeans.TDCSOperContent commonInfo = opInfo.MainParam;
//                paintOp = new OpErase(gp.tBeginPt.nPosx, gp.tBeginPt.nPosy, gp.tEndPt.nPosx, gp.tEndPt.nPosy, commonInfo.dwMsgSequence, commonInfo.achTabId);
//            } else if (Msg.DcsOperFullScreen_Ntf.equals(ntfId)) {
//                MsgBeans.DcsOperFullScreen_Ntf opInfo = (MsgBeans.DcsOperFullScreen_Ntf) ntfContent;
//                MsgBeans.TDCSWbDisPlayInfo gp = opInfo.AssParam;
//                MsgBeans.TDCSOperContent commonInfo = opInfo.MainParam;
//                float[] matrixValue = new float[9];
//                for (int i=0; i<gp.aachMatrixValue.length; ++i){
//                    matrixValue[i] = Float.valueOf(gp.aachMatrixValue[i]);
//                }
//                paintOp = new OpMatrix(matrixValue, commonInfo.dwMsgSequence, commonInfo.achTabId);
//            } else if (Msg.DcsOperUndo_Ntf.equals(ntfId)) {
//                MsgBeans.DcsOperUndo_Ntf opInfo = (MsgBeans.DcsOperUndo_Ntf) ntfContent;
//                MsgBeans.TDCSOperContent commonInfo = opInfo.MainParam;
//                paintOp = new OpUndo(commonInfo.dwMsgSequence, commonInfo.achTabId);
//            } else if (Msg.DcsOperRedo_Ntf.equals(ntfId)) {
//                MsgBeans.DcsOperRedo_Ntf opInfo = (MsgBeans.DcsOperRedo_Ntf) ntfContent;
//                MsgBeans.TDCSOperContent commonInfo = opInfo.MainParam;
//                paintOp = new OpRedo(commonInfo.dwMsgSequence, commonInfo.achTabId);
//            } else if (Msg.DcsOperClearScreen_Ntf.equals(ntfId)) {
//                MsgBeans.TDCSOperContent opInfo = (MsgBeans.TDCSOperContent) ntfContent;
//                paintOp = new OpClearScreen(opInfo.dwMsgSequence, opInfo.achTabId);
//            }
            else if (Msg.DCElementEndNtf.equals(ntfId)) {
                if (!isRecvingBatchOps) {
                    return;
                }
                KLog.p("batch paint ops <<<<<<<<<<<<<<<<<<<<<<<");
                handler.removeCallbacks(batchOpTimeout);
                while (!batchOps.isEmpty()) {
                    ((IOnPaintOpListener)listener).onPaintOp(batchOps.poll());
                }
                isRecvingBatchOps = false;
                return;
            }

            if (isRecvingBatchOps) {
                batchOps.offer(paintOp);
            } else {
                if (null != paintOp)
                    ((IOnPaintOpListener)listener).onPaintOp(paintOp);
            }

        }
    }







    public interface IOnPaintOpListener{
        void onPaintOp(OpPaint op);
    }

    public interface IOnBoardOpListener {
        void onBoardCreated(PaintBoardInfo boardInfo);
        void onBoardDeleted(String boardId);
        void onBoardSwitched(String boardId);
    }


    public void addBoardOpListener(IOnBoardOpListener onBoardOpListener){
        subscribe(boardOpNtfs, onBoardOpListener);
    }


    public void addPaintOpListener(IOnPaintOpListener onPaintOpListener){
        subscribe(paintOpNtfs, onPaintOpListener);
    }


    private class DownloadListener implements IResponseListener{
        private IOnPaintOpListener onPaintOpListener;
        DownloadListener(IOnPaintOpListener onPaintOpListener){
            this.onPaintOpListener = onPaintOpListener;
        }

        @Override
        public void onResponse(int i, Object o) {

        }
    }

    public void ejectNtf(Msg msg){
//        if (!BuildConfig.DEBUG){
//            return;
//        }
        eject(Msg.DCApplyOperatorNtf);
        eject(msg);
    }


}
