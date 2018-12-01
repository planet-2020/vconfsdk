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
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;


public class DataCollaborateManager extends RequestAgent {

    /*同步过程中缓存的操作*/
    private Map<String, PaintOpsBuffer> cachedPaintOps = new HashMap<>();

    //当前数据协作会议的e164
    private String curDcConfE164;

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
                    PaintOpsBuffer paintOpsBuffer = cachedPaintOps.get(boardId);
                    Set<Object> listeners = getNtfListeners(Msg.DCElementEndNtf);
                    if (null != paintOpsBuffer
                            && (null != listeners && !listeners.isEmpty())){
                        paintOpsBuffer.bSynchronizing = false;
                        PriorityQueue<MsgBeans.DCPaintOp> ops = paintOpsBuffer.cachedOps;
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
    }

    /**退出数据协作。
     * 注：仅自己退出，协作仍存在，不影响其他人继续*/
    public void quitDcConf(IResultListener resultListener){
        req(Msg.DCQuitConf, new MsgBeans.DCSQuitConf(curDcConfE164), resultListener);
        curDcConfE164 = null;
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


    private void onBoardNtfs(Msg ntfId, Object ntfContent, Set<Object> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        MsgBeans.DCBoard dcBoard = (MsgBeans.DCBoard) ntfContent;
        for (Object listener : listeners) {
            IOnBoardOpListener onBoardOpListener = (IOnBoardOpListener) listener;
            if (Msg.DCCurrentBoardNtf.equals(ntfId)) {
                onBoardOpListener.onBoardCreated(ToDoConverter.fromTransferObj(dcBoard));
                onBoardOpListener.onBoardSwitched(dcBoard.id);
            } else if (Msg.DCBoardCreatedNtf.equals(ntfId)) {
                onBoardOpListener.onBoardCreated(ToDoConverter.fromTransferObj(dcBoard));
            } else if (Msg.DCBoardSwitchedNtf.equals(ntfId)) {
                onBoardOpListener.onBoardSwitched(dcBoard.id);
            } else if (Msg.DCBoardDeletedNtf.equals(ntfId)) {
                onBoardOpListener.onBoardDeleted(dcBoard.id);
            }
        }


        if (Msg.DCCurrentBoardNtf.equals(ntfId)) { // 此条通知仅在刚入会时上报，我们开始下载当前会议中所有画板的所有已有图元

            // 获取所有画板
            req(Msg.DCQueryAllBoards, new MsgBeans.DCConfId(dcBoard.confE164), new QueryAllBoardsInnerListener() {
                @Override
                public void onSuccess(Object result) {
                    MsgBeans.DCBoard[] dcBoards = (MsgBeans.DCBoard[]) result;
                    PaintOpsBuffer paintOpsBuffer;
                    cachedPaintOps.clear();
                    for (MsgBeans.DCBoard board : dcBoards){
                        // 上报所有画板
                        if (!dcBoard.id.equals(board.id)){
                            for (Object listener : listeners) {
                                ((IOnBoardOpListener)listener).onBoardCreated(ToDoConverter.fromTransferObj(board));
                            }
                        }

                        paintOpsBuffer = new PaintOpsBuffer();
                        paintOpsBuffer.bSynchronizing = true;
                        cachedPaintOps.put(board.id, paintOpsBuffer);
                        // 下载每个画板已有的图元
                        /* TODO 确认在入会后下载已有图元前，协作方的操作会不会广播到己方，如果会，则需对这种情形做处理
                        （即不能直接上报用户要缓存。方案可以是在收到confCreated通知时置一个标志位，在获取所有画板的响应中遍历如果有对应的boardId则需将之前收到的操作缓存到cachedPaintOps,如果没有就丢弃），
                        不过不会则改进cachedPaintOps，直接用PriorityQueue就好，而且bSyncing也不用了。*/
                        req(Msg.DCDownload, new MsgBeans.DownloadPara(board.id, board.elementUrl), new IResultListener() {
                            @Override
                            public void onSuccess(Object result) {
                                /* 后续会批量上报当前画板已有的图元，直到收到End消息为止。此处我们开启超时机制防止收不到End消息 */
                                Message msg = Message.obtain();
                                msg.what = MsgID_SynchronizingTimeout;
                                msg.obj = board.id;
                                handler.sendMessageDelayed(msg, 20*1000);
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
                    KLog.p(KLog.ERROR, "DCQueryAllBoards for conf %s failed!", dcBoard.confE164);
                }

                @Override
                public void onTimeout() {
                    KLog.p(KLog.ERROR, "DCQueryAllBoards for conf %s timeout!", dcBoard.confE164);
                }
            });
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


    private String genPicFullName(String picId){
        return PIC_SAVE_DIR +"/"+ picId + ".jpg";
    }

    @SuppressWarnings("ConstantConditions")
    private void onPaintNtfs(Msg ntfId, Object ntfContent, Set<Object> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        switch (ntfId){
            case DCElementBeginNtf:
                // NOTHING TO DO. NOTE:此通知并不能准确标记批量图元推送的起点，我们以下载作为起点。
                break;
            case DCElementEndNtf:
                KLog.p("batch paint ops <<<<<<<<<<<<<<<<<<<<<<<");

                handler.removeMessages(MsgID_SynchronizingTimeout);

                /*当前画板已有图元推送结束，我们上报给用户。
                NOTE：之所以推送结束时才上报而不是边收到推送边上报，是因为推送的这些图元操作到达时序可能跟图元操作顺序不一致，
                所以需要收齐后排好序再上报给用户才能保证用户接收时序和图元操作顺序一致，进而正确绘制。
                比如协作方操作顺序是“画线、清屏、画圆”最终效果是一个圆，但推送到达己方的时序可能是“画圆、清屏、画线”，
                若不做处理直接上报用户，用户界面展示的效果将是一条线。*/
                MsgBeans.DCBoardId boardId = (MsgBeans.DCBoardId) ntfContent;
                PaintOpsBuffer paintOpsBuffer = cachedPaintOps.get(boardId.boardId);
                if (null != paintOpsBuffer && paintOpsBuffer.bSynchronizing){
                    paintOpsBuffer.bSynchronizing = false;
                    PriorityQueue<MsgBeans.DCPaintOp> ops = paintOpsBuffer.cachedOps;
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

                /* 仅需要在刚入会同步会议中已有图元时需要主动请求获取图片的url，因为此种场景不会上报DCPicDownloadableNtf通知。
                其他情形下都以DCPicDownloadableNtf通知为下载图片的时机*/
                if (null != cachedPaintOps.get(dcInertPicOp.boardId)
                        && cachedPaintOps.get(dcInertPicOp.boardId).bSynchronizing){

                    // TODO 判断本地是否有图片，有的话直接获取否则下载
                    // 获取图片下载地址
                    req(Msg.DCQueryPicUrl,
                        new MsgBeans.DCQueryPicUrlPara(dcInertPicOp.picId, dcInertPicOp.confE164, dcInertPicOp.boardId, dcInertPicOp.pageId),
                        new IResultListener(){
                            @Override
                            public void onSuccess(Object result) {
                                MsgBeans.DCQueryPicUrlResult queryPicUrlResult = (MsgBeans.DCQueryPicUrlResult) result;
                                // 下载图片
                                req(Msg.DCDownload, new MsgBeans.DownloadPara(queryPicUrlResult.boardId, queryPicUrlResult.picId, genPicFullName(queryPicUrlResult.picId), queryPicUrlResult.url),
                                    new IResultListener() {
                                        @Override
                                        public void onSuccess(Object result) {
                                            KLog.p("download pic %s for board %s success! save path=%s",
                                                    queryPicUrlResult.picId, queryPicUrlResult.boardId, PIC_SAVE_DIR +"/"+ queryPicUrlResult.picId + ".jpg");
                                            MsgBeans.DownloadResult downloadResult = (MsgBeans.DownloadResult) result;
                                            OpPaint op = new OpUpdatePic(downloadResult.boardId, downloadResult.picId, BitmapFactory.decodeFile(downloadResult.picSavePath));
                                            for (Object onPaintOpListener : listeners) {
                                                if (containsNtfListener(onPaintOpListener)) { // 在下载过程中可能listener销毁了删除了，所以需做此判断
                                                    ((IOnPaintOpListener) onPaintOpListener).onPaintOp(op);  // 前面我们插入图片的操作并无实际效果，因为图片是“置空”的，此时图片已下载完成，我们更新之前置空的图片。
                                                }
                                            }
                                        }

                                        @Override
                                        public void onFailed(int errorCode) {
                                            KLog.p(KLog.ERROR, "download pic %s for board %s failed!", queryPicUrlResult.picId, queryPicUrlResult.boardId);
                                        }

                                        @Override
                                        public void onTimeout() {
                                            KLog.p(KLog.ERROR, "download pic %s for board %s timeout!", queryPicUrlResult.picId, queryPicUrlResult.boardId);
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
                    PaintOpsBuffer opsBuffer = cachedPaintOps.get(dcPaintOp.boardId);
                    if (null != opsBuffer && opsBuffer.bSynchronizing){
                        opsBuffer.cachedOps.offer(dcPaintOp);
                    } else {
                        OpPaint paintOp = ToDoConverter.fromTransferObj(dcPaintOp);
                        if (null != paintOp) {
                            for (Object listener : listeners) {
                                ((IOnPaintOpListener) listener).onPaintOp(paintOp);
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
            // 入会成功。NOTE:创会响应共用此消息
            case DCConfCreated:
                MsgBeans.DCCreateConfResult dcCreateConfResult = (MsgBeans.DCCreateConfResult) ntfContent;
                curDcConfE164 = dcCreateConfResult.confE164;
                CreateConfResult createConfResult = ToDoConverter.fromTransferObj(dcCreateConfResult);
                KLog.p("createConfResult=%s", createConfResult);
                for (Object listener : listeners){
                    ((INotificationListener)listener).onNotification(createConfResult);
                }
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
                if (!new File(genPicFullName(dcPicUrl.picId)).exists()){
                    // 下载图片
                    req(Msg.DCDownload, new MsgBeans.DownloadPara(dcPicUrl.boardId, dcPicUrl.picId, genPicFullName(dcPicUrl.picId), dcPicUrl.url),
                        new IResultListener() {
                            @Override
                            public void onSuccess(Object result) {
                                KLog.p("download pic %s for board %s success! save path=%s",
                                        dcPicUrl.picId, dcPicUrl.boardId, genPicFullName(dcPicUrl.picId));
                                MsgBeans.DownloadResult downloadResult = (MsgBeans.DownloadResult) result;
                                OpPaint op = new OpUpdatePic(downloadResult.boardId, downloadResult.picId, BitmapFactory.decodeFile(downloadResult.picSavePath)); // 该通知一定是在插入图片通知之后，所以此处update的目标对象已经存在。
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


    public void addOnDcConfJoinedListener(INotificationListener onConfJoinedListener){
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

    private class PaintOpsBuffer{
        private boolean bSynchronizing = false;
        /*使用优先级队列自动为图元操作排序*/
        private PriorityQueue<MsgBeans.DCPaintOp> cachedOps = new PriorityQueue<>();
    }

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
