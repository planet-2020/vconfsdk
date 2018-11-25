package com.kedacom.vconf.sdk.datacollaborate;

import android.graphics.BitmapFactory;
import android.os.Handler;

import com.kedacom.vconf.sdk.base.IResponseListener;
import com.kedacom.vconf.sdk.base.Msg;
import com.kedacom.vconf.sdk.base.MsgBeans;
import com.kedacom.vconf.sdk.base.RequestAgent;
import com.kedacom.vconf.sdk.base.CommonResultCode;
import com.kedacom.vconf.sdk.base.KLog;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCMember;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpUpdatePic;
import com.kedacom.vconf.sdk.datacollaborate.bean.BoardInfo;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpPaint;
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
    private PriorityQueue<MsgBeans.DCPaintOp> batchOps = new PriorityQueue<>();

    //当前数据协作会议的e164
    private String curDcConfE164;

    private static final Msg[] boardOpNtfs = new Msg[]{
            Msg.DCCurrentBoardNtf,
            Msg.DCBoardCreatedNtf,
            Msg.DCBoardSwitchedNtf,
            Msg.DCBoardDeletedNtf,
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

        processorMap.put(Msg.DCCreateConf, this::onConfOpRsps);
        processorMap.put(Msg.DCReleaseConf, this::onConfOpRsps);
        processorMap.put(Msg.DCQuitConf, this::onConfOpRsps);

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
        req(Msg.DCLogin, new MsgBeans.DCLoginPara(serverIp, port, terminalType.toTransferType()), resultListener);
    }

    /**注销数据协作*/
    public void logout(IResponseListener resultListener){
        req(Msg.DCLogout, null, resultListener);
    }

    private void onSessionRsps(Msg rspId, Object rspContent, IResponseListener listener){
        KLog.p("rspId=%s, rspContent=%s, listener=%s",rspId, rspContent, listener);
        MsgBeans.CommonResult result;
        if (Msg.DCBuildLink4LoginRsp.equals(rspId)){
            result = (MsgBeans.CommonResult) rspContent;
            if (!result.bSuccess
                    && null != listener){
                cancelReq(Msg.DCLogin, listener);  // 后续不会有DCLoginRsp上来，取消该请求以防等待超时。
                listener.onResponse(CommonResultCode.FAILED, null);
            }
        }else if (Msg.DCLoginRsp.equals(rspId)){
            result = (MsgBeans.CommonResult) rspContent;
            if (null != listener){
                if (result.bSuccess) {  // ??? 需要保存登录状态吗
                    listener.onResponse(CommonResultCode.SUCCESS, null);
                }else{
                    listener.onResponse(CommonResultCode.FAILED, null);
                }
            }
        }else if (Msg.DCLogoutRsp.equals(rspId)){
            result = (MsgBeans.CommonResult) rspContent;
            if (null != listener){
                if (result.bSuccess){
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

    private void onConfOpRsps(Msg rspId, Object rspContent, IResponseListener listener){
        MsgBeans.CommonResult result;
        if (Msg.DCBuildLink4ConfRsp.equals(rspId)){
            result = (MsgBeans.CommonResult) rspContent;
            if (!result.bSuccess
                    && null != listener){
                cancelReq(Msg.DCCreateConf, listener);  // 后续不会有DCCreateConfRsp上来，取消该请求以防等待超时。
                listener.onResponse(CommonResultCode.FAILED, null);
            }
        }else if (Msg.DCConfCreated.equals(rspId)){
            MsgBeans.DCCreateConfResult createConfResult = (MsgBeans.DCCreateConfResult) rspContent;
            curDcConfE164 = createConfResult.confE164;
            if (null != listener){
                if (createConfResult.bSuccess) {
                    listener.onResponse(CommonResultCode.SUCCESS, null);
                }else{
                    listener.onResponse(CommonResultCode.FAILED, null);
                }
            }
        }else if (Msg.DCReleaseConfRsp.equals(rspId)){
            result = (MsgBeans.CommonResult) rspContent;
            if (null != listener){
                if (result.bSuccess){
                    listener.onResponse(CommonResultCode.SUCCESS, rspContent);
                }else{
                    listener.onResponse(CommonResultCode.FAILED, rspContent);
                }
            }
        }else if (Msg.DCQuitConfRsp.equals(rspId)){
            result = (MsgBeans.CommonResult) rspContent;
            if (null != listener){
                if (result.bSuccess){
                    listener.onResponse(CommonResultCode.SUCCESS, rspContent);
                }else{
                    listener.onResponse(CommonResultCode.FAILED, rspContent);
                }
            }
        }
    }



    /**添加协作方*/
    public void addOperator(DCMember[] members, IResponseListener resultListener){
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
    public void delOperator(DCMember[] members, IResponseListener resultListener){
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
            if (result.bSuccess){
                listener.onResponse(CommonResultCode.SUCCESS, rspContent);
            }else{
                listener.onResponse(CommonResultCode.FAILED, rspContent);
            }
        }
    }

    private void onOperatorsChangedNtfs(Msg ntfId, Object ntfContent, Set<Object> listeners){

        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
    }


    private static final String SAVE_PATH = "/data/local/tmp/";
    private void onDownloadRsp(Msg rspId, Object rspContent, IResponseListener listener){
        MsgBeans.DownloadResult result = (MsgBeans.DownloadResult) rspContent;
        if (!result.bSuccess){
            KLog.p(KLog.ERROR, "download file failed!");
            return;
        }



        // FIXME just for debug
        if (null != listener){
            // 下载的是图片
            OpPaint op = new OpUpdatePic(result.boardId, result.picId, BitmapFactory.decodeFile(result.picSavePath));
            Set<Object> onPaintOpListeners = ((DownloadListener)listener).onPaintOpListeners; // TODO 能不能自适应activity生命周期？
            KLog.p("download pic finished, onPaintOpListeners=%s", onPaintOpListeners);
            for (Object onPaintOpListener : onPaintOpListeners){
                ((IOnPaintOpListener)onPaintOpListener).onPaintOp(op);  // 前面我们插入图片的操作并无实际效果，因为图片是“置空”的，此时图片已下载完成，我们更新之前置空的图片。
            }

        }else{
            pushCachedOps();
        }
        return;



//        if (!result.bPic){
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
//        Set<Object> onPaintOpListeners = ((DownloadListener)listener).onPaintOpListeners; // TODO 能不能自适应activity生命周期？
//        KLog.p("download pic finished, onPaintOpListeners=%s", onPaintOpListeners);
//        for (Object onPaintOpListener : onPaintOpListeners){
//            ((IOnPaintOpListener)onPaintOpListener).onPaintOp(op);  // 前面我们插入图片的操作并无实际效果，因为图片是“置空”的，此时图片已下载完成，我们更新之前置空的图片。
//        }
    }


    private void onQueryPicUrlRsp(Msg rspId, Object rspContent, IResponseListener listener){
        MsgBeans.DCQueryPicUrlResult result = (MsgBeans.DCQueryPicUrlResult) rspContent;
        if (!result.bSuccess){
            return;
        }

        // 下载图片文件
        req(Msg.DCDownload,
                new MsgBeans.DownloadPara(result.boardId, result.picId, SAVE_PATH+result.picId+".jpg", result.url),
                listener);
    }


    private void onBoardNtfs(Msg ntfId, Object ntfContent, Set<Object> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        IOnBoardOpListener onBoardOpListener;
        MsgBeans.DCBoard board;
        for (Object listener : listeners) {
            onBoardOpListener = (IOnBoardOpListener) listener;
            if (Msg.DCCurrentBoardNtf.equals(ntfId)) {
                board = (MsgBeans.DCBoard) ntfContent;
                onBoardOpListener.onBoardSwitched(board.id);
            } else if (Msg.DCBoardCreatedNtf.equals(ntfId)) {
                board = (MsgBeans.DCBoard) ntfContent;
                onBoardOpListener.onBoardCreated(new BoardInfo(board.id, board.name));
            } else if (Msg.DCBoardSwitchedNtf.equals(ntfId)) {
                board = (MsgBeans.DCBoard) ntfContent;
                onBoardOpListener.onBoardSwitched(board.id);
                // 下载当前画板已有的图元操作。NOTE:下载过程中可能有其他画板比如画板2的操作board2_ops也上报上来，然后切到画板2时又要批量下载已有图元，这时批量下载的操作时序上应该在board2_ops前面，但接收到的时序恰好相反，记得处理这种情形。
                /* NOTE:对于下载下来的图片相关的操作，如插入图片、删除图片等，并不包含图片文件本身。
                要获取图片文件本身，需在后续专门下载。*/
                req(Msg.DCDownload,
                        new MsgBeans.DownloadPara(board.id, board.elementUrl),
                        null);  // TODO DcsSwitch_Ntf可以有多次，而下载只应该一次。在DcsCurrentWhiteBoard_Ntf中做？实测抓消息。
            } else if (Msg.DCBoardDeletedNtf.equals(ntfId)) {
                board = (MsgBeans.DCBoard) ntfContent;
                onBoardOpListener.onBoardDeleted(board.id);
            }
        }
    }

    private Handler handler = new Handler();
    private final Runnable batchOpTimeout = () -> {
        KLog.p(KLog.ERROR,"wait batch paint ops timeout <<<<<<<<<<<<<<<<<<<<<<<");
        Set<Object> listeners = getNtfListeners(Msg.DCElementBeginNtf);
        while (!batchOps.isEmpty()) {
            OpPaint op = ToDoConverter.fromTransferObj(batchOps.poll());
            if (null != op) {
                for (Object listener : listeners) {
                    ((IOnPaintOpListener) listener).onPaintOp(op);
                }
            }
        }

        isRecvingBatchOps = false;
    };
    @SuppressWarnings("ConstantConditions")
    private void onPaintNtfs(Msg ntfId, Object ntfContent, Set<Object> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        switch (ntfId){
            case DCElementBeginNtf:
                if (isRecvingBatchOps) {  // TODO 多个画板切换会有多个DcsElementOperBegin_Ntf，此处应该要分boardId处理；另外在DcsElementOperBegin_Ntf之前就有可能有新的操作过来，或许要在开始download的时候就开启isRecvingBatchOps
                    return;
                }
                KLog.p("batch paint ops >>>>>>>>>>>>>>>>>>>>>>");
                isRecvingBatchOps = true;
                handler.postDelayed(batchOpTimeout, 10000); // 起定时器防止final消息不到。
                break;
            case DCElementEndNtf:
                if (!isRecvingBatchOps) {
                    return;
                }
                KLog.p("batch paint ops <<<<<<<<<<<<<<<<<<<<<<<");
                handler.removeCallbacks(batchOpTimeout);
                while (!batchOps.isEmpty()) {
                    OpPaint opPaint = ToDoConverter.fromTransferObj(batchOps.poll());
                    if (null != opPaint) {
                        for (Object listener : listeners) {
                            ((IOnPaintOpListener) listener).onPaintOp(opPaint); // TODO 需要区分boardId。需要记录每个boardId是否已经下载批量操作对于尚未下载的先缓存
                        }
                    }
                }
                isRecvingBatchOps = false;
                break;
            case DCPicInsertedNtf:
                req(Msg.DCQueryPicUrl,
                        new MsgBeans.DCQueryPicUrlPara("picId", "confE164", "boardId", 1),
                        new DownloadListener(listeners)); // FIXME DownloadListener还能感知原来listener宿主的生命周期吗？要怎么才能做到感知呢？
            default:
                if (ntfContent instanceof MsgBeans.DCPaintOp) {
                    MsgBeans.DCPaintOp dcPaintOp = (MsgBeans.DCPaintOp) ntfContent;
                    if (isRecvingBatchOps) {// todo 根据boarid判断isSynchronized(paintOp.getBoardId())
                        batchOps.offer(dcPaintOp);
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







    public interface IOnPaintOpListener{
        void onPaintOp(OpPaint op);
    }

    public interface IOnBoardOpListener {
        void onBoardCreated(BoardInfo boardInfo);
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
        private Set<Object> onPaintOpListeners;
        DownloadListener(Set<Object> listeners){
            onPaintOpListeners = listeners;
        }

        @Override
        public void onResponse(int i, Object o) {

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
//        eject(Msg.DCApplyOperatorNtf);
        eject(msg);
    }


}
