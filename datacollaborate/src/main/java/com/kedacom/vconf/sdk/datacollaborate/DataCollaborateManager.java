package com.kedacom.vconf.sdk.datacollaborate;

import android.graphics.PointF;

import com.kedacom.vconf.sdk.base.INotificationListener;
import com.kedacom.vconf.sdk.base.IResultListener;
import com.kedacom.vconf.sdk.base.Msg;
import com.kedacom.vconf.sdk.base.MsgBeans;
import com.kedacom.vconf.sdk.base.MsgConst;
import com.kedacom.vconf.sdk.base.RequestAgent;
import com.kedacom.vconf.sdk.base.ResultCode;
import com.kedacom.vconf.sdk.base.KLog;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCEraseOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCLineOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCMatrixOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCOvalOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCPaintCfg;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCPathOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCRectOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCRedoOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCUndoOp;

//import static com.kedacom.vconf.sdk.base.MsgBeans.*; // TODO 使用static import？

import java.util.HashMap;
import java.util.Map;
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

    private IDCPainter painter;

    @Override
    protected Map<Msg, RspProcessor> rspProcessors() {
        Map<Msg, RspProcessor> processorMap = new HashMap<>();
        processorMap.put(Msg.DCLogin, this::onLoginResponses);
        processorMap.put(Msg.DCSCreateConfReq, this::onCreateDcConfResponses);
        return processorMap;
    }

    @Override
    protected Map<Msg, NtfProcessor> ntfProcessors() {
        Map<Msg, NtfProcessor> processorMap = new HashMap<>();
        processorMap.put(Msg.DcsCurrentWhiteBoard_Ntf, this::onCurrentWhiteBoardNtf);
        processorMap.put(Msg.DcsNewWhiteBoard_Ntf, this::onNewWhiteBoardNtf);
        processorMap.put(Msg.DcsSwitch_Ntf, this::onSwitchWhiteBoardNtf);
        processorMap.put(Msg.DcsElementOperBegin_Ntf, this::onBatchOpBeginNtf);
        processorMap.put(Msg.DcsOperLineOperInfo_Ntf, this::onLineOpNtf);
        processorMap.put(Msg.DcsOperCircleOperInfo_Ntf, this::onCircleOpNtf);
        processorMap.put(Msg.DcsOperRectangleOperInfo_Ntf, this::onRectangleOpNtf);
        processorMap.put(Msg.DcsOperPencilOperInfo_Ntf, this::onPencilOpNtf);
        processorMap.put(Msg.DcsOperInsertPic_Ntf, this::onInsertPicNtf);
        processorMap.put(Msg.DcsOperPitchPicDrag_Ntf, this::onPitchPicNtf);
        processorMap.put(Msg.DcsOperPitchPicDel_Ntf, this::onDelPicNtf);
        processorMap.put(Msg.DcsOperEraseOperInfo_Ntf, this::onEraseNtf);
        processorMap.put(Msg.DcsOperFullScreen_Ntf, this::onMatrixNtf);
        processorMap.put(Msg.DcsOperUndo_Ntf, this::onUndoNtf);
        processorMap.put(Msg.DcsOperRedo_Ntf, this::onRedoNtf);
        processorMap.put(Msg.DcsOperClearScreen_Ntf, this::onClearScreenNtf);
        processorMap.put(Msg.DcsElementOperFinal_Ntf, this::onBatchOpFinNtf);

        return processorMap;
    }


    public void setPainter(IDCPainter painter){
        this.painter = painter;
    }

    public void login(String serverIp, int port, int terminalType, IResultListener resultListener){
        req(Msg.DCLogin, new MsgBeans.TDCSRegInfo(serverIp, port, convertTerminalType(terminalType)), resultListener);
    }

    public void createDcConf(IResultListener resultListener){
        req(Msg.DCSCreateConfReq, new MsgBeans.DCSCreateConf(), resultListener);
    }

    private void onLoginResponses(Msg rspId, Object rspContent, IResultListener listener){
        KLog.p("rspId=%s, rspContent=%s, listener=%s",rspId, rspContent, listener);
        if (Msg.DCBuildLink4LoginRsp.equals(rspId)){
            MsgBeans.DcsLinkCreationResult linkCreationResult = (MsgBeans.DcsLinkCreationResult) rspContent;
            if (!linkCreationResult.bSuccess
                    && null != listener){
                cancelReq(Msg.DCLogin, listener);  // 后续不会有DcsLoginSrv_Rsp上来，取消该请求以防等待超时。
                listener.onResponse(ResultCode.FAILED, null);
            }
        }else if (Msg.DCLoginRsp.equals(rspId)){
            MsgBeans.DcsLoginResult loginRes = (MsgBeans.DcsLoginResult) rspContent;
            if (null != listener){
                if (loginRes.bSucces) {
                    listener.onResponse(ResultCode.SUCCESS, null);
                }else{
                    listener.onResponse(ResultCode.FAILED, null);
                }
            }
        }
    }


    private void onCreateDcConfResponses(Msg rspId, Object rspContent, IResultListener listener){
        if (Msg.DcsConfResult_Ntf.equals(rspId)){
            MsgBeans.DcsConfResult dcsConfResult = (MsgBeans.DcsConfResult) rspContent;
            if (!dcsConfResult.bSuccess
                    && null != listener){
                cancelReq(Msg.DCSCreateConfReq, listener);  // 后续不会有DcsCreateConf_Rsp上来，取消该请求以防等待超时。
                listener.onResponse(ResultCode.FAILED, null);
            }
        }else if (Msg.DcsCreateConf_Rsp.equals(rspId)){
            MsgBeans.TDCSCreateConfResult createConfResult = (MsgBeans.TDCSCreateConfResult) rspContent;
            if (null != listener){
                if (createConfResult.bSuccess) {
                    listener.onResponse(ResultCode.SUCCESS, null);
                }else{
                    listener.onResponse(ResultCode.FAILED, null);
                }
            }
        }
    }

    private void onCurrentWhiteBoardNtf(Msg ntfId, Object ntfContent, Set<INotificationListener> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
    }

    private void onNewWhiteBoardNtf(Msg ntfId, Object ntfContent, Set<INotificationListener> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        for (INotificationListener listener : listeners) {
            listener.onNotification(ntfContent);
        }
    }

    private void onSwitchWhiteBoardNtf(Msg ntfId, Object ntfContent, Set<INotificationListener> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        for (INotificationListener listener : listeners) {
            listener.onNotification(ntfContent);
        }
    }

    private void onBatchOpBeginNtf(Msg ntfId, Object ntfContent, Set<INotificationListener> listeners){
//        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        if (null != painter){
            painter.startBatchDraw();
        }
    }

    private void onLineOpNtf(Msg ntfId, Object ntfContent, Set<INotificationListener> listeners){
//        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        if (null != painter){
            MsgBeans.DcsOperLineOperInfo_Ntf OpInfo = (MsgBeans.DcsOperLineOperInfo_Ntf) ntfContent;
            MsgBeans.TDCSOperContent commonInfo = OpInfo.MainParam;
            MsgBeans.TDCSWbLine gp = OpInfo.AssParam.tLine;
            KLog.p("line{left=%s, top=%s, right=%s, bottom=%s}, paint{width=%s, rgb=%s}",
                    gp.tBeginPt.nPosx, gp.tBeginPt.nPosy, gp.tEndPt.nPosx, gp.tEndPt.nPosy, gp.dwLineWidth, (int) gp.dwRgb);
            painter.draw(new DCLineOp(gp.tBeginPt.nPosx, gp.tBeginPt.nPosy, gp.tEndPt.nPosx, gp.tEndPt.nPosy,
                    commonInfo.dwMsgSequence, new DCPaintCfg(gp.dwLineWidth, (int) gp.dwRgb)));
//            painter.draw(new DCLineOp(gp.tBeginPt.nPosx, gp.tBeginPt.nPosy, gp.tEndPt.nPosx, gp.tEndPt.nPosy,
//                    1, new DCPaintCfg(gp.dwLineWidth, (int) gp.dwRgb)));
        }
    }

    private void onCircleOpNtf(Msg ntfId, Object ntfContent, Set<INotificationListener> listeners){
//        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        if (null != painter){
            MsgBeans.DcsOperCircleOperInfo_Ntf opInfo = (MsgBeans.DcsOperCircleOperInfo_Ntf) ntfContent;
            MsgBeans.TDCSOperContent commonInfo = opInfo.MainParam;
            MsgBeans.TDCSWbCircle gp = opInfo.AssParam.tCircle;
            KLog.p("oval{left=%s, top=%s, right=%s, bottom=%s}, paint{width=%s, rgb=%s}",
                    gp.tBeginPt.nPosx, gp.tBeginPt.nPosy, gp.tEndPt.nPosx, gp.tEndPt.nPosy, gp.dwLineWidth, (int) gp.dwRgb);
            painter.draw(new DCOvalOp(gp.tBeginPt.nPosx, gp.tBeginPt.nPosy, gp.tEndPt.nPosx, gp.tEndPt.nPosy,
                    commonInfo.dwMsgSequence, new DCPaintCfg(gp.dwLineWidth, (int) gp.dwRgb)));
//            painter.draw(new DCOvalOp(gp.tBeginPt.nPosx, gp.tBeginPt.nPosy, gp.tEndPt.nPosx, gp.tEndPt.nPosy,
//                    2, new DCPaintCfg(gp.dwLineWidth, (int) gp.dwRgb)));
        }
    }

    private void onRectangleOpNtf(Msg ntfId, Object ntfContent, Set<INotificationListener> listeners){
//        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        if (null != painter){
            MsgBeans.DcsOperRectangleOperInfo_Ntf opInfo = (MsgBeans.DcsOperRectangleOperInfo_Ntf) ntfContent;
            MsgBeans.TDCSOperContent commonInfo = opInfo.MainParam;
            MsgBeans.TDCSWbRectangle gp = opInfo.AssParam.tRectangle;
            KLog.p("line{left=%s, top=%s, right=%s, bottom=%s}, paint{width=%s, rgb=%s}",
                    gp.tBeginPt.nPosx, gp.tBeginPt.nPosy, gp.tEndPt.nPosx, gp.tEndPt.nPosy, gp.dwLineWidth, (int) gp.dwRgb);
            painter.draw(new DCRectOp(gp.tBeginPt.nPosx, gp.tBeginPt.nPosy, gp.tEndPt.nPosx, gp.tEndPt.nPosy,
                    commonInfo.dwMsgSequence, new DCPaintCfg(gp.dwLineWidth, (int) gp.dwRgb)));
//            painter.draw(new DCRectOp(gp.tBeginPt.nPosx, gp.tBeginPt.nPosy, gp.tEndPt.nPosx, gp.tEndPt.nPosy,
//                    3, new DCPaintCfg(gp.dwLineWidth, (int) gp.dwRgb)));
        }
    }

    private void onPencilOpNtf(Msg ntfId, Object ntfContent, Set<INotificationListener> listeners){
//        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        if (null != painter){
            MsgBeans.DcsOperPencilOperInfo_Ntf opInfo = (MsgBeans.DcsOperPencilOperInfo_Ntf) ntfContent;
            MsgBeans.TDCSOperContent commonInfo = opInfo.MainParam;
            MsgBeans.TDCSWbPencil gp = opInfo.AssParam.tPencil;
            MsgBeans.TDCSWbPoint[] pl = gp.atPList;
            PointF[] points = new PointF[pl.length];
            if (0 == points.length){
                return;
            }
            for (int i=0; i<points.length; ++i){
                points[i] = new PointF(pl[i].nPosx, pl[i].nPosy);
                KLog.p("path.point{%s, %s}", points[i].x, points[i].y);
            }
            KLog.p("path.paint{width=%s, rgb=%s}", gp.dwLineWidth, (int) gp.dwRgb);
            painter.draw(new DCPathOp(points, commonInfo.dwMsgSequence, new DCPaintCfg(gp.dwLineWidth, (int) gp.dwRgb)));
//            painter.draw(new DCPathOp(points, 6, new DCPaintCfg(gp.dwLineWidth, (int) gp.dwRgb)));
        }
    }

    private void onInsertPicNtf(Msg ntfId, Object ntfContent, Set<INotificationListener> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        for (INotificationListener listener : listeners) {
            listener.onNotification(ntfContent);
        }
    }

    private void onPitchPicNtf(Msg ntfId, Object ntfContent, Set<INotificationListener> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        for (INotificationListener listener : listeners) {
            listener.onNotification(ntfContent);
        }
    }

    private void onDelPicNtf(Msg ntfId, Object ntfContent, Set<INotificationListener> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        for (INotificationListener listener : listeners) {
            listener.onNotification(ntfContent);
        }
    }

    private void onEraseNtf(Msg ntfId, Object ntfContent, Set<INotificationListener> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        if (null != painter){
            MsgBeans.DcsOperEraseOperInfo_Ntf opInfo = (MsgBeans.DcsOperEraseOperInfo_Ntf) ntfContent;
            MsgBeans.TDCSWbEraseOperInfo gp = opInfo.AssParam;
            MsgBeans.TDCSOperContent commonInfo = opInfo.MainParam;
            painter.draw(new DCEraseOp(gp.tBeginPt.nPosx, gp.tBeginPt.nPosy, gp.tEndPt.nPosx, gp.tEndPt.nPosy, commonInfo.dwMsgSequence));
        }
    }

    private void onMatrixNtf(Msg ntfId, Object ntfContent, Set<INotificationListener> listeners){
//        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        if (null != painter){
            MsgBeans.DcsOperFullScreen_Ntf opInfo = (MsgBeans.DcsOperFullScreen_Ntf) ntfContent;
            MsgBeans.TDCSWbDisPlayInfo gp = opInfo.AssParam;
            MsgBeans.TDCSOperContent commonInfo = opInfo.MainParam;
            painter.draw(new DCMatrixOp(gp.aachMatrixValue, commonInfo.dwMsgSequence));
        }
    }

    private void onUndoNtf(Msg ntfId, Object ntfContent, Set<INotificationListener> listeners){
//        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        if (null != painter){
            MsgBeans.DcsOperUndo_Ntf opInfo = (MsgBeans.DcsOperUndo_Ntf) ntfContent;
            MsgBeans.TDCSOperContent commonInfo = opInfo.MainParam;
            painter.draw(new DCUndoOp(commonInfo.dwMsgSequence));
//            painter.draw(new DCUndoOp(4));
        }
    }

    private void onRedoNtf(Msg ntfId, Object ntfContent, Set<INotificationListener> listeners){
//        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        if (null != painter){
            MsgBeans.DcsOperRedo_Ntf opInfo = (MsgBeans.DcsOperRedo_Ntf) ntfContent;
            MsgBeans.TDCSOperContent commonInfo = opInfo.MainParam;
            painter.draw(new DCRedoOp(commonInfo.dwMsgSequence));
//            painter.draw(new DCRedoOp(5));
        }
    }


    private void onClearScreenNtf(Msg ntfId, Object ntfContent, Set<INotificationListener> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        for (INotificationListener listener : listeners) {
            listener.onNotification(ntfContent);
        }
    }


    private void onBatchOpFinNtf(Msg ntfId, Object ntfContent, Set<INotificationListener> listeners){
//        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        if (null != painter){
            painter.finishBatchDraw();
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
//        eject(Msg.DcsCurrentWhiteBoard_Ntf);
//        eject(Msg.DcsNewWhiteBoard_Ntf);
//        eject(Msg.DcsSwitch_Ntf);
//        eject(Msg.DcsElementOperBegin_Ntf);
//        eject(Msg.DcsOperLineOperInfo_Ntf);
//        eject(Msg.DcsOperCircleOperInfo_Ntf);
//        eject(Msg.DcsOperRectangleOperInfo_Ntf);
//        eject(Msg.DcsOperPencilOperInfo_Ntf);
//        eject(Msg.DcsOperInsertPic_Ntf);
//        eject(Msg.DcsOperPitchPicDrag_Ntf);
//        eject(Msg.DcsOperPitchPicDel_Ntf);
//        eject(Msg.DcsOperEraseOperInfo_Ntf);
//        eject(Msg.DcsOperUndo_Ntf);
//        eject(Msg.DcsOperRedo_Ntf);
//        eject(Msg.DcsOperClearScreen_Ntf);
//        eject(Msg.DcsElementOperFinal_Ntf);
        eject(new Msg[]{
//                Msg.DcsElementOperBegin_Ntf,
                Msg.DcsOperLineOperInfo_Ntf,
                Msg.DcsOperCircleOperInfo_Ntf,
//                Msg.DcsOperUndo_Ntf,
//                Msg.DcsOperUndo_Ntf,
//                Msg.DcsOperUndo_Ntf,
//                Msg.DcsOperUndo_Ntf,
                Msg.DcsOperFullScreen_Ntf,
//                Msg.DcsOperUndo_Ntf,
//                Msg.DcsOperUndo_Ntf,
//                Msg.DcsOperUndo_Ntf,
//                Msg.DcsOperRedo_Ntf,
                Msg.DcsOperRectangleOperInfo_Ntf,
//                Msg.DcsOperRedo_Ntf,
                Msg.DcsOperPencilOperInfo_Ntf,
                Msg.DcsOperEraseOperInfo_Ntf,
//                Msg.DcsElementOperFinal_Ntf,
        });
    }

//    private DataCollaborateManager(){
////        Timer timer = new Timer();
////        timer.schedule(new TimerTask() {
////            @Override
////            public void run() {
////                DataCollaborateManager.this.ejectNtfs();
////            }
////        }, 5000, 5000);
//        new Handler().postDelayed(this::ejectNtfs, 5000);
//    }

}
