package com.kedacom.vconf.sdk.datacollaborate;

import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.os.Handler;

import com.kedacom.vconf.sdk.base.INotificationListener;
import com.kedacom.vconf.sdk.base.IResultListener;
import com.kedacom.vconf.sdk.base.Msg;
import com.kedacom.vconf.sdk.base.MsgBeans;
import com.kedacom.vconf.sdk.base.MsgConst;
import com.kedacom.vconf.sdk.base.RequestAgent;
import com.kedacom.vconf.sdk.base.ResultCode;
import com.kedacom.vconf.sdk.base.KLog;
import com.kedacom.vconf.sdk.datacollaborate.bean.ClearScreenOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.DrawLineOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.DrawPathOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.DrawRectOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.EraseOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.InsertPicOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.MatrixOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.PaintCfg;
import com.kedacom.vconf.sdk.datacollaborate.bean.PaintOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.DrawOvalOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.RedoOp;
import com.kedacom.vconf.sdk.datacollaborate.bean.UndoOp;

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

    private IPainter painter;

    private boolean isBatchDrawing = false;
    private PriorityQueue<PaintOp> batchOps = new PriorityQueue<>(); // 批量操作缓存。批量模式下操作到达的时序可能跟操作的序列号顺序不相符，此处我们使用PriorityQueue来为我们自动排序。

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


    public void setPainter(IPainter painter){
        this.painter = painter;
    }


    public void login(String serverIp, int port, int terminalType, IResultListener resultListener){
        req(Msg.DCLogin, new MsgBeans.TDCSRegInfo(serverIp, port, convertTerminalType(terminalType)), resultListener);
    }

    public void createDcConf(IResultListener resultListener){
        req(Msg.DCSCreateConfReq, new MsgBeans.DCSCreateConf(), resultListener);
    }

    public void postPaintOp(PaintOp op){
//        req();
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

    private void onControlNtfs(Msg ntfId, Object ntfContent, Set<INotificationListener> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        if (null == painter){
            return;
        }
        if (Msg.DcsCurrentWhiteBoard_Ntf.equals(ntfId)
                || Msg.DcsNewWhiteBoard_Ntf.equals(ntfId)
                || Msg.DcsSwitch_Ntf.equals(ntfId)){
            MsgBeans.TDCSBoardInfo boardInfo = (MsgBeans.TDCSBoardInfo) ntfContent;
            painter.switchPaintBoard(boardInfo.achTabId);
        }else if (Msg.DcsDelWhiteBoard_Ntf.equals(ntfId)){
            MsgBeans.TDCSBoardInfo boardInfo = (MsgBeans.TDCSBoardInfo) ntfContent;
            painter.deletePaintBoard(boardInfo.achTabId);
        }
    }

    private Handler handler = new Handler();
    private final Runnable batchOpTimeout = () -> {
        KLog.p(KLog.ERROR,"wait batch paint ops timeout <<<<<<<<<<<<<<<<<<<<<<<");
        if (null != painter){
            while(!batchOps.isEmpty()){
                painter.paint(batchOps.poll());
            }
        }else{
            batchOps.clear();
        }
        isBatchDrawing = false;
    };
    private void onPaintNtfs(Msg ntfId, Object ntfContent, Set<INotificationListener> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        if (null == painter){
            return;
        }

        PaintOp paintOp = null;
        if (Msg.DcsElementOperBegin_Ntf.equals(ntfId)){
            if (isBatchDrawing){
                return;
            }
            KLog.p("batch paint ops >>>>>>>>>>>>>>>>>>>>>>");
            isBatchDrawing = true;
            handler.postDelayed(batchOpTimeout, 10000); // 起定时器防止final消息不到。
            return;
        }else if (Msg.DcsOperLineOperInfo_Ntf.equals(ntfId)){
            MsgBeans.DcsOperLineOperInfo_Ntf OpInfo = (MsgBeans.DcsOperLineOperInfo_Ntf) ntfContent;
            MsgBeans.TDCSOperContent commonInfo = OpInfo.MainParam;
            MsgBeans.TDCSWbLine gp = OpInfo.AssParam.tLine;
            paintOp = new DrawLineOp(gp.tBeginPt.nPosx, gp.tBeginPt.nPosy, gp.tEndPt.nPosx, gp.tEndPt.nPosy,
                    commonInfo.dwMsgSequence, new PaintCfg(gp.dwLineWidth, (int) gp.dwRgb), commonInfo.achTabId);
        }else if (Msg.DcsOperCircleOperInfo_Ntf.equals(ntfId)){
            MsgBeans.DcsOperCircleOperInfo_Ntf opInfo = (MsgBeans.DcsOperCircleOperInfo_Ntf) ntfContent;
            MsgBeans.TDCSOperContent commonInfo = opInfo.MainParam;
            MsgBeans.TDCSWbCircle gp = opInfo.AssParam.tCircle;
            paintOp = new DrawOvalOp(gp.tBeginPt.nPosx, gp.tBeginPt.nPosy, gp.tEndPt.nPosx, gp.tEndPt.nPosy,
                    commonInfo.dwMsgSequence, new PaintCfg(gp.dwLineWidth, (int) gp.dwRgb), commonInfo.achTabId);
        }else if (Msg.DcsOperRectangleOperInfo_Ntf.equals(ntfId)){
            MsgBeans.DcsOperRectangleOperInfo_Ntf opInfo = (MsgBeans.DcsOperRectangleOperInfo_Ntf) ntfContent;
            MsgBeans.TDCSOperContent commonInfo = opInfo.MainParam;
            MsgBeans.TDCSWbRectangle gp = opInfo.AssParam.tRectangle;
            paintOp = new DrawRectOp(gp.tBeginPt.nPosx, gp.tBeginPt.nPosy, gp.tEndPt.nPosx, gp.tEndPt.nPosy,
                    commonInfo.dwMsgSequence, new PaintCfg(gp.dwLineWidth, (int) gp.dwRgb), commonInfo.achTabId);
        }else if (Msg.DcsOperPencilOperInfo_Ntf.equals(ntfId)){
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
            }
            paintOp = new DrawPathOp(points, commonInfo.dwMsgSequence, new PaintCfg(gp.dwLineWidth, (int) gp.dwRgb), commonInfo.achTabId);
        }else if (Msg.DcsOperInsertPic_Ntf.equals(ntfId)){
            MsgBeans.DcsOperInsertPic_Ntf opInfo = (MsgBeans.DcsOperInsertPic_Ntf) ntfContent;
            MsgBeans.TDCSOperContent commonInfo = opInfo.MainParam;
            MsgBeans.TDCSWbInsertPicOperInfo gp = opInfo.AssParam;
            paintOp = new InsertPicOp(BitmapFactory.decodeFile("/data/local/tmp/wb.png"), gp.dwImgWidth, gp.dwImgHeight,
                    gp.tPoint.nPosx, gp.tPoint.nPosy, gp.aachMatrixValue, commonInfo.dwMsgSequence, commonInfo.achTabId);
        }else if (Msg.DcsOperPitchPicDrag_Ntf.equals(ntfId)){
            paintOp = null;
        }else if (Msg.DcsOperPitchPicDel_Ntf.equals(ntfId)){
            paintOp = null;
        }else if (Msg.DcsOperEraseOperInfo_Ntf.equals(ntfId)){
            MsgBeans.DcsOperEraseOperInfo_Ntf opInfo = (MsgBeans.DcsOperEraseOperInfo_Ntf) ntfContent;
            MsgBeans.TDCSWbEraseOperInfo gp = opInfo.AssParam;
            MsgBeans.TDCSOperContent commonInfo = opInfo.MainParam;
            paintOp = new EraseOp(gp.tBeginPt.nPosx, gp.tBeginPt.nPosy, gp.tEndPt.nPosx, gp.tEndPt.nPosy, commonInfo.dwMsgSequence, commonInfo.achTabId);
        }else if (Msg.DcsOperFullScreen_Ntf.equals(ntfId)){
            MsgBeans.DcsOperFullScreen_Ntf opInfo = (MsgBeans.DcsOperFullScreen_Ntf) ntfContent;
            MsgBeans.TDCSWbDisPlayInfo gp = opInfo.AssParam;
            MsgBeans.TDCSOperContent commonInfo = opInfo.MainParam;
            paintOp = new MatrixOp(gp.aachMatrixValue, commonInfo.dwMsgSequence, commonInfo.achTabId);
        }else if (Msg.DcsOperUndo_Ntf.equals(ntfId)){
            MsgBeans.DcsOperUndo_Ntf opInfo = (MsgBeans.DcsOperUndo_Ntf) ntfContent;
            MsgBeans.TDCSOperContent commonInfo = opInfo.MainParam;
            paintOp = new UndoOp(commonInfo.dwMsgSequence, commonInfo.achTabId);
        }else if (Msg.DcsOperRedo_Ntf.equals(ntfId)){
            MsgBeans.DcsOperRedo_Ntf opInfo = (MsgBeans.DcsOperRedo_Ntf) ntfContent;
            MsgBeans.TDCSOperContent commonInfo = opInfo.MainParam;
            paintOp = new RedoOp(commonInfo.dwMsgSequence, commonInfo.achTabId);
        }else if (Msg.DcsOperClearScreen_Ntf.equals(ntfId)){
            MsgBeans.TDCSOperContent opInfo = (MsgBeans.TDCSOperContent) ntfContent;
            paintOp = new ClearScreenOp(opInfo.dwMsgSequence, opInfo.achTabId);
        }else if (Msg.DcsElementOperFinal_Ntf.equals(ntfId)){
            if (!isBatchDrawing) {
                return;
            }
            KLog.p("batch paint ops <<<<<<<<<<<<<<<<<<<<<<<");
            handler.removeCallbacks(batchOpTimeout);
            while(!batchOps.isEmpty()){
                painter.paint(batchOps.poll());
            }
            isBatchDrawing = false;
            return;
        }

        if (isBatchDrawing){
            batchOps.offer(paintOp);
        }else{
            if (null != paintOp)
                painter.paint(paintOp);
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

}
