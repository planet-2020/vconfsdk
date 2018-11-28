package com.kedacom.vconf.sdk.datacollaborate;

import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;

import com.kedacom.vconf.sdk.base.AgentManager;
import com.kedacom.vconf.sdk.base.ILifecycleOwner;
import com.kedacom.vconf.sdk.base.INotificationListener;
import com.kedacom.vconf.sdk.base.IResultListener;
import com.kedacom.vconf.sdk.base.Msg;
import com.kedacom.vconf.sdk.base.MsgBeans;
import com.kedacom.vconf.sdk.base.MsgConst;
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

    // 是否正在同步当前数据协作中的操作
    private boolean isSynchronizing = false;

    /*同步过程中缓存的操作*/
    private PriorityQueue<MsgBeans.DCPaintOp> cachedOps = new PriorityQueue<>();

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
                if (!result.bSuccess
                        && null != listener){
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
                if (!result.bSuccess
                        && null != listener){
                    cancelReq(Msg.DCCreateConf, listener);  // 后续不会有DCCreateConfRsp上来，取消该请求以防等待超时。
                    listener.onFailed(ErrCode_BuildLink4ConfFailed);
                }
                break;
            case DCConfCreated:
                MsgBeans.DCCreateConfResult createConfResult = (MsgBeans.DCCreateConfResult) rspContent;
                curDcConfE164 = createConfResult.confE164;
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
        // 下载当前画板已有的图元操作。NOTE:下载过程中可能有其他画板比如画板2的操作board2_ops也上报上来，然后切到画板2时又要批量下载已有图元，这时批量下载的操作时序上应该在board2_ops前面，但接收到的时序恰好相反，记得处理这种情形。
                /* NOTE:对于下载下来的图片相关的操作，如插入图片、删除图片等，并不包含图片文件本身。
                要获取图片文件本身，需在后续专门下载。*/
        if (Msg.DCCurrentBoardNtf.equals(ntfId)) {
            req(Msg.DCDownload, new MsgBeans.DownloadPara(board.id, board.elementUrl), null);
        }

    }


    private static final String SAVE_PATH = "/data/local/tmp/"; // FIXME DEBUG
    private void onRsps(Msg rspId, Object rspContent, IResultListener listener){
        switch (rspId){
            case DCDownloadRsp:
                MsgBeans.DownloadResult result = (MsgBeans.DownloadResult) rspContent;
                if (!result.bSuccess){
                    KLog.p(KLog.ERROR, "download file failed!");
                    return;
                }
                if (result.bPic) {
                    // 下载的是图片
                    OpPaint op = new OpUpdatePic(result.boardId, result.picId, BitmapFactory.decodeFile(result.picSavePath));
                    Set<Object> onPaintOpListeners = ((DownloadListener) listener).onPaintOpListeners; // TODO 能不能自适应activity生命周期？
                    KLog.p("download pic finished, onPaintOpListeners=%s", onPaintOpListeners);
                    for (Object onPaintOpListener : onPaintOpListeners) {
                        if (containsNtfListener(onPaintOpListener)) { // 在下载过程中可能listener销毁了删除了，所以需做此判断
                            ((IOnPaintOpListener) onPaintOpListener).onPaintOp(op);  // 前面我们插入图片的操作并无实际效果，因为图片是“置空”的，此时图片已下载完成，我们更新之前置空的图片。
                        }
                    }
                }else{
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
                     * */
                    return;
                }
                break;
            case DCQueryPicUrlRsp:
                MsgBeans.DCQueryPicUrlResult queryPicUrlResult = (MsgBeans.DCQueryPicUrlResult) rspContent;
                if (!queryPicUrlResult.bSuccess) {
                    return;
                }
                // 下载图片文件
                req(Msg.DCDownload,
                        new MsgBeans.DownloadPara(queryPicUrlResult.boardId, queryPicUrlResult.picId, SAVE_PATH + queryPicUrlResult.picId + ".jpg", queryPicUrlResult.url),
                        listener);
                break;
        }
    }



    private Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable batchOpTimeout = () -> {
        KLog.p(KLog.ERROR,"wait batch paint ops timeout <<<<<<<<<<<<<<<<<<<<<<<");
        Set<Object> listeners = getNtfListeners(Msg.DCElementEndNtf);
        if(!listeners.isEmpty()) {
            while (!cachedOps.isEmpty()) {
                OpPaint op = ToDoConverter.fromTransferObj(cachedOps.poll());
                if (null != op) {
                    for (Object listener : listeners) {
                        ((IOnPaintOpListener) listener).onPaintOp(op);
                    }
                }
            }
        }

        isSynchronizing = false;
    };
    @SuppressWarnings("ConstantConditions")
    private void onPaintNtfs(Msg ntfId, Object ntfContent, Set<Object> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        switch (ntfId){
            case DCElementBeginNtf:
                if (isSynchronizing) {  // TODO 多个画板切换会有多个DcsElementOperBegin_Ntf，此处应该要分boardId处理；另外在DcsElementOperBegin_Ntf之前就有可能有新的操作过来，或许要在开始download的时候就开启isRecvingBatchOps
                    return;
                }
                KLog.p("batch paint ops >>>>>>>>>>>>>>>>>>>>>>");
                isSynchronizing = true;
                handler.postDelayed(batchOpTimeout, 10000); // 起定时器防止final消息不到。
                break;
            case DCElementEndNtf:
                if (!isSynchronizing) {
                    return;
                }
                KLog.p("batch paint ops <<<<<<<<<<<<<<<<<<<<<<<");
                handler.removeCallbacks(batchOpTimeout);
                while (!cachedOps.isEmpty()) {
                    OpPaint opPaint = ToDoConverter.fromTransferObj(cachedOps.poll());
                    if (null != opPaint) {
                        for (Object listener : listeners) {
                            ((IOnPaintOpListener) listener).onPaintOp(opPaint); // TODO 需要区分boardId。需要记录每个boardId是否已经下载批量操作对于尚未下载的先缓存
                        }
                    }
                }
                isSynchronizing = false;
                break;
            case DCPicInsertedNtf:
                MsgBeans.DCInertPicOp dcInertPicOp = (MsgBeans.DCInertPicOp) ntfContent;
                req(Msg.DCQueryPicUrl,
                        new MsgBeans.DCQueryPicUrlPara(dcInertPicOp.picId, dcInertPicOp.confE164, dcInertPicOp.boardId, dcInertPicOp.pageId),
                        new DownloadListener(listeners));
            default:
                if (ntfContent instanceof MsgBeans.DCPaintOp) {
                    MsgBeans.DCPaintOp dcPaintOp = (MsgBeans.DCPaintOp) ntfContent;

                    if (isSynchronizing) {// todo 根据boarid判断isSynchronized(paintOp.getBoardId())
                        cachedOps.offer(dcPaintOp);
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


    private class DownloadListener implements IResultListener{
        private Set<Object> onPaintOpListeners;
        DownloadListener(Set<Object> listeners){
            onPaintOpListeners = listeners;
        }
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
