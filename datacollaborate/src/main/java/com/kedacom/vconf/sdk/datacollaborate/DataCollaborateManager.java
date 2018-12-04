package com.kedacom.vconf.sdk.datacollaborate;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.kedacom.vconf.sdk.base.AgentManager;
import com.kedacom.vconf.sdk.base.ILifecycleOwner;
import com.kedacom.vconf.sdk.base.INotificationListener;
import com.kedacom.vconf.sdk.base.IResultListener;
import com.kedacom.vconf.sdk.base.Msg;
import com.kedacom.vconf.sdk.base.MsgBeans;
import com.kedacom.vconf.sdk.base.RequestAgent;
import com.kedacom.vconf.sdk.base.KLog;
import com.kedacom.vconf.sdk.datacollaborate.bean.CreateConfResult;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCMember;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpUpdatePic;
import com.kedacom.vconf.sdk.datacollaborate.bean.BoardInfo;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpPaint;
import com.kedacom.vconf.sdk.datacollaborate.bean.ETerminalType;

//import static com.kedacom.vconf.sdk.base.MsgBeans.*; // TODO 使用static import？

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;


public class DataCollaborateManager extends RequestAgent {

    /*同步过程中缓存的操作*/
    private Map<String, PriorityQueue<MsgBeans.DCPaintOp>> cachedPaintOps = new HashMap<>();

    //当前数据协作会议的e164
    private String curDcConfE164;


    /* 是否正在准备同步。
    标记从入会成功到开始同步会议中已有图元这段时间，对这段时间内到达的图元
    我们也需像同步图元一样先缓存起来而不是直接上报给用户。*/
    private boolean bPreparingSync = false;


    // 画板相关通知
    private static final Msg[] boardOpNtfs = new Msg[]{
            Msg.DCCurrentBoardNtf,
            Msg.DCBoardCreatedNtf,
            Msg.DCBoardSwitchedNtf,
            Msg.DCBoardDeletedNtf,
    };

    // 绘制相关通知
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


    // 错误码
    public static final int ErrCode_Failed = -1;
    public static final int ErrCode_BuildLink4LoginFailed = -2;
    public static final int ErrCode_BuildLink4ConfFailed = -3;

    private final int MsgID_SynchronizingTimeout = 10;
    private Handler handler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MsgID_SynchronizingTimeout:
                    String boardId = (String) msg.obj;
                    PriorityQueue<MsgBeans.DCPaintOp> ops = cachedPaintOps.remove(boardId);
                    if (null != ops){
                        Set<Object> listeners = getNtfListeners(Msg.DCElementEndNtf);
                        if(null != listeners && !listeners.isEmpty()){ // 判断监听者是否还在，因为监听者（如activity）可能已经销毁了
                            while (!ops.isEmpty()) {
                                OpPaint opPaint = ToDoConverter.fromTransferObj(ops.poll());
                                if (null != opPaint) {
                                    for (Object listener : listeners) {
                                        ((IOnPaintOpListener) listener).onPaintOp(opPaint);
                                    }
                                }
                            }
                        }
                    }
                    break;
            }
        }
    };

    private static String PIC_SAVE_DIR;
    private static Context context;
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

    /**发送绘制操作*/
    public void postPaintOp(OpPaint op){
//        req();
    }

    /**登录数据协作*/
    public void login(String serverIp, int port, ETerminalType terminalType, IResultListener resultListener){
        req(Msg.DCLogin, new MsgBeans.DCLoginPara(serverIp, port, ToDoConverter.toTransferObj(terminalType)), resultListener);
    }

    /**注销数据协作*/
    public void logout(IResultListener resultListener){
        req(Msg.DCLogout, null, resultListener);
        cachedPaintOps.clear();
    }

    private void onSessionRsps(Msg rspId, Object rspContent, IResultListener listener){
        KLog.p("rspId=%s, rspContent=%s, listener=%s",rspId, rspContent, listener);
        MsgBeans.CommonResult result;
        switch (rspId){
            case DCBuildLink4LoginRsp:
                result = (MsgBeans.CommonResult) rspContent;
                if (!result.bSuccess && null != listener){
                    cancelReq(Msg.DCLogin, listener);  // 后续不会有DCLoginRsp上来，取消该请求以防等待超时。
                    listener.onFailed(ErrCode_BuildLink4LoginFailed);
                }
                break;

            case DCLoginRsp:
                result = (MsgBeans.CommonResult) rspContent;
                if (null != listener){
                    if (result.bSuccess) {  // ??? 需要保存登录状态吗
                        listener.onSuccess(null);
                    }else{
                        listener.onFailed(ErrCode_Failed);
                    }
                }
                break;

            case DCLogoutRsp:
                result = (MsgBeans.CommonResult) rspContent;
                if (null != listener){
                    if (result.bSuccess){
                        listener.onSuccess(null);
                    }else{
                        listener.onFailed(ErrCode_Failed);
                    }
                }
                break;
        }

    }



    /**创建数据协作*/
    public void createDcConf(IResultListener resultListener){
        req(Msg.DCCreateConf, new MsgBeans.DCCreateConfPara(), resultListener);
    }

    /**结束数据协作*/
    public void releaseDcConf(IResultListener resultListener){
        req(Msg.DCReleaseConf, new MsgBeans.DCConfId(curDcConfE164), resultListener);
        curDcConfE164 = null;
        cachedPaintOps.clear();
    }

    /**退出数据协作。
     * 注：仅自己退出，协作仍存在，不影响其他人继续*/
    public void quitDcConf(IResultListener resultListener){
        req(Msg.DCQuitConf, new MsgBeans.DCSQuitConf(curDcConfE164), resultListener);
        curDcConfE164 = null;
        cachedPaintOps.clear();
    }

    private void onConfOpRsps(Msg rspId, Object rspContent, IResultListener listener){
        MsgBeans.CommonResult result;
        switch (rspId){
            case DCBuildLink4ConfRsp:
                result = (MsgBeans.CommonResult) rspContent;
                if (!result.bSuccess && null != listener){
                    cancelReq(Msg.DCCreateConf, listener);  // 后续不会有DCCreateConfRsp上来，取消该请求以防等待超时。
                    listener.onFailed(ErrCode_BuildLink4ConfFailed);
                }
                break;
            case DCConfCreated:
                MsgBeans.DCCreateConfResult createConfResult = (MsgBeans.DCCreateConfResult) rspContent;
//                curDcConfE164 = createConfResult.confE164;
                if (null != listener){
                    if (createConfResult.bSuccess) {
                        listener.onSuccess(ToDoConverter.fromTransferObj(createConfResult));
                    }else{
                        listener.onFailed(ErrCode_Failed);
                    }
                }
                break;
            case DCReleaseConfRsp:
                result = (MsgBeans.CommonResult) rspContent;
                if (null != listener){
                    if (result.bSuccess){
                        listener.onSuccess(null);
                    }else{
                        listener.onFailed(ErrCode_Failed);
                    }
                }
                break;
            case DCQuitConfRsp:
                result = (MsgBeans.CommonResult) rspContent;
                if (null != listener){
                    if (result.bSuccess){
                        listener.onSuccess(null);
                    }else{
                        listener.onFailed(ErrCode_Failed);
                    }
                }
                break;
        }

    }



    /**添加协作方*/
    public void addOperator(DCMember[] members, IResultListener resultListener){
        if (null == curDcConfE164) {
            KLog.p(KLog.ERROR,"not in DC conf yet!");
            return;
        }
//        MsgBeans.TDCSConfUserInfo[] confUserInfos = new MsgBeans.TDCSConfUserInfo[members.length];
//        for (int i=0; i<members.length; ++i){
//            confUserInfos[i] = members[i].toTransferType();
//        }
//        req(Msg.DCAddOperator, new MsgBeans.TDCSOperator(curDcConfE164, confUserInfos), resultListener);
    }

    /**删除协作方*/
    public void delOperator(DCMember[] members, IResultListener resultListener){
        if (null == curDcConfE164) {
            KLog.p(KLog.ERROR,"not in DC conf yet!");
            return;
        }
//        MsgBeans.TDCSConfUserInfo[] confUserInfos = new MsgBeans.TDCSConfUserInfo[members.length];
//        for (int i=0; i<members.length; ++i){
//            confUserInfos[i] = members[i].toTransferType();
//        }
//        req(Msg.DCDelOperator, new MsgBeans.TDCSOperator(curDcConfE164, confUserInfos), resultListener);
    }

    /**申请协作方*/
    public void applyForOperator(String e164, IResultListener resultListener){
        if (null == curDcConfE164) {
            KLog.p(KLog.ERROR,"not in DC conf yet!");
            return;
        }
//        req(Msg.DCApplyOperator, new MsgBeans.DCSBriefConfInfo(e164), resultListener);
    }
    /**取消协作方*/
    public void cancelOperator(String e164, IResultListener resultListener){
        if (null == curDcConfE164) {
            KLog.p(KLog.ERROR,"not in DC conf yet!");
            return;
        }
//        req(Msg.DCCancelOperator, new MsgBeans.DCSBriefConfInfo(e164), resultListener);
    }

    private void onChangeOperatorsRsps(Msg rspId, Object rspContent, IResultListener listener){
        MsgBeans.CommonResult result = (MsgBeans.CommonResult) rspContent;
        if (null != listener){
            if (result.bSuccess){
                listener.onSuccess(null);
            }else{
                listener.onFailed(ErrCode_Failed);
            }
        }
    }

    private void onOperatorsChangedNtfs(Msg ntfId, Object ntfContent, Set<Object> listeners){

        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
    }


    private void onBoardOpRsps(Msg rspId, Object rspContent, IResultListener listener){
        switch (rspId){
            case DCQueryAllBoardsRsp:
                MsgBeans.DCQueryAllBoardsResult queryAllBoardsResult = (MsgBeans.DCQueryAllBoardsResult) rspContent;
                if (!queryAllBoardsResult.bSuccess){
                    KLog.p(KLog.ERROR, "DCQueryAllBoards failed, errorCode=%s", queryAllBoardsResult.errorCode);
                    if (null != listener) listener.onFailed(ErrCode_Failed);
                    return;
                }

                if (null != listener) {
                    if (listener instanceof QueryAllBoardsInnerListener) {
                        listener.onSuccess(queryAllBoardsResult.boards);
                    } else {
                        BoardInfo[] boardInfos = new BoardInfo[queryAllBoardsResult.boards.length];
                        for (int i = 0; i < boardInfos.length; ++i) {
                            boardInfos[i] = ToDoConverter.fromTransferObj(queryAllBoardsResult.boards[i]);
                        }
                        listener.onSuccess(boardInfos);
                    }
                }

                break;
        }
    }


    private String curBoardId;
    private boolean bGotAllBoard = false;
    private void onBoardNtfs(Msg ntfId, Object ntfContent, Set<Object> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        MsgBeans.DCBoard dcBoard = (MsgBeans.DCBoard) ntfContent;
        for (Object listener : listeners) {
            IOnBoardOpListener onBoardOpListener = (IOnBoardOpListener) listener;
            if (Msg.DCBoardCreatedNtf.equals(ntfId)) {
                onBoardOpListener.onBoardCreated(ToDoConverter.fromTransferObj(dcBoard));
            } else if (Msg.DCBoardSwitchedNtf.equals(ntfId)) {
                onBoardOpListener.onBoardSwitched(dcBoard.id);
            } else if (Msg.DCBoardDeletedNtf.equals(ntfId)) {
                onBoardOpListener.onBoardDeleted(dcBoard.id);
            }
        }

        if (Msg.DCCurrentBoardNtf.equals(ntfId)) { // NOTE: 入会通知一定是早于当前画板通知到达
            curBoardId = dcBoard.id;
            if (bGotAllBoard){ // 已获取当前会议中所有画板则我们可以上报用户切换画板。
                for (Object listener : listeners) {
                    ((IOnBoardOpListener) listener).onBoardSwitched(dcBoard.id);
                }
                bGotAllBoard = false;
            }
        }

    }


    private void onRsps(Msg rspId, Object rspContent, IResultListener listener){
        KLog.p("rspContent=%s", rspContent);
        switch (rspId){
            case DCDownloadRsp:
                MsgBeans.DownloadResult result = (MsgBeans.DownloadResult) rspContent;
                if (result.bSuccess){
                    listener.onSuccess(result);
                }else{
                    listener.onFailed(ErrCode_Failed);
                }
                break;

            case DCQueryPicUrlRsp:
                MsgBeans.DCQueryPicUrlResult queryPicUrlResult = (MsgBeans.DCQueryPicUrlResult) rspContent;
                if (queryPicUrlResult.bSuccess){
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

    @SuppressWarnings("ConstantConditions")
    private void onPaintNtfs(Msg ntfId, Object ntfContent, Set<Object> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        switch (ntfId){
            case DCElementBeginNtf:
                // NOTHING TO DO. NOTE:此通知并不能准确标记批量图元推送的起点。
                break;
            case DCElementEndNtf:
                /*当前画板已有图元推送结束，我们上报给用户。
                NOTE：之所以推送结束时才上报而不是边收到推送边上报，是因为推送的这些图元操作到达时序可能跟图元操作顺序不一致，
                所以需要收齐后排好序再上报给用户才能保证用户接收时序和图元操作顺序一致，进而正确绘制。
                比如协作方操作顺序是“画线、清屏、画圆”最终效果是一个圆，但推送到达己方的时序可能是“画圆、清屏、画线”，
                若不做处理直接上报用户，用户界面展示的效果将是一条线。*/
                MsgBeans.DCBoardId boardId = (MsgBeans.DCBoardId) ntfContent;
                handler.removeMessages(MsgID_SynchronizingTimeout, boardId.boardId);
                PriorityQueue<MsgBeans.DCPaintOp> ops = cachedPaintOps.remove(boardId.boardId);
                if (null != ops){
                    while (!ops.isEmpty()) {
                        OpPaint opPaint = ToDoConverter.fromTransferObj(ops.poll());
                        if (null != opPaint) {
                            for (Object listener : listeners) {
                                ((IOnPaintOpListener) listener).onPaintOp(opPaint);
                            }
                        }
                    }
                }

                break;

            case DCPicInsertedNtf:
                // NOTE:插入图片比较特殊，当前只获取到了插入操作的基本信息，图片本身还需进一步下载

                MsgBeans.DCInertPicOp dcInertPicOp = (MsgBeans.DCInertPicOp) ntfContent;

                /* 查询图片下载地址然后下载图片。
                NOTE: 仅在刚入会同步会议中已有图元时需要主动请求获取图片的url然后下载，因为此种场景不会上报“图片可下载”通知。
                其他情形下均在收到“图片可下载”通知后开始下载图片。*/
                if (null != cachedPaintOps.get(dcInertPicOp.boardId) // 正在同步图元
                        && !new File(getPicSavePath(dcInertPicOp.picId)).exists()){ // 图片未被下载过

                    // 获取图片下载地址
                    req(Msg.DCQueryPicUrl,
                        new MsgBeans.DCQueryPicUrlPara(dcInertPicOp.picId, dcInertPicOp.confE164, dcInertPicOp.boardId, dcInertPicOp.pageId),
                        new IResultListener(){
                            @Override
                            public void onSuccess(Object result) {
                                MsgBeans.DCQueryPicUrlResult picUrl = (MsgBeans.DCQueryPicUrlResult) result;
                                // 下载图片
                                req(Msg.DCDownload, new MsgBeans.DownloadPara(picUrl.boardId, picUrl.picId, getPicSavePath(picUrl.picId), picUrl.url),
                                    new IResultListener() {
                                        @Override
                                        public void onSuccess(Object result) {
                                            KLog.p("download pic %s for board %s success! save path=%s",picUrl.picId, picUrl.boardId, getPicSavePath(picUrl.picId));
                                            MsgBeans.DownloadResult downRst = (MsgBeans.DownloadResult) result;
//                                            PriorityQueue<MsgBeans.DCPaintOp> cachedOps = cachedPaintOps.get(downRst.boardId); // TODO 需要先将缓存的图元操作转换为DO
//                                            if (null != cachedOps){ // 当前正在同步中，插入图片的操作被缓存尚未上报给用户，故我们直接更新“插入图片”的操作
//                                            }
                                            OpPaint op = new OpUpdatePic(downRst.boardId, downRst.picId, downRst.picSavePath); // TODO 如果同步已结束则上报，否则不用上报因为同步完成后会统一刷新。不用解码，让painter去做（只需保证painter获取到图片路径）。但是如果下载过程中painter尝试解码图片不会有问题吗？怎么判断已下载完？大小？
                                            for (Object onPaintOpListener : listeners) {
                                                if (containsNtfListener(onPaintOpListener)) { // 在下载过程中可能listener销毁了删除了，所以需做此判断
                                                    ((IOnPaintOpListener) onPaintOpListener).onPaintOp(op);  // 前面我们插入图片的操作并无实际效果，因为图片是“置空”的，此时图片已下载完成，我们更新之前置空的图片。
                                                }
                                            }
                                        }

                                        @Override
                                        public void onFailed(int errorCode) {
                                            KLog.p(KLog.ERROR, "download pic %s for board %s failed!", picUrl.picId, picUrl.boardId);
                                        }

                                        @Override
                                        public void onTimeout() {
                                            KLog.p(KLog.ERROR, "download pic %s for board %s timeout!", picUrl.picId, picUrl.boardId);
                                        }
                                    });
                            }

                            @Override
                            public void onFailed(int errorCode) {
                                KLog.p(KLog.ERROR, "query url of pic %s for board %s failed!", dcInertPicOp.picId, dcInertPicOp.boardId);
                            }

                            @Override
                            public void onTimeout() {
                                KLog.p(KLog.ERROR, "query url of pic %s for board %s timeout!", dcInertPicOp.picId, dcInertPicOp.boardId);
                            }
                        });
                }
            default:
                if (ntfContent instanceof MsgBeans.DCPaintOp) {
                    MsgBeans.DCPaintOp dcPaintOp = (MsgBeans.DCPaintOp) ntfContent;
                    PriorityQueue<MsgBeans.DCPaintOp> cachedOps = cachedPaintOps.get(dcPaintOp.boardId);
                    if (null != cachedOps){ // 正在同步该画板的图元
                        if (!cachedOps.contains(dcPaintOp)) {
                            cachedOps.offer(dcPaintOp);
                        }
                    } else {
                        if (bPreparingSync){ // 入会后同步前收到的图元也需缓存下来
                            PriorityQueue<MsgBeans.DCPaintOp> ops1 = new PriorityQueue<>();
                            ops1.offer(dcPaintOp);
                            cachedPaintOps.put(dcPaintOp.boardId, ops1);
                        }else {
                            OpPaint paintOp = ToDoConverter.fromTransferObj(dcPaintOp);
                            if (null != paintOp) {
                                for (Object listener : listeners) {
                                    ((IOnPaintOpListener) listener).onPaintOp(paintOp);
                                }
                            }
                        }
                    }
                }

                break;
        }

    }


    private void onNtfs(Msg ntfId, Object ntfContent, Set<Object> listeners) {
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        switch (ntfId){
            // 入会通知
            case DCConfCreated:
                MsgBeans.DCCreateConfResult dcConfinfo = (MsgBeans.DCCreateConfResult) ntfContent;
                curDcConfE164 = dcConfinfo.confE164;
                CreateConfResult createConfResult = ToDoConverter.fromTransferObj(dcConfinfo);
                KLog.p("createConfResult: %s", createConfResult);
                // 上报用户入会通知
                for (Object listener : listeners){
                    ((INotificationListener)listener).onNotification(createConfResult);
                }

                if (!dcConfinfo.bSuccess){
                    KLog.p(KLog.ERROR,"join data collaborate conf{%s, %s} failed", dcConfinfo.confName, dcConfinfo.confE164);
                    return; // 入会失败
                }

                // 入会成功后准备同步会议中已有的图元。（入会成功后实时的图元操作可能在任意时间点到达）
                bPreparingSync = true;
                cachedPaintOps.clear();

                // 获取所有画板
                req(Msg.DCQueryAllBoards, new MsgBeans.DCConfId(dcConfinfo.confE164), new QueryAllBoardsInnerListener() {
                    @Override
                    public void onResultArrived() {
                        /* 获取所有画板结束，准备阶段结束*/
                        bPreparingSync = false;
                    }

                    @Override
                    public void onSuccess(Object result) {
                        bGotAllBoard = true;
                        MsgBeans.DCBoard[] dcBoards = (MsgBeans.DCBoard[]) result;
                        // 检查准备阶段缓存的图元所在画板是否仍存在，若不存在则删除之。
                        Iterator it = cachedPaintOps.keySet().iterator();
                        while (it.hasNext()){
                            boolean bMatched = false;
                            String tmpId = (String) it.next();
                            for (MsgBeans.DCBoard board : dcBoards){
                                if (tmpId.equals(board.id)){
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
                            for (Object listener : boardCreatedListeners) {
                                for (MsgBeans.DCBoard board : dcBoards) {
                                    ((IOnBoardOpListener) listener).onBoardCreated(ToDoConverter.fromTransferObj(board));
                                }
                            }
                        }

                        // 上报用户切换画板
                        if (null != curBoardId){ // 当前画板通知已早于此到达，彼时还无法通知用户切换画板，因为彼时尚未上报用户画板已创建，所以此时我们补上通知用户切换画板。
                            Set<Object> boardSwitchedListeners = getNtfListeners(Msg.DCBoardSwitchedNtf);
                            if (null != boardSwitchedListeners && !boardSwitchedListeners.isEmpty()) {
                                for (Object listener : boardSwitchedListeners) {
                                    ((IOnBoardOpListener) listener).onBoardSwitched(curBoardId);
                                }
                            }
                            curBoardId = null;
                        }

                        // 为各画板创建图元缓存队列
                        for (MsgBeans.DCBoard board : dcBoards){
                            PriorityQueue<MsgBeans.DCPaintOp> ops = cachedPaintOps.get(board.id);
                            if (null == ops){ // 若不为null则表明准备阶段已有该画板的实时图元到达，缓存队列在那时已创建，此处复用它即可
                                ops = new PriorityQueue<>();
                                cachedPaintOps.put(board.id, ops);
                            }
                        }

                        // 开始同步所有画板已有图元
                        for (MsgBeans.DCBoard board : dcBoards){

                            // 下载每个画板已有的图元
                            req(Msg.DCDownload, new MsgBeans.DownloadPara(board.id, board.elementUrl), new IResultListener() {
                                @Override
                                public void onSuccess(Object result) {
                                    // 后续会批量上报当前画板已有的图元，直到收到End消息为止。此处我们开启超时机制防止收不到End消息
                                    Message msg = Message.obtain();
                                    msg.what = MsgID_SynchronizingTimeout;
                                    msg.obj = board.id;
                                    handler.sendMessageDelayed(msg, 10*1000);
                                }

                                @Override
                                public void onFailed(int errorCode) {
                                    KLog.p(KLog.ERROR, "download paint element for board %s failed!", board.id);
                                    cachedPaintOps.remove(board.id);
                                }

                                @Override
                                public void onTimeout() {
                                    KLog.p(KLog.ERROR, "download paint element for board %s timeout!", board.id);
                                    cachedPaintOps.remove(board.id);
                                }
                            });

                        }

                    }

                    @Override
                    public void onFailed(int errorCode) {
                        KLog.p(KLog.ERROR, "DCQueryAllBoards for conf %s failed!", dcConfinfo.confE164);
                    }

                    @Override
                    public void onTimeout() {
                        KLog.p(KLog.ERROR, "DCQueryAllBoards for conf %s timeout!", dcConfinfo.confE164);
                    }
                });

                break;

            // 图片可下载。
            /*己端展示图片的过程：
            协作方发出“插入图片”的操作并将图片上传服务器；
            己端先收到“插入图片”的通知，然后需等待“图片可下载”通知；
            一会后图片上传成功，平台广播各与会方“图片可下载”通知；
            己端收到该通知后主动去下载图片到本地；
            己端下载图片完成后结合此前收到的“插入图片”通知内的信息方可展示该图片。
            NOTE：有例外。己端刚加入数据协作时，平台不会给己端发送该通知，己端需要先拉取协作中已有的图元操作
            并针对其中的“插入图片”操作主动查询图片下载地址再根据下载地址下载图片。*/
            case DCPicDownloadableNtf:
                MsgBeans.DCPicUrl dcPicUrl = (MsgBeans.DCPicUrl) ntfContent;
                if (!new File(getPicSavePath(dcPicUrl.picId)).exists()){
                    // 下载图片
                    req(Msg.DCDownload, new MsgBeans.DownloadPara(dcPicUrl.boardId, dcPicUrl.picId, getPicSavePath(dcPicUrl.picId), dcPicUrl.url),
                        new IResultListener() {
                            @Override
                            public void onSuccess(Object result) {
                                KLog.p("download pic %s for board %s success! save path=%s",
                                        dcPicUrl.picId, dcPicUrl.boardId, getPicSavePath(dcPicUrl.picId));
                                MsgBeans.DownloadResult downloadResult = (MsgBeans.DownloadResult) result;
                                OpPaint op = new OpUpdatePic(downloadResult.boardId, downloadResult.picId, downloadResult.picSavePath); // 该通知一定是在插入图片通知之后，所以此处update的目标对象已经存在。
                                for (Object onPaintOpListener : listeners) {
                                    if (containsNtfListener(onPaintOpListener)) { // 在下载过程中可能listener销毁了删除了，所以需做此判断
                                        ((IOnPaintOpListener) onPaintOpListener).onPaintOp(op);  // 前面我们插入图片的操作并无实际效果，因为图片是“置空”的，此时图片已下载完成，我们更新之前置空的图片。
                                    }
                                }
                            }
                        });
                }
                break;
        }
    }


    public void addOnDcConfJoinedListener(INotificationListener onConfJoinedListener){ // TODO 改为addOnDcConfJoinResultListener，onSuccess, onFailed，通知响应消息体剔除掉bSuccess字段。
        subscribe(Msg.DCConfCreated, onConfJoinedListener);
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


    private class QueryAllBoardsInnerListener implements IResultListener{
    }

//    private class PaintOpsBuffer{
//        private boolean bSynchronizing = false;
//        /*使用优先级队列自动为图元操作排序*/
//        private PriorityQueue<MsgBeans.DCPaintOp> cachedOps = new PriorityQueue<>();
//    }

    // FIXME just for debug
    private void pushCachedOps(){
        eject(paintOpNtfs);
    }

    public void ejectNtf(Msg msg){
//        if (!BuildConfig.DEBUG){
//            return;
//        }
//        eject(Msg.DCConfCreated);
        eject(msg);
    }




}
