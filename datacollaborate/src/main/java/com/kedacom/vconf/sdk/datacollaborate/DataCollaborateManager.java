package com.kedacom.vconf.sdk.datacollaborate;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.kedacom.vconf.sdk.base.AgentManager;
import com.kedacom.vconf.sdk.base.ILifecycleOwner;
import com.kedacom.vconf.sdk.base.IResultListener;
import com.kedacom.vconf.sdk.base.Msg;
import com.kedacom.vconf.sdk.base.RequestAgent;
import com.kedacom.vconf.sdk.base.KLog;
import com.kedacom.vconf.sdk.base.bean.dc.BaseTypeString;
import com.kedacom.vconf.sdk.base.bean.dc.DcsDownloadImageRsp;
import com.kedacom.vconf.sdk.base.bean.dc.DcsGetAllWhiteBoardRsp;
import com.kedacom.vconf.sdk.base.bean.dc.DcsOperInsertPicNtf;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSBoardInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSConfUserInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSConnectResult;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSCreateConf;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSCreateConfResult;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSDelWhiteBoardInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSFileInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSFileLoadResult;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSImageUrl;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSRegInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSResult;
import com.kedacom.vconf.sdk.base.bean.dc.TDcsCacheElementParseResult;
import com.kedacom.vconf.sdk.datacollaborate.bean.CreateConfResult;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCMember;
import com.kedacom.vconf.sdk.datacollaborate.bean.EDcMode;
import com.kedacom.vconf.sdk.datacollaborate.bean.EConfType;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpInsertPic;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpUpdatePic;
import com.kedacom.vconf.sdk.datacollaborate.bean.BoardInfo;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpPaint;
import com.kedacom.vconf.sdk.datacollaborate.bean.ETerminalType;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;


public class DataCollaborateManager extends RequestAgent {

    /*同步过程中缓存的操作*/
    private Map<String, PriorityQueue<OpPaint>> cachedPaintOps = new HashMap<>();

    /* 是否正在准备同步。
    标记从入会成功到开始同步会议中已有图元这段时间，对这段时间内到达的图元
    我们也需像同步图元一样先缓存起来而不是直接上报给用户。*/
    private boolean bPreparingSync = false;

    // 错误码
    public static final int ErrCode_Failed = -1;
    public static final int ErrCode_BuildLink4LoginFailed = -2;
    public static final int ErrCode_BuildLink4ConfFailed = -3;

    private String curDcConfE164;
    public String getCurDcConfE164(){
        return curDcConfE164;
    }

    // 终端类型
    private ETerminalType terminalType;

    // 画板相关通知
    private static final Msg[] boardOpNtfs = new Msg[]{
            Msg.DCCurrentBoardNtf,
            Msg.DCBoardCreatedNtf,
            Msg.DCBoardSwitchedNtf,
            Msg.DCBoardDeletedNtf,
    };

    // 绘制相关通知
    private static final Msg[] paintOpNtfs = new Msg[]{
//            Msg.DCElementBeginNtf,
            Msg.DCLineDrawnNtf,
            Msg.DCOvalDrawnNtf,
            Msg.DCRectDrawnNtf,
            Msg.DCPathDrawnNtf,
            Msg.DCPicInsertedNtf,
            Msg.DCPicDraggedNtf,
            Msg.DCPicDeletedNtf,
            Msg.DCErasedNtf,
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

        processorMap.put(Msg.DCCreateConf, this::onConfOpRsps);
        processorMap.put(Msg.DCReleaseConf, this::onConfOpRsps);
        processorMap.put(Msg.DCQuitConf, this::onConfOpRsps);


        processorMap.put(Msg.DCQueryAllBoards, this::onBoardOpRsps);

        processorMap.put(Msg.DCAddOperator, this::onChangeOperatorsRsps);
        processorMap.put(Msg.DCDelOperator, this::onChangeOperatorsRsps);
        processorMap.put(Msg.DCApplyOperator, this::onChangeOperatorsRsps);
        processorMap.put(Msg.DCCancelOperator, this::onChangeOperatorsRsps);

        processorMap.put(Msg.DCDownload, this::onRsps);
        processorMap.put(Msg.DCQueryPicUrl, this::onRsps);
        processorMap.put(Msg.DCUpload, this::onRsps);
        processorMap.put(Msg.DCQueryPicUploadUrl, this::onRsps);

        processorMap.put(Msg.DCDrawLine, null);
        processorMap.put(Msg.DCDrawOval, null);
        processorMap.put(Msg.DCDrawRect, null);
        processorMap.put(Msg.DCDrawPath, null);
        processorMap.put(Msg.DCUndo, null);
        processorMap.put(Msg.DCRedo, null);
        processorMap.put(Msg.DCClearScreen, null);
        processorMap.put(Msg.DCErase, null);
        processorMap.put(Msg.DCRectErase, null);
//        processorMap.put(Msg.DCZoom, null);
//        processorMap.put(Msg.DCRotateLeft, null);
//        processorMap.put(Msg.DCRotateRight, null);
//        processorMap.put(Msg.DCScrollScreen, null);
        processorMap.put(Msg.DCMatrix, null);
        processorMap.put(Msg.DCInsertPic, null);
        processorMap.put(Msg.DCDeletePic, null);
        processorMap.put(Msg.DCDragPic, null);

        return processorMap;
    }

    @Override
    protected Map<Msg, NtfProcessor> ntfProcessors() {
        Map<Msg, NtfProcessor> processorMap = new HashMap<>();
//        processorMap.put(Msg.DCApplyOperatorNtf, this::onOperatorsChangedNtfs);
        processorMap.put(Msg.DCConfCreated, this::onNtfs);
        processorMap.put(Msg.DCPicDownloadableNtf, this::onNtfs);
        return processorMap;
    }

    @Override
    protected Map<Msg[], NtfProcessor> ntfsProcessors() {
        Map<Msg[], NtfProcessor> processorMap = new HashMap<>();
        processorMap.put(boardOpNtfs, this::onBoardNtfs);
        processorMap.put(paintOpNtfs, this::onPaintNtfs);
        return processorMap;
    }


    private static String PIC_SAVE_DIR;
    private static Context context;

    static Context getContext() {
        return context;
    }

    private DataCollaborateManager(){}
    public static DataCollaborateManager getInstance(Context ctx) {
        if (null == context
                && null != ctx){
            context = ctx;
            File dir = new File(ctx.getCacheDir(), ".dc_pic");
            if (!dir.exists()){
                dir.mkdir();
            }
            PIC_SAVE_DIR = dir.getAbsolutePath();

            // 检查图片缓存文件夹是否已超出大小上限，若超出则清空
            long size = 0;
            for (File file : dir.listFiles()) {
                size += file.length();
            }
            long LIMIT = 50*1024*1024;
            KLog.p("pic cache dir=%s, size=%s, limit=%s", PIC_SAVE_DIR, size, LIMIT);
            if (size > LIMIT){
                KLog.p(KLog.WARN, "clean cached pics");
                for (File file : dir.listFiles()) {
                    file.delete();
                }
            }
        }
        return AgentManager.obtain(DataCollaborateManager.class);
    }


    /**发布绘制操作*/
    public void publishPaintOp(OpPaint op){
        KLog.p("publish op=%s", op);
        Object to = ToDoConverter.toPaintTransferObj(op);
        // TODO 对于插入图片还需调用插入图片然后查询图片地址然后上传图片。
        if (null != to) {
            req(ToDoConverter.opTypeToReqMsg(op.getType()), null,
                    ToDoConverter.toCommonPaintTransferObj(op), ToDoConverter.toPaintTransferObj(op));
        }else{
            req(ToDoConverter.opTypeToReqMsg(op.getType()), null,
                    ToDoConverter.toCommonPaintTransferObj(op));
        }
    }

    /**登录数据协作
     * @param serverIp 数据协作服务器Ip
     * @param port 数据协作服务器port
     * @param terminalType 己端终端类型
     * @param resultListener 登陆结果监听器*/
    public void login(String serverIp, int port, ETerminalType terminalType, IResultListener resultListener){
        this.terminalType = terminalType;
        req(Msg.DCLogin, resultListener, new TDCSRegInfo(serverIp, port, ToDoConverter.toTransferObj(terminalType)));
    }

    /**注销数据协作
     * @param resultListener 注销结果监听器*/
    public void logout(IResultListener resultListener){
        req(Msg.DCLogout, resultListener);
        cachedPaintOps.clear();
        curDcConfE164 = null;
    }

    /**
     * 会话（登陆/注销）响应处理
     * @param rspId 响应消息Id
     * @param rspContent 响应内容
     * @param listener 结果监听器（为请求时传下的）
     * */
    private void onSessionRsps(Msg rspId, Object rspContent, IResultListener listener){
        KLog.p("rspId=%s, rspContent=%s, listener=%s",rspId, rspContent, listener);
        switch (rspId){
            case DCBuildLink4LoginRsp:
                TDCSConnectResult result = (TDCSConnectResult) rspContent;
                if (!result.bSuccess && null != listener){
                    cancelReq(Msg.DCLogin, listener);  // 后续不会有DCLoginRsp上来，取消该请求以防等待超时。
                    listener.onFailed(ErrCode_BuildLink4LoginFailed);
                }
                break;

            case DCLoginRsp:
                TDCSResult loginRes = (TDCSResult) rspContent;
                if (null != listener){
                    if (loginRes.bSucces) {
                        listener.onSuccess(null);
                    }else{
                        listener.onFailed(ErrCode_Failed);
                    }
                }
                break;

            case DCLogoutRsp:
                TDCSResult logoutRes = (TDCSResult) rspContent;
                if (null != listener){
                    if (logoutRes.bSucces){
                        listener.onSuccess(null);
                    }else{
                        listener.onFailed(ErrCode_Failed);
                    }
                }
                break;
        }

    }


    /**创建数据协作
     * @param confE164 会议e164
     * @param confName 会议名称
     * @param dcMode 数据协作模式
     * @param confType 会议类型
     * @param adminE164 主席e164
     * @param members 与会成员
     * @param resultListener 创会结果监听器*/
    public void createDcConf(String confE164, String confName, EDcMode dcMode, EConfType confType, String adminE164, List<DCMember> members, IResultListener resultListener){
        List<TDCSConfUserInfo> tdcsConfUserInfos = new ArrayList<>();
        for (DCMember member : members){
            tdcsConfUserInfos.add(ToDoConverter.toTransferObj(member));
        }
        req(Msg.DCCreateConf, resultListener,
                new TDCSCreateConf(ToDoConverter.toTransferObj(confType),
                        confE164, confName, ToDoConverter.toTransferObj(dcMode),
                        tdcsConfUserInfos, adminE164, ToDoConverter.toTransferObj(terminalType)));
        cachedPaintOps.clear();
        curDcConfE164 = null;
    }

    /**结束数据协作*/
    public void releaseDcConf(IResultListener resultListener){
//        req(Msg.DCReleaseConf, new MsgBeans.DCConfId(curDcConfE164), resultListener);
        curDcConfE164 = null;
        cachedPaintOps.clear();
    }

    /**退出数据协作。
     * 注：仅自己退出，协作仍存在，不影响其他人继续*/
    public void quitDcConf(IResultListener resultListener){
//        req(Msg.DCQuitConf, new MsgBeans.DCSQuitConf(curDcConfE164), resultListener);
        curDcConfE164 = null;
        cachedPaintOps.clear();
    }

    private void onConfOpRsps(Msg rspId, Object rspContent, IResultListener listener){
        switch (rspId){
//            case DCBuildLink4ConfRsp:
//                TDCSConnectResult result = (TDCSConnectResult) rspContent;
//                if (!result.bSuccess && null != listener){
//                    cancelReq(Msg.DCCreateConf, listener);  // 后续不会有DCCreateConfRsp上来，取消该请求以防等待超时。
//                    listener.onFailed(ErrCode_BuildLink4ConfFailed);
//                }
//                break;
            case DCConfCreated:
                TDCSCreateConfResult createConfResult = (TDCSCreateConfResult) rspContent;
                if (createConfResult.bSuccess){
                    curDcConfE164 = createConfResult.achConfE164;
                }
                if (null != listener){
                    if (createConfResult.bSuccess) {
                        listener.onSuccess(ToDoConverter.fromPaintTransferObj(createConfResult));
                    }else{
                        listener.onFailed(ErrCode_Failed);
                    }
                }
                break;
//            case DCReleaseConfRsp:
//                TDCSResult releaseRes = (TDCSResult) rspContent;
//                if (null != listener){
//                    if (releaseRes.bSucces){
//                        listener.onSuccess(null);
//                    }else{
//                        listener.onFailed(ErrCode_Failed);
//                    }
//                }
//                break;
//            case DCQuitConfRsp:
//                TDCSResult quitRes = (TDCSResult) rspContent;
//                if (null != listener){
//                    if (quitRes.bSucces){
//                        listener.onSuccess(null);
//                    }else{
//                        listener.onFailed(ErrCode_Failed);
//                    }
//                }
//                break;
        }

    }



    /**添加协作方*/
    public void addOperator(DCMember[] members, IResultListener resultListener){
//        MsgBeans.TDCSConfUserInfo[] confUserInfos = new MsgBeans.TDCSConfUserInfo[members.length];
//        for (int i=0; i<members.length; ++i){
//            confUserInfos[i] = members[i].toTransferType();
//        }
//        req(Msg.DCAddOperator, new MsgBeans.TDCSOperator(curDcConfE164, confUserInfos), resultListener);
    }

    /**删除协作方*/
    public void delOperator(DCMember[] members, IResultListener resultListener){
//        MsgBeans.TDCSConfUserInfo[] confUserInfos = new MsgBeans.TDCSConfUserInfo[members.length];
//        for (int i=0; i<members.length; ++i){
//            confUserInfos[i] = members[i].toTransferType();
//        }
//        req(Msg.DCDelOperator, new MsgBeans.TDCSOperator(curDcConfE164, confUserInfos), resultListener);
    }

    /**申请协作方
     * @param e164 申请者e164*/
    public void applyForOperator(String e164, IResultListener resultListener){
        req(Msg.DCApplyOperator, resultListener, e164);
    }
    /**取消协作方
     * @param e164 申请者e164*/
    public void cancelOperator(String e164, IResultListener resultListener){
        req(Msg.DCCancelOperator, resultListener, e164);
    }

    /**
     * 协作方变更（添加/删除/申请/取消）响应处理
     * */
    private void onChangeOperatorsRsps(Msg rspId, Object rspContent, IResultListener listener){
        switch (rspId){
            case DCApplyOperatorRsp:
                if (null != listener){
                    if (((TDCSResult) rspContent).bSucces){
                        listener.onSuccess(null);
                    }else{
                        listener.onFailed(ErrCode_Failed);
                    }
                }
                break;
            case DCCancelOperatorRsp:
                if (null != listener){
                    if (((TDCSResult) rspContent).bSucces){
                        listener.onSuccess(null);
                    }else{
                        listener.onFailed(ErrCode_Failed);
                    }
                }
                break;
        }
    }

    private void onOperatorsChangedNtfs(Msg ntfId, Object ntfContent, Set<Object> listeners){

        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
    }


    private void onBoardOpRsps(Msg rspId, Object rspContent, IResultListener listener){
        switch (rspId){
            case DCQueryAllBoardsRsp:
                DcsGetAllWhiteBoardRsp queryAllBoardsResult = (DcsGetAllWhiteBoardRsp) rspContent;
                if (!queryAllBoardsResult.MainParam.bSucces){
                    KLog.p(KLog.ERROR, "DCQueryAllBoards failed, errorCode=%s", queryAllBoardsResult.MainParam.dwErrorCode);
                    if (null != listener) listener.onFailed(ErrCode_Failed);
                    return;
                }

                if (null != listener) {
                    if (listener instanceof QueryAllBoardsInnerListener) {
                        listener.onSuccess(queryAllBoardsResult.AssParam.atBoardInfo);
                    } else {
                        List<BoardInfo> boardInfos = new ArrayList<>();
                        for (TDCSBoardInfo tdcsBoardInfo : queryAllBoardsResult.AssParam.atBoardInfo) {
                            boardInfos.add(ToDoConverter.fromTransferObj(tdcsBoardInfo));
                        }
                        listener.onSuccess(boardInfos);
                    }
                }

                break;
        }
    }


    private String curBoardId;
    private boolean bGotAllBoard;
    /**
     * 画板操作（创建/删除/切换）通知处理
     * */
    private void onBoardNtfs(Msg ntfId, Object ntfContent, Set<Object> listeners){
        KLog.p("listener==%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        if (Msg.DCBoardCreatedNtf.equals(ntfId)) {
            for (Object listener : listeners) {
                ((IOnBoardOpListener) listener).onBoardCreated(ToDoConverter.fromTransferObj((TDCSBoardInfo) ntfContent));
            }
        } else if (Msg.DCBoardSwitchedNtf.equals(ntfId)) {
            for (Object listener : listeners) {
                ((IOnBoardOpListener) listener).onBoardSwitched(((TDCSBoardInfo) ntfContent).achTabId);
            }
        } else if (Msg.DCBoardDeletedNtf.equals(ntfId)) {
            for (Object listener : listeners) {
                ((IOnBoardOpListener) listener).onBoardDeleted(((TDCSDelWhiteBoardInfo) ntfContent).strIndex);
            }
        } else if (Msg.DCCurrentBoardNtf.equals(ntfId)) { //NOTE: 该通知仅在刚入会时会收到
            curBoardId = ((TDCSBoardInfo) ntfContent).achTabId;
            if (bGotAllBoard){ // 已获取当前会议中所有画板则我们可以上报用户“切换画板”。
                for (Object listener : listeners) {
                    ((IOnBoardOpListener) listener).onBoardSwitched(curBoardId);
                }
                bGotAllBoard = false;
            }
        }

    }


    private void onRsps(Msg rspId, Object rspContent, IResultListener listener){
        KLog.p("rspContent=%s", rspContent);
        switch (rspId){
            case DCDownloadRsp:
                TDCSFileLoadResult result = (TDCSFileLoadResult) rspContent;
                if (result.bSuccess){
                    listener.onSuccess(result);
                }else{
                    listener.onFailed(ErrCode_Failed);
                }
                break;

            case DCQueryPicUrlRsp:
                DcsDownloadImageRsp queryPicUrlResult = (DcsDownloadImageRsp) rspContent;
                if (queryPicUrlResult.MainParam.bSucces){
                    listener.onSuccess(queryPicUrlResult);
                }else{
                    listener.onFailed(ErrCode_Failed);
                }
                break;
        }
    }


    private String getPicSavePath(String picId){
        return PIC_SAVE_DIR +File.pathSeparator+ picId + ".jpg";
    }



    private final int MsgID_SynchronizingTimeout = 10;
    private Handler handler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MsgID_SynchronizingTimeout:
                    PriorityQueue<OpPaint> ops = cachedPaintOps.remove(msg.obj);
                    if (null == ops){
                        KLog.p(KLog.ERROR, "unexpected MsgID_SynchronizingTimeout, no such synchronizing board(%s) exists", msg.obj);
                        return;
                    }
                    Set<Object> listeners = getNtfListeners(Msg.DCElementEndNtf);
                    if (null == listeners || listeners.isEmpty()){// 判断监听者是否还在，因为监听者（如activity）可能已经销毁了
                        KLog.p(KLog.ERROR, "listeners for DCElementEndNtf not exists");
                        return;
                    }
                    // 上报用户目前为止已同步的图元操作
                    while (!ops.isEmpty()) {
                        OpPaint opPaint = ops.poll();
                        for (Object listener : listeners) {
                            ((IOnPaintOpListener) listener).onPaintOp(opPaint);
                        }
                    }
                    break;
            }
        }
    };


    /**
     * 绘制操作通知处理
     * */
    private void onPaintNtfs(Msg ntfId, Object ntfContent, Set<Object> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        switch (ntfId){
//            case DCElementBeginNtf:
//                // NOTHING TO DO. NOTE:此通知并不能准确标记批量图元推送的起点。
//                break;
            // 图元同步结束通知
            case DCElementEndNtf:
                /*当前画板已有图元推送结束，我们上报给用户。
                NOTE：之所以推送结束时才上报而不是边收到推送边上报，是因为推送的这些图元操作到达时序可能跟图元操作顺序不一致，
                所以需要收齐后排好序再上报给用户才能保证用户接收时序和图元操作顺序一致，进而正确绘制。
                比如协作方操作顺序是“画线、清屏、画圆”最终效果是一个圆，但推送到达己方的时序可能是“画圆、清屏、画线”，
                若不做处理直接上报用户，用户界面展示的效果将是一条线。*/
                TDcsCacheElementParseResult end = (TDcsCacheElementParseResult) ntfContent;
                handler.removeMessages(MsgID_SynchronizingTimeout, end.achTabId);
                PriorityQueue<OpPaint> ops = cachedPaintOps.remove(end.achTabId);
                if (null == ops){
                    KLog.p(KLog.ERROR, "unexpected DCElementEndNtf, no such synchronizing board(%s) exists", end.achTabId);
                    return;
                }
                // 上报用户图元绘制操作
                while (!ops.isEmpty()) {
                    OpPaint opPaint = ops.poll();
                    for (Object listener : listeners) {
                        ((IOnPaintOpListener) listener).onPaintOp(opPaint);
                    }
                }

                break;

            // 插入图片通知。 NOTE:插入图片比较特殊，通知中只有插入图片操作的基本信息，图片本身还需进一步下载
            case DCPicInsertedNtf:
                DcsOperInsertPicNtf dcInertPicOp = (DcsOperInsertPicNtf) ntfContent;

                cacheOrReportPaintOp(ToDoConverter.fromTransferObj(dcInertPicOp), listeners);

                String confE164 = dcInertPicOp.MainParam.achConfE164;
                String boardId = dcInertPicOp.MainParam.achTabId;
                int pageId = dcInertPicOp.MainParam.dwWbPageId;
                String picId = dcInertPicOp.AssParam.achImgId;
                if (new File(getPicSavePath(picId)).exists()){ // 图片已下载到本地
                    KLog.p("pic already exists: %s", getPicSavePath(picId));
                    updateInsertPicOp(new OpUpdatePic(boardId, picId, getPicSavePath(picId)), listeners);

                }else if (null != cachedPaintOps.get(boardId)){ // 图片尚未下载到本地且正在同步图元
                    /* 获取图片下载地址。
                    * NOTE: 仅在同步图元阶段需要如下这样操作——获取图片的url，然后下载。其他情形均在收到“图片可下载”通知后开始下载图片。
                     之所以要分情形而无法统一处理是因为：一方面刚入会同步过程中不会收到“图片可下载”通知所以需要主动获取下载url然后下载；
                     另一方面除了刚入会同步的场景其它场景下主动获取图片下载url均可能失败，因为图片可能尚未上传到服务器，所以需要等到“图片可下载通知”方可下载*/
                    req(Msg.DCQueryPicUrl,
                        new IResultListener() {
                            @Override
                            public void onSuccess(Object result) {
                                DcsDownloadImageRsp picUrl = (DcsDownloadImageRsp) result;
                                // 下载图片
                                req(Msg.DCDownload,
                                    new IResultListener() {
                                        @Override
                                        public void onSuccess(Object result) {
//                                            KLog.p("download pic %s for board %s success! save path=%s", picUrl.picId, picUrl.boardId, getPicSavePath(picUrl.picId));
                                            TDCSFileLoadResult downRst = (TDCSFileLoadResult) result;
                                            updateInsertPicOp(new OpUpdatePic(downRst.achTabid, downRst.achWbPicentityId, downRst.achFilePathName), listeners);
                                        }

                                        @Override
                                        public void onFailed(int errorCode) {
//                                            KLog.p(KLog.ERROR, "download pic %s for board %s failed!", picUrl.picId, picUrl.boardId);
                                        }

                                        @Override
                                        public void onTimeout() {
//                                            KLog.p(KLog.ERROR, "download pic %s for board %s timeout!", picUrl.picId, picUrl.boardId);
                                        }
                                    },

                                    new BaseTypeString(picUrl.AssParam.achPicUrl),
                                    new TDCSFileInfo(getPicSavePath(picUrl.AssParam.achWbPicentityId), picUrl.AssParam.achWbPicentityId, picUrl.AssParam.achTabId, false)
                                );
                            }

                            @Override
                            public void onFailed(int errorCode) {
//                                KLog.p(KLog.ERROR, "query url of pic %s for board %s failed!", dcInertPicOp.picId, dcInertPicOp.boardId);
                            }

                            @Override
                            public void onTimeout() {
//                                KLog.p(KLog.ERROR, "query url of pic %s for board %s timeout!", dcInertPicOp.picId, dcInertPicOp.boardId);
                            }
                        },

                        new TDCSImageUrl(confE164, boardId, pageId, picId)
                    );
                }

                break;

            default:

                cacheOrReportPaintOp(ToDoConverter.fromPaintTransferObj(ntfContent), listeners);

                break;
        }

    }


    private void cacheOrReportPaintOp(OpPaint op, Set<Object> listeners){
        PriorityQueue<OpPaint> cachedOps = cachedPaintOps.get(op.getBoardId());
        if (null != cachedOps){ // 当前正在同步该画板的图元则缓存图元
            if (!cachedOps.contains(op)) { // 去重。 同步期间有可能收到重复的图元
                cachedOps.offer(op);
            }
        } else {
            if (bPreparingSync){ // 入会后同步前收到的图元也需缓存下来
                PriorityQueue<OpPaint> ops1 = new PriorityQueue<>();
                ops1.offer(op);
                cachedPaintOps.put(op.getBoardId(), ops1);
            }else {
                // 过了同步阶段，直接上报用户图元操作
                for (Object listener : listeners) {
                    ((IOnPaintOpListener) listener).onPaintOp(op);
                }
            }
        }
    }

    private void updateInsertPicOp(OpUpdatePic opUpdatePic, Set<Object> listeners){
        PriorityQueue<OpPaint> cachedOps = cachedPaintOps.get(opUpdatePic.getBoardId());
        if (null != cachedOps){ // 当前正在同步中，插入图片的操作被缓存尚未上报给用户，故我们直接更新“插入图片”的操作
            boolean bUpdated = false;
            for (OpPaint op : cachedOps){
                if (op instanceof OpInsertPic && ((OpInsertPic)op).getPicId().equals(opUpdatePic.getPicId())){
                    ((OpInsertPic)op).setPicSavePath(opUpdatePic.getPicSavePath()); // 更新图片的所在路径
                    bUpdated = true;
                    break;
                }
            }
            if (!bUpdated){
                KLog.p(KLog.ERROR, "update pic %s failed", opUpdatePic.getPicId());
                return;
            }

        }else{ // 同步已结束则上报用户“更新图片”
            if (null == listeners || listeners.isEmpty()){
                KLog.p(KLog.ERROR,"no listener for DCPicInsertedNtf");
                return;
            }
            for (Object onPaintOpListener : listeners) {
                if (!containsNtfListener(onPaintOpListener)) { // 在下载过程中可能listener被销毁了删除了
                    KLog.p(KLog.ERROR,"listener %s for DCPicInsertedNtf has been destroyed", onPaintOpListener);
                    continue;
                }
                ((IOnPaintOpListener) onPaintOpListener).onPaintOp(opUpdatePic);  // 前面我们插入图片的操作并无实际效果，因为图片是“置空”的，此时图片已下载完成，我们更新之前置空的图片。
            }
        }
    }



    private void onNtfs(Msg ntfId, Object ntfContent, Set<Object> listeners) {
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        switch (ntfId){
            // 入会结果通知
            case DCConfCreated:
                TDCSCreateConfResult dcConfinfo = (TDCSCreateConfResult) ntfContent;

                if (!dcConfinfo.bSuccess){
                    // 入会失败
                    curDcConfE164 = null;
                    KLog.p(KLog.ERROR,"join data collaborate conf{%s, %s} failed", dcConfinfo.achConfName, dcConfinfo.achConfE164);
                    for (Object listener : listeners){
                        ((IOnDcConfJoinResultListener)listener).onFailed(ErrCode_Failed);
                    }
                    return;
                }

                curDcConfE164 = dcConfinfo.achConfE164;
                CreateConfResult createConfResult = ToDoConverter.fromTransferObj(dcConfinfo);
                KLog.p("createConfResult: %s", createConfResult);
                for (Object listener : listeners){
                    ((IOnDcConfJoinResultListener)listener).onSuccess(createConfResult);
                }

                // 入会成功后准备同步会议中已有的图元。（入会成功后实时的图元操作可能在任意时间点到达）
                bPreparingSync = true;
                cachedPaintOps.clear();

                // 获取所有画板
                req(Msg.DCQueryAllBoards, new QueryAllBoardsInnerListener() {
                    @Override
                    public void onArrive() {
                        /* 获取所有画板结束，准备阶段结束*/
                        bPreparingSync = false;
                    }

                    @Override
                    public void onSuccess(Object result) {
                        bGotAllBoard = true;
                        List<TDCSBoardInfo> dcBoards = (List<TDCSBoardInfo>) result;
                        // 检查准备阶段缓存的图元所在画板是否仍存在，若不存在则删除之。
                        Iterator it = cachedPaintOps.keySet().iterator();
                        while (it.hasNext()){
                            boolean bMatched = false;
                            String tmpId = (String) it.next();
                            for (TDCSBoardInfo board : dcBoards){
                                if (tmpId.equals(board.achTabId)){
                                    bMatched = true;
                                    break;
                                }
                            }
                            if (!bMatched){
                                it.remove();
                            }
                        }

                        // 上报用户所有已创建的画板
                        Set<Object> boardCreatedListeners = getNtfListeners(Msg.DCBoardCreatedNtf);
                        if (null != boardCreatedListeners && !boardCreatedListeners.isEmpty()){
                            for (TDCSBoardInfo board : dcBoards) {
                                BoardInfo boardInfo = ToDoConverter.fromTransferObj(board);
                                for (Object listener : boardCreatedListeners) {
                                    ((IOnBoardOpListener) listener).onBoardCreated(boardInfo);
                                }
                            }
                        }

                        // 上报用户切换画板
                        if (null != curBoardId){ // “当前画板”通知已早于此到达，彼时还无法通知用户“切换画板”，因为彼时尚未上报用户画板已创建，所以此时我们补上通知“切换画板”。
                            Set<Object> boardSwitchedListeners = getNtfListeners(Msg.DCBoardSwitchedNtf);
                            if (null != boardSwitchedListeners && !boardSwitchedListeners.isEmpty()) {
                                for (Object listener : boardSwitchedListeners) {
                                    ((IOnBoardOpListener) listener).onBoardSwitched(curBoardId);
                                }
                            }
                            curBoardId = null;
                        }

                        // 为各画板创建图元缓存队列（刚入会需同步会议中已有图元）
                        for (TDCSBoardInfo board : dcBoards){
                            PriorityQueue<OpPaint> ops = cachedPaintOps.get(board.achTabId);
                            if (null == ops){ // 若不为null则表明准备阶段已有该画板的实时图元到达，缓存队列在那时已创建，此处复用它即可
                                ops = new PriorityQueue<>();
                                cachedPaintOps.put(board.achTabId, ops);
                            }
                        }

                        // 开始同步所有画板的已有图元
                        for (TDCSBoardInfo board : dcBoards){

                            // 下载每个画板的已有图元
                            req(Msg.DCDownload, new IResultListener() {
                                @Override
                                public void onSuccess(Object result) {
                                    // 后续会批量上报当前画板已有图元，直到收到End消息为止。此处我们开启超时机制防止收不到End消息
                                    Message msg = Message.obtain();
                                    msg.what = MsgID_SynchronizingTimeout;
                                    msg.obj = board.achTabId;
                                    handler.sendMessageDelayed(msg, 10*1000);
                                }

                                @Override
                                public void onFailed(int errorCode) {
//                                    KLog.p(KLog.ERROR, "download paint element for board %s failed!", board.id);
//                                    cachedPaintOps.remove(board.id);
                                }

                                @Override
                                public void onTimeout() {
//                                    KLog.p(KLog.ERROR, "download paint element for board %s timeout!", board.id);
//                                    cachedPaintOps.remove(board.id);
                                }
                            },

                            new BaseTypeString(board.achElementUrl),
                            new TDCSFileInfo(null, null, board.achTabId, true)
                            );

                        }

                    }

                    @Override
                    public void onFailed(int errorCode) {
//                        KLog.p(KLog.ERROR, "DCQueryAllBoards for conf %s failed!", dcConfinfo.confE164);
                    }

                    @Override
                    public void onTimeout() {
//                        KLog.p(KLog.ERROR, "DCQueryAllBoards for conf %s timeout!", dcConfinfo.confE164);
                    }
                },

                createConfResult.getConfE164()
                );

                break;

            // 图片可下载通知。
            /*己端展示图片的过程：
            协作方发出“插入图片”的操作并将图片上传服务器；
            己端先收到“插入图片”的通知，然后需等待“图片可下载”通知；
            一会后图片上传成功，平台广播各与会方“图片可下载”通知；
            己端收到该通知后主动去下载图片到本地；
            己端下载图片完成后结合此前收到的“插入图片”通知内的信息方可展示该图片。
            NOTE：有例外。己端刚加入数据协作时，平台不会给己端发送该通知，己端需要先拉取协作中已有的图元操作
            并针对其中的“插入图片”操作主动查询图片下载地址再根据下载地址下载图片。*/
            case DCPicDownloadableNtf:
                TDCSImageUrl dcPicUrl = (TDCSImageUrl) ntfContent;
                if (!new File(getPicSavePath(dcPicUrl.achWbPicentityId)).exists()){ // 图片尚未下载到本地
                    // 下载图片
                    req(Msg.DCDownload,
                        new IResultListener() {
                            @Override
                            public void onSuccess(Object result) {
                                KLog.p("download pic %s for board %s success! save path=%s",dcPicUrl.achWbPicentityId, dcPicUrl.achTabId, getPicSavePath(dcPicUrl.achWbPicentityId));
                                TDCSFileLoadResult downRst = (TDCSFileLoadResult) result;
                                updateInsertPicOp(new OpUpdatePic(downRst.achTabid, downRst.achWbPicentityId, downRst.achFilePathName), getNtfListeners(Msg.DCPicInsertedNtf));
                            }

                            @Override
                            public void onFailed(int errorCode) {

                            }

                            @Override
                            public void onTimeout() {

                            }
                        },
                        new BaseTypeString(dcPicUrl.achPicUrl),
                        new TDCSFileInfo(getPicSavePath(dcPicUrl.achWbPicentityId), dcPicUrl.achWbPicentityId, dcPicUrl.achTabId, false)
                    );
                }else{
                    KLog.p("pic already exists: %s", getPicSavePath(dcPicUrl.achWbPicentityId));
                }
                break;
        }
    }



    public void addOnDcConfJoinResultListener(IOnDcConfJoinResultListener onDcConfJoinResultListener){
        subscribe(Msg.DCConfCreated, onDcConfJoinResultListener);
    }

    public void addBoardOpListener(IOnBoardOpListener onBoardOpListener){
        subscribe(boardOpNtfs, onBoardOpListener);
    }

    public void addPaintOpListener(IOnPaintOpListener onPaintOpListener){
        subscribe(paintOpNtfs, onPaintOpListener);
    }




    public interface IOnPaintOpListener extends ILifecycleOwner {
        void onPaintOp(OpPaint op);
    }

    public interface IOnBoardOpListener extends ILifecycleOwner{
        void onBoardCreated(BoardInfo boardInfo);
        void onBoardDeleted(String boardId);
        void onBoardSwitched(String boardId);
    }

    public interface IOnDcConfJoinResultListener extends ILifecycleOwner{
        void onSuccess(CreateConfResult result);
        void onFailed(int errCode);
    }

    private class QueryAllBoardsInnerListener implements IResultListener{
    }

    public void ejectNtf(Msg msg){
        eject(msg);
    }

}
