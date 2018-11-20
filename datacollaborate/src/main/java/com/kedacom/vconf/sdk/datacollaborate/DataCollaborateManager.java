package com.kedacom.vconf.sdk.datacollaborate;

import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.os.Handler;

import com.kedacom.vconf.sdk.base.IResponseListener;
import com.kedacom.vconf.sdk.base.Msg;
import com.kedacom.vconf.sdk.base.MsgBeans;
import com.kedacom.vconf.sdk.base.MsgConst;
import com.kedacom.vconf.sdk.base.RequestAgent;
import com.kedacom.vconf.sdk.base.CommonResultCode;
import com.kedacom.vconf.sdk.base.KLog;
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

//import static com.kedacom.vconf.sdk.base.MsgBeans.*; // TODO 使用static import？

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;


public class DataCollaborateManager extends RequestAgent {

    // 终端类型
    public static final int Terminal_TrueLinkPc = 1; // 致邻PC版
    public static final int Terminal_TrueLinkIosPhone = 2; // 致邻IOS手机版
    public static final int Terminal_TrueLinkIosPad = 3; // 致邻IOS平板版
    public static final int Terminal_TrueLinkAndroidPhone = 4; // 致邻Android手机版
    public static final int Terminal_TrueLinkAndroidPad = 5; // 致邻Android平板版
    public static final int Terminal_TrueSens = 6; // 硬终端
    public static final int Terminal_Imix = 7; // 网呈IMIX
    public static final int Terminal_Other = 8; // 其他终端

    // 是否正在接收批量上报的操作
    private boolean isRecvingBatchOps = false;

    /*批量操作缓存。
    批量上报的操作到达的时序可能跟操作的实际时序不相符，平台特意为此加了序列号字段，
    此处我们使用PriorityQueue结合该序列号字段来为我们自动排序。*/
    private PriorityQueue<OpPaint> batchOps = new PriorityQueue<>();

    @Override
    protected Map<Msg, RspProcessor> rspProcessors() {
        Map<Msg, RspProcessor> processorMap = new HashMap<>();
        processorMap.put(Msg.DCLogin, this::onLoginRsps);
        processorMap.put(Msg.DCSCreateConfReq, this::onCreateDcConfRsps);
        processorMap.put(Msg.DCDownload, this::onDownloadRsp);
        processorMap.put(Msg.DCQueryPicUrl, this::onQueryPicUrlRsp);
        return processorMap;
    }

    @Override
    protected Map<Msg[], NtfProcessor> ntfsProcessors() {
        Map<Msg[], NtfProcessor> processorMap = new HashMap<>();
        processorMap.put(new Msg[]{
                Msg.DcsCurrentWhiteBoard_Ntf,
                Msg.DcsNewWhiteBoard_Ntf,
                Msg.DcsSwitch_Ntf,
                Msg.DcsDelWhiteBoard_Ntf,
        }, this::onControlNtfs);
        processorMap.put(new Msg[]{
                Msg.DcsElementOperBegin_Ntf,
                Msg.DcsOperLineOperInfo_Ntf,
                Msg.DcsOperCircleOperInfo_Ntf,
                Msg.DcsOperRectangleOperInfo_Ntf,
                Msg.DcsOperPencilOperInfo_Ntf,
                Msg.DcsOperInsertPic_Ntf,
                Msg.DcsOperPitchPicDrag_Ntf,
                Msg.DcsOperPitchPicDel_Ntf,
                Msg.DcsOperEraseOperInfo_Ntf,
                Msg.DcsOperFullScreen_Ntf,
                Msg.DcsOperUndo_Ntf,
                Msg.DcsOperRedo_Ntf,
                Msg.DcsOperClearScreen_Ntf,
                Msg.DcsElementOperFinal_Ntf,
        }, this::onPaintNtfs);

        return processorMap;
    }


    public void login(String serverIp, int port, int terminalType, IResponseListener resultListener){
        req(Msg.DCLogin, new MsgBeans.TDCSRegInfo(serverIp, port, convertTerminalType(terminalType)), resultListener);
    }

    public void createDcConf(IResponseListener resultListener){
        req(Msg.DCSCreateConfReq, new MsgBeans.DCSCreateConf(), resultListener);
    }

    /**发送绘制操作*/
    public void postPaintOp(OpPaint op){
//        req();
    }

    private void onLoginRsps(Msg rspId, Object rspContent, IResponseListener listener){
        KLog.p("rspId=%s, rspContent=%s, listener=%s",rspId, rspContent, listener);
        if (Msg.DCBuildLink4LoginRsp.equals(rspId)){
            MsgBeans.DcsLinkCreationResult linkCreationResult = (MsgBeans.DcsLinkCreationResult) rspContent;
            if (!linkCreationResult.bSuccess
                    && null != listener){
                cancelReq(Msg.DCLogin, listener);  // 后续不会有DcsLoginSrv_Rsp上来，取消该请求以防等待超时。
                listener.onResponse(CommonResultCode.FAILED, null);
            }
        }else if (Msg.DCLoginRsp.equals(rspId)){
            MsgBeans.DcsLoginResult loginRes = (MsgBeans.DcsLoginResult) rspContent;
            if (null != listener){
                if (loginRes.bSucces) {
                    listener.onResponse(CommonResultCode.SUCCESS, null);
                }else{
                    listener.onResponse(CommonResultCode.FAILED, null);
                }
            }
        }
    }


    private void onCreateDcConfRsps(Msg rspId, Object rspContent, IResponseListener listener){
        if (Msg.DcsConfResult_Ntf.equals(rspId)){
            MsgBeans.DcsConfResult dcsConfResult = (MsgBeans.DcsConfResult) rspContent;
            if (!dcsConfResult.bSuccess
                    && null != listener){
                cancelReq(Msg.DCSCreateConfReq, listener);  // 后续不会有DcsCreateConf_Rsp上来，取消该请求以防等待超时。
                listener.onResponse(CommonResultCode.FAILED, null);
            }
        }else if (Msg.DcsCreateConf_Rsp.equals(rspId)){
            MsgBeans.TDCSCreateConfResult createConfResult = (MsgBeans.TDCSCreateConfResult) rspContent;
            if (null != listener){
                if (createConfResult.bSuccess) {
                    listener.onResponse(CommonResultCode.SUCCESS, null);
                }else{
                    listener.onResponse(CommonResultCode.FAILED, null);
                }
            }
        }
    }


    private void onDownloadRsp(Msg rspId, Object rspContent, IResponseListener listener){
        MsgBeans.TDCSFileLoadResult result = (MsgBeans.TDCSFileLoadResult) rspContent;
        if (!result.bSuccess){
            KLog.p(KLog.ERROR, "download file failed!");
            return;
        }
        if (result.bElementFile){
            /* 下载的是图元（而非图片本身）。
            * 后续会批量上报当前画板已有的图元：
            * DcsElementOperBegin_Ntf // 批量上报开始
            * ...  // 批量图元，如画线、画圆、插入图片等等
            *
            * ...ElementShouldAfterFinal  // 应该在该批次图元之后出现的图元。
            *                                NOTE: 批量上报过程中可能会混入画板中当前正在进行的操作，
            *                                这会导致图元上报的时序跟实际时序不一致，需要自行处理。
            *                                （实际时序应该是：在成功响应下载请求后的所有后续进行的操作都应出现在DcsElementOperFinal_Ntf之后）
            * ...// 批量图元
            * DcsElementOperFinal_Ntf // 批量结束
            *
            *
            * */
            return;
        }

        // 下载的是图片
        OpPaint op = new OpUpdatePic(result.achTabid, result.achWbPicentityId, BitmapFactory.decodeFile(result.achFilePathName));
        IOnPaintOpListener onPaintOpListener = ((DownloadListener)listener).onPaintOpListener;
        KLog.p("download pic finished, onPaintOpListener=%s", onPaintOpListener);
        if (null != onPaintOpListener){
            onPaintOpListener.onPaintOp(op);  // 前面我们插入图片的操作并无实际效果，因为图片是“置空”的，此时图片已下载完成，我们更新之前置空的图片。
        }
    }


    private void onQueryPicUrlRsp(Msg rspId, Object rspContent, IResponseListener listener){
        MsgBeans.DCTransferPicUrlRsp transferPicUrlRsp = (MsgBeans.DCTransferPicUrlRsp) rspContent;
        MsgBeans.TDCSResult result = transferPicUrlRsp.MainParam;
        if (!result.bSucces){
            return;
        }
        MsgBeans.TDCSImageUrl url = transferPicUrlRsp.AssParam;

        // 下载图片文件
        req(Msg.DCDownload,
                new MsgBeans.DownloadFilePara(url.achTabId, url.achWbPicentityId, "/data/local/tmp/"+url.achWbPicentityId+".jpg", url.achPicUrl),
                listener);
    }


    private void onControlNtfs(Msg ntfId, Object ntfContent, Set<Object> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        IPaintBoardLifecycleListener boardLifecycleListener;
        for (Object listener : listeners) {
            boardLifecycleListener = (IPaintBoardLifecycleListener) listener;
            if (Msg.DcsCurrentWhiteBoard_Ntf.equals(ntfId)) {
                MsgBeans.TDCSBoardInfo boardInfo = (MsgBeans.TDCSBoardInfo) ntfContent;
                boardLifecycleListener.onBoardSwitched(boardInfo.achTabId);
            } else if (Msg.DcsNewWhiteBoard_Ntf.equals(ntfId)) {
                MsgBeans.TDCSBoardInfo boardInfo = (MsgBeans.TDCSBoardInfo) ntfContent;
                boardLifecycleListener.onBoardCreated(new PaintBoardInfo(boardInfo.achTabId, boardInfo.achWbName));
            } else if (Msg.DcsSwitch_Ntf.equals(ntfId)) {
                MsgBeans.TDCSBoardInfo boardInfo = (MsgBeans.TDCSBoardInfo) ntfContent;
                boardLifecycleListener.onBoardSwitched(boardInfo.achTabId);
                // 下载当前画板已有的图元操作。
                /* NOTE:对于下载下来的图片相关的操作，如插入图片、删除图片等，并不包含图片文件本身。
                要获取图片文件本身，需在后续专门下载。*/
                req(Msg.DCDownload,
                        new MsgBeans.DownloadFilePara(boardInfo.achTabId, boardInfo.achElementUrl),
                        null);  // TODO DcsSwitch_Ntf可以有多次，而下载只应该一次。在DcsCurrentWhiteBoard_Ntf中做？实测抓消息。
            } else if (Msg.DcsDelWhiteBoard_Ntf.equals(ntfId)) {
                // TODO
            }
        }
    }

    private Handler handler = new Handler();
    private final Runnable batchOpTimeout = () -> {
        KLog.p(KLog.ERROR,"wait batch paint ops timeout <<<<<<<<<<<<<<<<<<<<<<<");
        Set<Object> listeners = getNtfListeners(Msg.DcsElementOperFinal_Ntf);
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
            if (Msg.DcsElementOperBegin_Ntf.equals(ntfId)) {
                if (isRecvingBatchOps) {  // TODO 多个画板切换会有多个DcsElementOperBegin_Ntf，此处应该要分boardId处理；另外在DcsElementOperBegin_Ntf之前就有可能有新的操作过来，或许要在开始download的时候就开启isRecvingBatchOps
                    return;
                }
                KLog.p("batch paint ops >>>>>>>>>>>>>>>>>>>>>>");
                isRecvingBatchOps = true;
                handler.postDelayed(batchOpTimeout, 10000); // 起定时器防止final消息不到。
                return;
            } else if (Msg.DcsOperLineOperInfo_Ntf.equals(ntfId)) {
                MsgBeans.DcsOperLineOperInfo_Ntf OpInfo = (MsgBeans.DcsOperLineOperInfo_Ntf) ntfContent;
                MsgBeans.TDCSOperContent commonInfo = OpInfo.MainParam;
                MsgBeans.TDCSWbLine gp = OpInfo.AssParam.tLine;
                paintOp = new OpDrawLine(gp.tBeginPt.nPosx, gp.tBeginPt.nPosy, gp.tEndPt.nPosx, gp.tEndPt.nPosy,
                        commonInfo.dwMsgSequence, new PaintCfg(gp.dwLineWidth, (int) gp.dwRgb), commonInfo.achTabId);
            } else if (Msg.DcsOperCircleOperInfo_Ntf.equals(ntfId)) {
                MsgBeans.DcsOperCircleOperInfo_Ntf opInfo = (MsgBeans.DcsOperCircleOperInfo_Ntf) ntfContent;
                MsgBeans.TDCSOperContent commonInfo = opInfo.MainParam;
                MsgBeans.TDCSWbCircle gp = opInfo.AssParam.tCircle;
                paintOp = new OpDrawOval(gp.tBeginPt.nPosx, gp.tBeginPt.nPosy, gp.tEndPt.nPosx, gp.tEndPt.nPosy,
                        commonInfo.dwMsgSequence, new PaintCfg(gp.dwLineWidth, (int) gp.dwRgb), commonInfo.achTabId);
            } else if (Msg.DcsOperRectangleOperInfo_Ntf.equals(ntfId)) {
                MsgBeans.DcsOperRectangleOperInfo_Ntf opInfo = (MsgBeans.DcsOperRectangleOperInfo_Ntf) ntfContent;
                MsgBeans.TDCSOperContent commonInfo = opInfo.MainParam;
                MsgBeans.TDCSWbRectangle gp = opInfo.AssParam.tRectangle;
                paintOp = new OpDrawRect(gp.tBeginPt.nPosx, gp.tBeginPt.nPosy, gp.tEndPt.nPosx, gp.tEndPt.nPosy,
                        commonInfo.dwMsgSequence, new PaintCfg(gp.dwLineWidth, (int) gp.dwRgb), commonInfo.achTabId);
            } else if (Msg.DcsOperPencilOperInfo_Ntf.equals(ntfId)) {
                MsgBeans.DcsOperPencilOperInfo_Ntf opInfo = (MsgBeans.DcsOperPencilOperInfo_Ntf) ntfContent;
                MsgBeans.TDCSOperContent commonInfo = opInfo.MainParam;
                MsgBeans.TDCSWbPencil gp = opInfo.AssParam.tPencil;
                MsgBeans.TDCSWbPoint[] pl = gp.atPList;
                PointF[] points = new PointF[pl.length];
                if (0 == points.length) {
                    return;
                }
                for (int i = 0; i < points.length; ++i) {
                    points[i] = new PointF(pl[i].nPosx, pl[i].nPosy);
                }
                paintOp = new OpDrawPath(points, commonInfo.dwMsgSequence, new PaintCfg(gp.dwLineWidth, (int) gp.dwRgb), commonInfo.achTabId);
            } else if (Msg.DcsOperInsertPic_Ntf.equals(ntfId)) {
                MsgBeans.DcsOperInsertPic_Ntf opInfo = (MsgBeans.DcsOperInsertPic_Ntf) ntfContent;
                MsgBeans.TDCSOperContent commonInfo = opInfo.MainParam;
                MsgBeans.TDCSWbInsertPicOperInfo gp = opInfo.AssParam;

                paintOp = new OpInsertPic(gp.achImgId,
                        null/*BitmapFactory.decodeFile("/data/local/tmp/wb.png")*/, // NOTE: 此时图片还未下载到本地，先置空，等下载完成后再更新
                        gp.dwImgWidth, gp.dwImgHeight,
                        gp.tPoint.nPosx, gp.tPoint.nPosy, gp.aachMatrixValue, commonInfo.dwMsgSequence, commonInfo.achTabId);

                // 获取图片下载地址（然后再下载图片）
                KLog.p("start download pic, onPaintOpListener=%s", listener);
                req(Msg.DCQueryPicUrl,
                        new MsgBeans.TDCSImageUrl(commonInfo.achConfE164, commonInfo.achTabId, gp.achImgId),
                        new DownloadListener((IOnPaintOpListener) listener));

            } else if (Msg.DcsOperPitchPicDrag_Ntf.equals(ntfId)) {
                MsgBeans.DcsOperPitchPicDrag_Ntf opInfo = (MsgBeans.DcsOperPitchPicDrag_Ntf) ntfContent;
                MsgBeans.TDCSOperContent commonInfo = opInfo.MainParam;
                MsgBeans.TDCSWbPitchPicOperInfo gp = opInfo.AssParam;
                Map<String, float[]> picsMatrix = new HashMap<>();
                for (MsgBeans.TDCSWbGraphsInfo picInfo : gp.atGraphsInfo){
                    float[] matrixValue = new float[9];
                    for (int i=0; i<picInfo.aachMatrixValue.length; ++i){
                        matrixValue[i] = Float.valueOf(picInfo.aachMatrixValue[i]);
                    }
                    picsMatrix.put(picInfo.achGraphsId, matrixValue);
                }
                paintOp = new OpDragPic(picsMatrix, commonInfo.dwMsgSequence, commonInfo.achTabId);
            } else if (Msg.DcsOperPitchPicDel_Ntf.equals(ntfId)) {
                MsgBeans.DcsOperPitchPicDel_Ntf opInfo = (MsgBeans.DcsOperPitchPicDel_Ntf) ntfContent;
                MsgBeans.TDCSOperContent commonInfo = opInfo.MainParam;
                MsgBeans.TDCSWbDelPicOperInfo gp = opInfo.AssParam;
                paintOp = new OpDeletePic(gp.achGraphsId, commonInfo.dwMsgSequence, commonInfo.achTabId);
            } else if (Msg.DcsOperEraseOperInfo_Ntf.equals(ntfId)) {
                MsgBeans.DcsOperEraseOperInfo_Ntf opInfo = (MsgBeans.DcsOperEraseOperInfo_Ntf) ntfContent;
                MsgBeans.TDCSWbEraseOperInfo gp = opInfo.AssParam;
                MsgBeans.TDCSOperContent commonInfo = opInfo.MainParam;
                paintOp = new OpErase(gp.tBeginPt.nPosx, gp.tBeginPt.nPosy, gp.tEndPt.nPosx, gp.tEndPt.nPosy, commonInfo.dwMsgSequence, commonInfo.achTabId);
            } else if (Msg.DcsOperFullScreen_Ntf.equals(ntfId)) {
                MsgBeans.DcsOperFullScreen_Ntf opInfo = (MsgBeans.DcsOperFullScreen_Ntf) ntfContent;
                MsgBeans.TDCSWbDisPlayInfo gp = opInfo.AssParam;
                MsgBeans.TDCSOperContent commonInfo = opInfo.MainParam;
                float[] matrixValue = new float[9];
                for (int i=0; i<gp.aachMatrixValue.length; ++i){
                    matrixValue[i] = Float.valueOf(gp.aachMatrixValue[i]);
                }
                paintOp = new OpMatrix(matrixValue, commonInfo.dwMsgSequence, commonInfo.achTabId);
            } else if (Msg.DcsOperUndo_Ntf.equals(ntfId)) {
                MsgBeans.DcsOperUndo_Ntf opInfo = (MsgBeans.DcsOperUndo_Ntf) ntfContent;
                MsgBeans.TDCSOperContent commonInfo = opInfo.MainParam;
                paintOp = new OpUndo(commonInfo.dwMsgSequence, commonInfo.achTabId);
            } else if (Msg.DcsOperRedo_Ntf.equals(ntfId)) {
                MsgBeans.DcsOperRedo_Ntf opInfo = (MsgBeans.DcsOperRedo_Ntf) ntfContent;
                MsgBeans.TDCSOperContent commonInfo = opInfo.MainParam;
                paintOp = new OpRedo(commonInfo.dwMsgSequence, commonInfo.achTabId);
            } else if (Msg.DcsOperClearScreen_Ntf.equals(ntfId)) {
                MsgBeans.TDCSOperContent opInfo = (MsgBeans.TDCSOperContent) ntfContent;
                paintOp = new OpClearScreen(opInfo.dwMsgSequence, opInfo.achTabId);
            } else if (Msg.DcsElementOperFinal_Ntf.equals(ntfId)) {
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






    private MsgConst.EmDcsType convertTerminalType(int terminal){
        switch (terminal){
            case Terminal_TrueLinkPc:
                return MsgConst.EmDcsType.emTypeTrueLink;
            case Terminal_TrueLinkIosPhone:
                return MsgConst.EmDcsType.emTypeTrueTouchPhoneIOS;
            case Terminal_TrueLinkIosPad:
                return MsgConst.EmDcsType.emTypeTrueTouchPadIOS;
            case Terminal_TrueLinkAndroidPhone:
                return MsgConst.EmDcsType.emTypeTrueTouchPhoneAndroid;
            case Terminal_TrueLinkAndroidPad:
                return MsgConst.EmDcsType.emTypeTrueTouchPadAndroid;
            case Terminal_TrueSens:
                return MsgConst.EmDcsType.emTypeTrueSens;
            case Terminal_Imix:
                return MsgConst.EmDcsType.emTypeIMIX;
            case Terminal_Other:
            default:
                return MsgConst.EmDcsType.emTypeThirdPartyTer;
        }
    }


    public interface IOnPaintOpListener{
        void onPaintOp(OpPaint op);
    }

    public interface IPaintBoardLifecycleListener{
        void onBoardCreated(PaintBoardInfo boardInfo);
        void onBoardDeleted(String boardId);
        void onBoardSwitched(String boardId);
    }


    public void addPaintBoardLifecycleListener(IPaintBoardLifecycleListener boardLifecycleListener){
        subscribe(new Msg[]{
                Msg.DcsCurrentWhiteBoard_Ntf,
                Msg.DcsNewWhiteBoard_Ntf,
                Msg.DcsSwitch_Ntf,
                Msg.DcsDelWhiteBoard_Ntf,
        }, boardLifecycleListener);
    }


    public void addPaintOpListener(IOnPaintOpListener onPaintOpListener){
        subscribe(new Msg[]{
                Msg.DcsElementOperBegin_Ntf,  // TODO batch消息对界面屏蔽
                Msg.DcsOperLineOperInfo_Ntf,
                Msg.DcsOperCircleOperInfo_Ntf,
                Msg.DcsOperRectangleOperInfo_Ntf,
                Msg.DcsOperPencilOperInfo_Ntf,
                Msg.DcsOperInsertPic_Ntf,
                Msg.DcsOperPitchPicDrag_Ntf,
                Msg.DcsOperPitchPicDel_Ntf,
                Msg.DcsOperEraseOperInfo_Ntf,
                Msg.DcsOperFullScreen_Ntf,
                Msg.DcsOperUndo_Ntf,
                Msg.DcsOperRedo_Ntf,
                Msg.DcsOperClearScreen_Ntf,
                Msg.DcsElementOperFinal_Ntf,
        }, onPaintOpListener);
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
        eject(msg);
    }


}
