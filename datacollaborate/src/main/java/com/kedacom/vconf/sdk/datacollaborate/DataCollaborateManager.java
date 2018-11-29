package com.kedacom.vconf.sdk.datacollaborate;

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

    private DataCollaborateManager(){}
    public static DataCollaborateManager getInstance() {
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
                    listener.onFailed(ErrCode_BuildLink4LoginFailed, null);
                }
                break;

            case DCLoginRsp:
                result = (MsgBeans.CommonResult) rspContent;
                if (null != listener){
                    if (result.bSuccess) {  // ??? 需要保存登录状态吗
                        listener.onSuccess(null);
                    }else{
                        listener.onFailed(ErrCode_Failed, null);
                    }
                }
                break;

            case DCLogoutRsp:
                result = (MsgBeans.CommonResult) rspContent;
                if (null != listener){
                    if (result.bSuccess){
                        listener.onSuccess(null);
                    }else{
                        listener.onFailed(ErrCode_Failed, null);
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
                    listener.onFailed(ErrCode_BuildLink4ConfFailed, null);
                }
                break;
            case DCConfCreated:
                MsgBeans.DCCreateConfResult createConfResult = (MsgBeans.DCCreateConfResult) rspContent;
                curDcConfE164 = createConfResult.confE164;
                if (null != listener){
                    if (createConfResult.bSuccess) {
                        listener.onSuccess(ToDoConverter.fromTransferObj(createConfResult));
                    }else{
                        listener.onFailed(ErrCode_Failed, null);
                    }
                }
                break;
            case DCReleaseConfRsp:
                result = (MsgBeans.CommonResult) rspContent;
                if (null != listener){
                    if (result.bSuccess){
                        listener.onSuccess(null);
                    }else{
                        listener.onFailed(ErrCode_Failed, null);
                    }
                }
                break;
            case DCQuitConfRsp:
                result = (MsgBeans.CommonResult) rspContent;
                if (null != listener){
                    if (result.bSuccess){
                        listener.onSuccess(null);
                    }else{
                        listener.onFailed(ErrCode_Failed, null);
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
                listener.onFailed(ErrCode_Failed, null);
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
                    if (null != listener) listener.onFailed(ErrCode_Failed, null);
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
        IOnBoardOpListener onBoardOpListener;
        MsgBeans.DCBoard board = (MsgBeans.DCBoard) ntfContent;
        for (Object listener : listeners) {
            onBoardOpListener = (IOnBoardOpListener) listener;
            if (Msg.DCCurrentBoardNtf.equals(ntfId)) {
                onBoardOpListener.onBoardCreated(ToDoConverter.fromTransferObj(board));
                onBoardOpListener.onBoardSwitched(board.id);
            } else if (Msg.DCBoardCreatedNtf.equals(ntfId)) {
                onBoardOpListener.onBoardCreated(ToDoConverter.fromTransferObj(board));
            } else if (Msg.DCBoardSwitchedNtf.equals(ntfId)) {
                onBoardOpListener.onBoardSwitched(board.id);
            } else if (Msg.DCBoardDeletedNtf.equals(ntfId)) {
                onBoardOpListener.onBoardDeleted(board.id);
            }
        }


        if (Msg.DCCurrentBoardNtf.equals(ntfId)) {
            /**
             * 此条通知仅在刚入会时上报，我们开始下载当前会议中所有画板的所有已有图元
             * */

            // 获取所有画板
            req(Msg.DCQueryAllBoards, new MsgBeans.DCConfId(board.confE164), new QueryAllBoardsInnerListener() {
                @Override
                public void onSuccess(Object result) {
                    MsgBeans.DCBoard[] dcBoards = (MsgBeans.DCBoard[]) result;
                    PaintOpsBuffer paintOpsBuffer;
                    cachedPaintOps.clear();
                    for (MsgBeans.DCBoard board : dcBoards){
                        paintOpsBuffer = new PaintOpsBuffer();
                        paintOpsBuffer.bSynchronizing = true;
                        cachedPaintOps.put(board.id, paintOpsBuffer);
                        // 下载每个画板已有的图元
                        req(Msg.DCDownload, new MsgBeans.DownloadPara(board.id, board.elementUrl), new IResultListener() {
                            @Override
                            public void onSuccess(Object result) {
                                /* 后续会批量上报当前画板已有的图元，直到收到End消息为止。此处我们开启超时机制防止收不到End消息 */
                                handler.sendEmptyMessageDelayed(MsgID_SynchronizingTimeout, 10*1000);
                            }

                            @Override
                            public void onFailed(int errorCode, Object info) {
                                MsgBeans.DownloadResult downloadResult = (MsgBeans.DownloadResult) info;
                                KLog.p(KLog.ERROR, "download paint element for board %s failed!", downloadResult.boardId);
                                cachedPaintOps.remove(downloadResult.boardId);
                            }

                            @Override
                            public void onTimeout() {
                                KLog.p(KLog.ERROR, "download paint element for board %s timeout!", board.id);
                            }
                        });

                        // TODO
//                        handler.postDelayed(batchOpTimeout, 10000); // 起定时器防止final消息不到。
                    }
                }

                @Override
                public void onFailed(int errorCode, Object info) {
                    KLog.p(KLog.ERROR, "DCQueryAllBoards for conf %s failed!", board.confE164);
                }

                @Override
                public void onTimeout() {
                    KLog.p(KLog.ERROR, "DCQueryAllBoards for conf %s timeout!", board.confE164);
                }
            });
        }

    }


    private static final String SAVE_PATH = "/data/local/tmp/"; // FIXME DEBUG
    private void onRsps(Msg rspId, Object rspContent, IResultListener listener){
        KLog.p("rspContent=%s", rspContent);
        switch (rspId){
            case DCDownloadRsp:
                MsgBeans.DownloadResult result = (MsgBeans.DownloadResult) rspContent;
                if (result.bSuccess){
                    listener.onSuccess(result);
                }else{
                    listener.onFailed(ErrCode_Failed, result);
                }
                break;

            case DCQueryPicUrlRsp:
                MsgBeans.DCQueryPicUrlResult queryPicUrlResult = (MsgBeans.DCQueryPicUrlResult) rspContent;
                if (queryPicUrlResult.bSuccess){
                    listener.onSuccess(queryPicUrlResult);
                }else{
                    listener.onFailed(ErrCode_Failed, queryPicUrlResult);
                }
                break;
        }
    }



//    private Handler handler = new Handler(Looper.getMainLooper());
//    private final Runnable batchOpTimeout = () -> {
//        KLog.p(KLog.ERROR,"wait batch paint ops timeout <<<<<<<<<<<<<<<<<<<<<<<");
//        Set<Object> listeners = getNtfListeners(Msg.DCElementEndNtf);
//        if(!listeners.isEmpty()) {
//            while (!cachedOps.isEmpty()) {
//                OpPaint op = ToDoConverter.fromTransferObj(cachedOps.poll());
//                if (null != op) {
//                    for (Object listener : listeners) {
//                        ((IOnPaintOpListener) listener).onPaintOp(op);
//                    }
//                }
//            }
//        }
//
//        isSynchronizing = false;
//    };

    @SuppressWarnings("ConstantConditions")
    private void onPaintNtfs(Msg ntfId, Object ntfContent, Set<Object> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        switch (ntfId){
            case DCElementBeginNtf:
                // NOTE:此通知并不能准确标记批量图元的起点，我们以下载作为起点。
                break;
            case DCElementEndNtf:
                KLog.p("batch paint ops <<<<<<<<<<<<<<<<<<<<<<<");

                handler.removeMessages(MsgID_SynchronizingTimeout);

                // 批量图元推送结束，我们发给用户
                MsgBeans.DCBoardId boardId = (MsgBeans.DCBoardId) ntfContent;
                PaintOpsBuffer paintOpsBuffer = cachedPaintOps.get(boardId.boardId);
                if (null != paintOpsBuffer){
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
                /* NOTE:插入图片比较特殊，当前只获取到了插入操作的信息，图片本身还需进一步下载。*/

                MsgBeans.DCInertPicOp dcInertPicOp = (MsgBeans.DCInertPicOp) ntfContent;

                // 获取图片下载地址
                req(Msg.DCQueryPicUrl,
                        new MsgBeans.DCQueryPicUrlPara(dcInertPicOp.picId, dcInertPicOp.confE164, dcInertPicOp.boardId, dcInertPicOp.pageId),
                        new IResultListener(){
                            @Override
                            public void onSuccess(Object result) {
                                MsgBeans.DCQueryPicUrlResult queryPicUrlResult = (MsgBeans.DCQueryPicUrlResult) result;
                                // 下载图片
                                req(Msg.DCDownload, new MsgBeans.DownloadPara(queryPicUrlResult.boardId, queryPicUrlResult.picId, SAVE_PATH + queryPicUrlResult.picId + ".jpg", queryPicUrlResult.url),
                                        new IResultListener() {
                                            @Override
                                            public void onSuccess(Object result) {
                                                MsgBeans.DownloadResult downloadResult = (MsgBeans.DownloadResult) result;
                                                OpPaint op = new OpUpdatePic(downloadResult.boardId, downloadResult.picId, BitmapFactory.decodeFile(downloadResult.picSavePath));
                                                for (Object onPaintOpListener : listeners) {
                                                    if (containsNtfListener(onPaintOpListener)) { // 在下载过程中可能listener销毁了删除了，所以需做此判断
                                                        ((IOnPaintOpListener) onPaintOpListener).onPaintOp(op);  // 前面我们插入图片的操作并无实际效果，因为图片是“置空”的，此时图片已下载完成，我们更新之前置空的图片。
                                                    }
                                                }
                                            }

                                            @Override
                                            public void onFailed(int errorCode, Object errorInfo) {
                                                KLog.p(KLog.ERROR, "download pic %s for board %s failed!", queryPicUrlResult.picId, queryPicUrlResult.boardId);
                                            }

                                            @Override
                                            public void onTimeout() {
                                                KLog.p(KLog.ERROR, "download pic %s for board %s timeout!", queryPicUrlResult.picId, queryPicUrlResult.boardId);
                                            }
                                        });
                            }

                            @Override
                            public void onFailed(int errorCode, Object errorInfo) {
                                KLog.p(KLog.ERROR, "query url of pic %s for board %s failed!", dcInertPicOp.picId, dcInertPicOp.boardId);
                            }

                            @Override
                            public void onTimeout() {
                                KLog.p(KLog.ERROR, "query url of pic %s for board %s timeout!", dcInertPicOp.picId, dcInertPicOp.boardId);
                            }
                        });
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
            case DCConfCreated:
                MsgBeans.DCCreateConfResult dcCreateConfResult = (MsgBeans.DCCreateConfResult) ntfContent;
                curDcConfE164 = dcCreateConfResult.confE164;
                CreateConfResult createConfResult = ToDoConverter.fromTransferObj(dcCreateConfResult);
                KLog.p("createConfResult=%s", createConfResult);
                for (Object listener : listeners){
                    ((INotificationListener)listener).onNotification(createConfResult);
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
