package com.kedacom.vconf.sdk.datacollaborate;

import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.os.Handler;

import com.kedacom.vconf.sdk.base.INotificationListener;
import com.kedacom.vconf.sdk.base.IResponseListener;
import com.kedacom.vconf.sdk.base.Msg;
import com.kedacom.vconf.sdk.base.MsgBeans;
import com.kedacom.vconf.sdk.base.MsgConst;
import com.kedacom.vconf.sdk.base.RequestAgent;
import com.kedacom.vconf.sdk.base.CommonResultCode;
import com.kedacom.vconf.sdk.base.KLog;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpClearScreen;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawLine;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawPath;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpDrawRect;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpErase;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpInsertPic;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpMatrix;
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
        processorMap.put(Msg.DCLogin, this::onLoginResponses);
        processorMap.put(Msg.DCSCreateConfReq, this::onCreateDcConfResponses);
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

    private void onLoginResponses(Msg rspId, Object rspContent, IResponseListener listener){
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


    private void onCreateDcConfResponses(Msg rspId, Object rspContent, IResponseListener listener){
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

    private void onControlNtfs(Msg ntfId, Object ntfContent, Set<INotificationListener> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        for (INotificationListener listener : listeners) {
            if (Msg.DcsCurrentWhiteBoard_Ntf.equals(ntfId)
                    || Msg.DcsNewWhiteBoard_Ntf.equals(ntfId)
                    || Msg.DcsSwitch_Ntf.equals(ntfId)) {
                MsgBeans.TDCSBoardInfo boardInfo = (MsgBeans.TDCSBoardInfo) ntfContent;
//                listener.onNotification();
            } else if (Msg.DcsDelWhiteBoard_Ntf.equals(ntfId)) {
                MsgBeans.TDCSBoardInfo boardInfo = (MsgBeans.TDCSBoardInfo) ntfContent;
//                listener.onNotification();
            }
        }
    }

    private Handler handler = new Handler();
    private final Runnable batchOpTimeout = () -> {
        KLog.p(KLog.ERROR,"wait batch paint ops timeout <<<<<<<<<<<<<<<<<<<<<<<");
        Set<INotificationListener> listeners = getNtfListeners(Msg.DcsElementOperFinal_Ntf);
        OpPaint op;
        while (!batchOps.isEmpty()) {
            op = batchOps.poll();
            for (INotificationListener listener : listeners) {
                listener.onNotification(op);
            }
        }

        isRecvingBatchOps = false;
    };
    private void onPaintNtfs(Msg ntfId, Object ntfContent, Set<INotificationListener> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);

        for (INotificationListener listener : listeners) {
            OpPaint paintOp = null;
            if (Msg.DcsElementOperBegin_Ntf.equals(ntfId)) {
                if (isRecvingBatchOps) {
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
                paintOp = new OpInsertPic(BitmapFactory.decodeFile("/data/local/tmp/wb.png"), gp.dwImgWidth, gp.dwImgHeight,
                        gp.tPoint.nPosx, gp.tPoint.nPosy, gp.aachMatrixValue, commonInfo.dwMsgSequence, commonInfo.achTabId);
            } else if (Msg.DcsOperPitchPicDrag_Ntf.equals(ntfId)) {
                paintOp = null;
            } else if (Msg.DcsOperPitchPicDel_Ntf.equals(ntfId)) {
                paintOp = null;
            } else if (Msg.DcsOperEraseOperInfo_Ntf.equals(ntfId)) {
                MsgBeans.DcsOperEraseOperInfo_Ntf opInfo = (MsgBeans.DcsOperEraseOperInfo_Ntf) ntfContent;
                MsgBeans.TDCSWbEraseOperInfo gp = opInfo.AssParam;
                MsgBeans.TDCSOperContent commonInfo = opInfo.MainParam;
                paintOp = new OpErase(gp.tBeginPt.nPosx, gp.tBeginPt.nPosy, gp.tEndPt.nPosx, gp.tEndPt.nPosy, commonInfo.dwMsgSequence, commonInfo.achTabId);
            } else if (Msg.DcsOperFullScreen_Ntf.equals(ntfId)) {
                MsgBeans.DcsOperFullScreen_Ntf opInfo = (MsgBeans.DcsOperFullScreen_Ntf) ntfContent;
                MsgBeans.TDCSWbDisPlayInfo gp = opInfo.AssParam;
                MsgBeans.TDCSOperContent commonInfo = opInfo.MainParam;
                paintOp = new OpMatrix(gp.aachMatrixValue, commonInfo.dwMsgSequence, commonInfo.achTabId);
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
                    listener.onNotification(batchOps.poll());
                }
                isRecvingBatchOps = false;
                return;
            }

            if (isRecvingBatchOps) {
                batchOps.offer(paintOp);
            } else {
                if (null != paintOp)
                    listener.onNotification(paintOp);
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

    public void ejectNtfs(){
        eject(new Msg[]{
                Msg.DcsCurrentWhiteBoard_Ntf,
                Msg.DcsNewWhiteBoard_Ntf,
                Msg.DcsSwitch_Ntf,
                Msg.DcsElementOperBegin_Ntf,
                Msg.DcsOperLineOperInfo_Ntf,
                Msg.DcsOperCircleOperInfo_Ntf,
//                Msg.DcsOperUndo_Ntf,
//                Msg.DcsOperRedo_Ntf,
//                Msg.DcsOperUndo_Ntf,
//                Msg.DcsOperUndo_Ntf,
//                Msg.DcsOperUndo_Ntf,
                Msg.DcsOperFullScreen_Ntf,
//                Msg.DcsOperUndo_Ntf,
//                Msg.DcsOperUndo_Ntf,
//                Msg.DcsOperUndo_Ntf,
//                Msg.DcsOperClearScreen_Ntf,
                Msg.DcsOperRectangleOperInfo_Ntf,
//                Msg.DcsOperRedo_Ntf,
//                Msg.DcsOperRedo_Ntf,
//                Msg.DcsOperRedo_Ntf,
                Msg.DcsOperPencilOperInfo_Ntf,
                Msg.DcsOperEraseOperInfo_Ntf,
                Msg.DcsOperInsertPic_Ntf,
                Msg.DcsElementOperFinal_Ntf,
        });
    }

    public interface IOnPaintOpArrivedListener{
        void onPaintOpArrived(OpPaint op);
    }

    public interface IPaintBoardLifecycleListener{

    }

    public void addPaintBoardLifecycleListener(INotificationListener notificationListener){
        subscribe(new Msg[]{
                Msg.DcsCurrentWhiteBoard_Ntf,
                Msg.DcsNewWhiteBoard_Ntf,
                Msg.DcsSwitch_Ntf,
                Msg.DcsDelWhiteBoard_Ntf,
        }, notificationListener);
    }

    public void addPaintOpListener(INotificationListener notificationListener){
        subscribe(new Msg[]{
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
        }, notificationListener);
    }

}
