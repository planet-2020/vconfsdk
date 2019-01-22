package com.kedacom.vconf.sdk.datacollaborate;

import android.app.Application;
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
import com.kedacom.vconf.sdk.base.bean.dc.DcsGetUserListRsp;
import com.kedacom.vconf.sdk.base.bean.dc.DcsGetWhiteBoardRsp;
import com.kedacom.vconf.sdk.base.bean.dc.DcsNewWhiteBoardRsp;
import com.kedacom.vconf.sdk.base.bean.dc.DcsOperInsertPicNtf;
import com.kedacom.vconf.sdk.base.bean.dc.DcsSwitchRsp;
import com.kedacom.vconf.sdk.base.bean.dc.DcsUploadImageRsp;
import com.kedacom.vconf.sdk.base.bean.dc.EmDcsType;
import com.kedacom.vconf.sdk.base.bean.dc.EmDcsWbMode;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSBoardInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSBoardResult;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSConfUserInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSConnectResult;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSCreateConf;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSCreateConfResult;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSDelWhiteBoardInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSFileInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSFileLoadResult;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSImageUrl;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSNewWhiteBoard;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSOperator;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSRegInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSResult;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSSwitchReq;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSUserInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSUserInfos;
import com.kedacom.vconf.sdk.base.bean.dc.TDcsCacheElementParseResult;
import com.kedacom.vconf.sdk.datacollaborate.bean.DcConfInfo;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCMember;
import com.kedacom.vconf.sdk.datacollaborate.bean.EDcMode;
import com.kedacom.vconf.sdk.datacollaborate.bean.EConfType;
import com.kedacom.vconf.sdk.datacollaborate.bean.EOpType;
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
import java.util.UUID;

import androidx.annotation.Nullable;


public class DataCollaborateManager extends RequestAgent {

    /*同步过程中缓存的操作*/
    private Map<String, PriorityQueue<OpPaint>> cachedPaintOps = new HashMap<>();

    /* 是否正在准备同步。
    标记从入会成功到开始同步会议中已有图元这段时间，对这段时间内到达的图元
    我们也需像同步图元一样先缓存起来而不是直接上报给用户。*/
    private boolean bPreparingSync = false;

    // 错误码
    // 失败
    public static final int ErrCode_Failed = -1;
    // 登录数据协作建链失败
    public static final int ErrCode_BuildLink4LoginFailed = -2;
    // 加入数据协作建链失败
    public static final int ErrCode_BuildLink4ConfFailed = -3;
    // 尚未加入数据协作
    public static final int ErrCode_NoDcConf = -4;

    private String curDcConfE164;
    String getCurDcConfE164(){
        return curDcConfE164;
    }

    // 终端类型
    private EmDcsType terminalType;

    // 会话相关通知
    private static final Msg[] sessionNtfs = new Msg[]{
            Msg.DCConfCreated,
            Msg.DCReleaseConfNtf,
    };

    // 协作权相关通知
    private static final Msg[] operatorNtfs = new Msg[]{
            Msg.DCUserJoinedNtf,
            Msg.DCOperatorAddedNtf,
            Msg.DCOperatorDeletedNtf,
            Msg.DCApplyOperatorNtf,
            Msg.DCApplyOperatorRejectedNtf,
    };

    // 画板相关通知
    private static final Msg[] boardOpNtfs = new Msg[]{
            Msg.DCCurrentBoardNtf,
            Msg.DCBoardCreatedNtf,
            Msg.DCBoardSwitchedNtf,
            Msg.DCBoardDeletedNtf,
            Msg.DCAllBoardDeletedNtf,
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

        processorMap.put(Msg.DCQueryCurBoard, this::onBoardOpRsps);
        processorMap.put(Msg.DCQueryBoard, this::onBoardOpRsps);
        processorMap.put(Msg.DCQueryAllBoards, this::onBoardOpRsps);
        processorMap.put(Msg.DCNewBoard, this::onBoardOpRsps);
        processorMap.put(Msg.DCDelBoard, this::onBoardOpRsps);
        processorMap.put(Msg.DCDelAllBoard, this::onBoardOpRsps);
        processorMap.put(Msg.DCSwitchBoard, this::onBoardOpRsps);

        processorMap.put(Msg.DCAddOperator, this::onChangeOperatorsRsps);
        processorMap.put(Msg.DCDelOperator, this::onChangeOperatorsRsps);
        processorMap.put(Msg.DCRejectApplyOperator, null);
        processorMap.put(Msg.DCApplyOperator, this::onChangeOperatorsRsps);
        processorMap.put(Msg.DCCancelOperator, this::onChangeOperatorsRsps);

        processorMap.put(Msg.DCQueryAllMembers, this::onRsps);

        processorMap.put(Msg.DCQueryPicUrl, this::onRsps);
        processorMap.put(Msg.DCDownload, this::onRsps);
        processorMap.put(Msg.DCQueryPicUploadUrl, this::onRsps);
        processorMap.put(Msg.DCUpload, this::onRsps);

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
        processorMap.put(Msg.DCPicDownloadableNtf, this::onNtfs);
        return processorMap;
    }

    @Override
    protected Map<Msg[], NtfProcessor> ntfsProcessors() {
        Map<Msg[], NtfProcessor> processorMap = new HashMap<>();
        processorMap.put(sessionNtfs, this::onSessionNtfs);
        processorMap.put(operatorNtfs, this::onOperatorNtfs);
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
    /**
     * 获取数据协作管理类实例
     * @param ctx 应用上下文
     * */
    public static DataCollaborateManager getInstance(Application ctx) {
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


    /**登录数据协作
     * @param serverIp 数据协作服务器Ip
     * @param port 数据协作服务器port
     * @param terminalType 己端终端类型
     * @param resultListener 登陆结果监听器。
     *                       成功返回结果null：
     *                       resultListener.onSuccess(null);
     *
     *                       失败返回错误码：
     *                       {@link #ErrCode_BuildLink4LoginFailed }
     *                       {@link #ErrCode_Failed}
     *                       resultListener.onFailed(errorCode);
     **/
    public void login(String serverIp, int port, ETerminalType terminalType, IResultListener resultListener){
        this.terminalType = ToDoConverter.toTransferObj(terminalType);
        req(Msg.DCLogin, resultListener, new TDCSRegInfo(serverIp, port, this.terminalType));
    }

    /**注销数据协作
     * @param resultListener 注销结果监听器。可以为null，若为null表示不关注注销结果。
     *                        成功返回结果null：
     *                        resultListener.onSuccess(null);
     *
     *                        失败返回错误码：
     *                        {@link #ErrCode_Failed}
     *                        resultListener.onFailed(errorCode);
     * */
    public void logout(@Nullable IResultListener resultListener){
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

    private void onSessionNtfs(Msg ntfId, Object ntfContent, Set<Object> listeners){
        switch (ntfId){
            // 入会结果通知
            case DCConfCreated:
                TDCSCreateConfResult dcConfinfo = (TDCSCreateConfResult) ntfContent;
                if (!dcConfinfo.bSuccess){
                    // 入会失败
                    curDcConfE164 = null;
                    KLog.p(KLog.ERROR,"join data collaborate conf{%s, %s} failed", dcConfinfo.achConfName, dcConfinfo.achConfE164);
                    for (Object listener : listeners){
                        ((IOnSessionEventListener)listener).onJoinDcFailed(ErrCode_Failed);
                    }
                    return;
                }

                curDcConfE164 = dcConfinfo.achConfE164;
                DcConfInfo dcConfInfo = ToDoConverter.fromTransferObj(dcConfinfo);
                KLog.p("dcConfInfo: %s", dcConfInfo);
                for (Object listener : listeners){
                    ((IOnSessionEventListener)listener).onJoinDcSuccess(dcConfInfo);
                }

                synchronizeCachedStuff(dcConfinfo);

                break;

            // 数据协作结束通知
            case DCReleaseConfNtf:
                BaseTypeString result = (BaseTypeString) ntfContent;
                if (result.basetype.equals(curDcConfE164)){
                    for (Object listener : listeners){
                        ((IOnSessionEventListener)listener).onDcReleased();
                    }
                    curDcConfE164 = null;
                }
                break;
        }
    }

    // 同步数据协作中已有内容
    void synchronizeCachedStuff(TDCSCreateConfResult dcConfInfo){

        // 入会成功后准备同步会议中已有的图元。
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
                                    new TDCSFileInfo(null, null, board.achTabId, true, 0)
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

                dcConfInfo.achConfE164
        );
    }

    /**创建数据协作
     * @param confE164 会议e164
     * @param confName 会议名称
     * @param dcMode 数据协作模式
     * @param confType 会议类型
     * @param adminE164 主席e164
     * @param members 与会成员
     * @param resultListener 结果监听器。
     *                       成功返回创会信息{@link DcConfInfo}
     *                       resultListener.onSuccess(DcConfInfo);
     *                       失败返回错误码：
     *                       {@link #ErrCode_Failed}
     *                       {@link #ErrCode_BuildLink4ConfFailed}
     *                       resultListener.onFailed(errorCode);*/
    public void createDcConf(String confE164, String confName, EDcMode dcMode, EConfType confType,
                             String adminE164, List<DCMember> members, IResultListener resultListener){
        req(Msg.DCCreateConf, resultListener,
                new TDCSCreateConf(ToDoConverter.toTransferObj(confType),
                        confE164, confName, ToDoConverter.toTransferObj(dcMode),
                        ToDoConverter.toDcUserList(members), adminE164, terminalType));
        cachedPaintOps.clear();
        curDcConfE164 = null;
    }

    /**结束数据协作
     * @param resultListener 结果监听器。
     *                       成功返回null
     *                       resultListener.onSuccess(null);
     *                       失败返回错误码：
     *                       {@link #ErrCode_Failed}
     *                       {@link #ErrCode_NoDcConf}
     *                       resultListener.onFailed(errorCode);*/
    public void releaseDcConf(IResultListener resultListener){
        req(Msg.DCReleaseConf, resultListener, curDcConfE164);
        curDcConfE164 = null;
        cachedPaintOps.clear();
    }

    /**退出数据协作。
     * 注：仅自己退出，协作仍存在，不影响其他人继续
     * @param bQuitConf 是否同时退出会议
     * @param resultListener 结果监听器。
     *                       成功返回null
     *                       resultListener.onSuccess(null);
     *                       失败返回错误码：
     *                       {@link #ErrCode_Failed}
     *                       {@link #ErrCode_NoDcConf}
     *                       resultListener.onFailed(errorCode);*/
    public void quitDcConf(boolean bQuitConf, IResultListener resultListener){
        req(Msg.DCQuitConf, resultListener, curDcConfE164, bQuitConf?0:1);
        curDcConfE164 = null;
        cachedPaintOps.clear();
    }


    private void onConfOpRsps(Msg rspId, Object rspContent, IResultListener listener){
        switch (rspId){
            case DCBuildLink4ConfRsp:
                TDCSConnectResult result = (TDCSConnectResult) rspContent;
                if (!result.bSuccess && null != listener){
                    cancelReq(Msg.DCCreateConf, listener);  // 后续不会有DCConfCreated上来，取消该请求以防等待超时。
                    listener.onFailed(ErrCode_BuildLink4ConfFailed);
                }
                break;
            case DCConfCreated:
                TDCSCreateConfResult createConfResult = (TDCSCreateConfResult) rspContent;
                if (createConfResult.bSuccess){
                    curDcConfE164 = createConfResult.achConfE164;
                }
                if (null != listener){
                    if (createConfResult.bSuccess) {
                        // 查询当前画板以判断当前数据协作中是否有画板，进而决定是否需要同步，用户也可据此决定是否新建默认画板。
                        req(Msg.DCQueryCurBoard, new IResultListener() {
                            @Override
                            public void onSuccess(Object result) {
                                KLog.p("DCQueryCurBoard onSuccess");
                                // 当前会议中有画板则同步
                                // （FIXME 有可能在查询当前画板的过程中监听器被销毁了）
                                DcConfInfo dcConfInfo = ToDoConverter.fromTransferObj(createConfResult);
                                dcConfInfo.setHasBoard(true);
                                listener.onSuccess(dcConfInfo);
                                synchronizeCachedStuff(createConfResult);
                            }

                            @Override
                            public void onFailed(int errorCode) {
                                KLog.p("DCQueryCurBoard onFailed");
                                DcConfInfo dcConfInfo = ToDoConverter.fromTransferObj(createConfResult);
                                dcConfInfo.setHasBoard(false);
                                listener.onSuccess(dcConfInfo);
                            }

                            @Override
                            public void onTimeout() {
                                KLog.p("DCQueryCurBoard onTimeout");
                                DcConfInfo dcConfInfo = ToDoConverter.fromTransferObj(createConfResult);
                                dcConfInfo.setHasBoard(false);
                                listener.onSuccess(dcConfInfo);
                                synchronizeCachedStuff(createConfResult);
                            }
                        }, createConfResult.achConfE164);

                    }else{
                        listener.onFailed(ErrCode_Failed);
                    }
                }
                break;

            case DCReleaseConfRsp:
                TDCSResult releaseRes = (TDCSResult) rspContent;
                if (null != listener){
                    if (releaseRes.bSucces){
                        listener.onSuccess(null);
                    }else{
                        listener.onFailed(ErrCode_Failed);
                    }
                }
                break;
            case DCQuitConfRsp:
                TDCSResult quitRes = (TDCSResult) rspContent;
                if (null != listener){
                    if (quitRes.bSucces){
                        listener.onSuccess(null);
                    }else{
                        listener.onFailed(ErrCode_Failed);
                    }
                }
                break;
        }

    }


    /**（管理方）添加协作方
     * @param memberE164 待添加对象e164
     * @param resultListener 结果监听器。
     *                       成功返回结果null：
     *                       resultListener.onSuccess(null);
     *                       失败返回错误码：
     *                       {@link #ErrCode_Failed}
     *                       resultListener.onFailed(errorCode);*/
    public void addOperator(String memberE164, IResultListener resultListener){
        List<TDCSConfUserInfo> tdcsConfUserInfos = new ArrayList<>(1);
        tdcsConfUserInfos.add(new TDCSConfUserInfo(memberE164, "", terminalType, true, true, false));
        req(Msg.DCAddOperator, resultListener, new TDCSOperator(curDcConfE164, tdcsConfUserInfos));
    }
    /**（管理方）批量添加协作方
     * @param memberE164List 待添加对象e164列表
     * @param resultListener 结果监听器。
     *                       成功返回结果null：
     *                       resultListener.onSuccess(null);
     *                       失败返回错误码：
     *                       {@link #ErrCode_Failed}
     *                       resultListener.onFailed(errorCode);*/
    public void addOperator(List<String> memberE164List, IResultListener resultListener){
        List<TDCSConfUserInfo> tdcsConfUserInfos = new ArrayList<>();
        for (String e164 : memberE164List){
            tdcsConfUserInfos.add(new TDCSConfUserInfo(e164, "", terminalType, true, true, false));
        }
        req(Msg.DCAddOperator, resultListener, new TDCSOperator(curDcConfE164, tdcsConfUserInfos));
    }

    /**（管理方）删除协作方
     * @param memberE164 待删除对象e164
     * @param resultListener 结果监听器。
     *                       成功返回结果null：
     *                       resultListener.onSuccess(null);
     *                       失败返回错误码：
     *                       {@link #ErrCode_Failed}
     *                       resultListener.onFailed(errorCode);*/
    public void delOperator(String memberE164, IResultListener resultListener){
        List<TDCSConfUserInfo> tdcsConfUserInfos = new ArrayList<>(1);
        tdcsConfUserInfos.add(new TDCSConfUserInfo(memberE164, "", terminalType, true, true, false));
        req(Msg.DCDelOperator, resultListener, new TDCSOperator(curDcConfE164, tdcsConfUserInfos));
    }

    /**（管理方）批量删除协作方
     * @param memberE164List 待删除对象e164列表
     * @param resultListener 结果监听器。
     *                       成功返回结果null：
     *                       resultListener.onSuccess(null);
     *                       失败返回错误码：
     *                       {@link #ErrCode_Failed}
     *                       resultListener.onFailed(errorCode);*/
    public void delOperator(List<String> memberE164List, IResultListener resultListener){
        List<TDCSConfUserInfo> tdcsConfUserInfos = new ArrayList<>();
        for (String e164 : memberE164List){
            tdcsConfUserInfos.add(new TDCSConfUserInfo(e164, "", terminalType, true, true, false));
        }
        req(Msg.DCDelOperator, resultListener, new TDCSOperator(curDcConfE164, tdcsConfUserInfos));
    }
    /**
     * （管理方）拒绝协作权申请
     * @param memberE164 被拒绝对象的e164
     * */
    public void rejectApplyOperator(String memberE164){
        List<TDCSConfUserInfo> tdcsConfUserInfos = new ArrayList<>();
        tdcsConfUserInfos.add(new TDCSConfUserInfo(memberE164, "", terminalType, true, false, false));
        req(Msg.DCRejectApplyOperator, null, new TDCSOperator(curDcConfE164, tdcsConfUserInfos));
    }
    /**
     * （管理方）批量拒绝协作权申请
     * @param memberE164List 被拒绝对象的e164列表
     * */
    public void rejectApplyOperator(List<String> memberE164List){
        List<TDCSConfUserInfo> tdcsConfUserInfos = new ArrayList<>();
        for (String memberE164 : memberE164List) {
            tdcsConfUserInfos.add(new TDCSConfUserInfo(memberE164, "", terminalType, true, false, false));
        }
        req(Msg.DCRejectApplyOperator, null, new TDCSOperator(curDcConfE164, tdcsConfUserInfos));
    }


    /**（普通方）申请协作权
     * @param e164 申请者e164
     * @param resultListener 结果监听器。
     *                       成功返回结果null：
     *                       resultListener.onSuccess(null);
     *                       失败返回错误码：
     *                       {@link #ErrCode_Failed}
     *                       resultListener.onFailed(errorCode);
     *
     *                       注意：该监听器返回成功与否仅表示该请求是否已成功被平台受理，
     *                       平台成功受理后会将该请求转给管理方，管理方同意后，才真正表明申请成功。
     *                       管理方同意或拒绝后平台会转发相应通知给申请者，
     *                       申请者需要监听{@link IOnOperatorEventListener#onOperatorAdded(List)}
     *                       和{@link IOnOperatorEventListener#onApplyOperatorRejected()}来判断申请是成功了还是被拒绝了。
     *                       */
    public void applyForOperator(String e164, IResultListener resultListener){
        req(Msg.DCApplyOperator, resultListener, e164);
    }
    /**（协作方）释放协作权
     * @param e164 申请者e164
     * @param resultListener 结果监听器。
     *                       成功返回结果null：
     *                       resultListener.onSuccess(null);
     *                       失败返回错误码：
     *                       {@link #ErrCode_Failed}
     *                       resultListener.onFailed(errorCode);*/
    public void cancelOperator(String e164, IResultListener resultListener){
        req(Msg.DCCancelOperator, resultListener, e164);
    }

    /**
     * 协作方变更（添加/删除/申请/取消）响应处理
     * */
    private void onChangeOperatorsRsps(Msg rspId, Object rspContent, IResultListener listener){
        switch (rspId){
            case DCAddOperatorRsp:
                if (null != listener){
                    if (((TDCSResult) rspContent).bSucces){
                        listener.onSuccess(null);
                    }else{
                        listener.onFailed(ErrCode_Failed);
                    }
                }
                break;
            case DCDelOperatorRsp:
                if (null != listener){
                    if (((TDCSResult) rspContent).bSucces){
                        listener.onSuccess(null);
                    }else{
                        listener.onFailed(ErrCode_Failed);
                    }
                }
                break;
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

    private void onOperatorNtfs(Msg ntfId, Object ntfContent, Set<Object> listeners){
        KLog.p("listener=%s, ntfId=%s, ntfContent=%s", listeners, ntfId, ntfContent);
        switch (ntfId){
            case DCUserJoinedNtf:
                for (Object listener : listeners){
                    ((IOnOperatorEventListener)listener).onUserJoinedNtf(ToDoConverter.fromTransferObj(((TDCSUserInfo)ntfContent).tUserInfo));
                }
                break;
            case DCOperatorAddedNtf:
                for (Object listener : listeners){
                    ((IOnOperatorEventListener)listener).onOperatorAdded(ToDoConverter.fromDcUserList(((TDCSUserInfos)ntfContent).atUserInfoList));
                }
                break;
            case DCOperatorDeletedNtf:
                for (Object listener : listeners){
                    ((IOnOperatorEventListener)listener).onOperatorDeleted(ToDoConverter.fromDcUserList(((TDCSUserInfos)ntfContent).atUserInfoList));
                }
                break;
            case DCApplyOperatorNtf:
                for (Object listener : listeners){
                    ((IOnOperatorEventListener)listener).onApplyOperator(ToDoConverter.fromTransferObj(((TDCSUserInfo)ntfContent).tUserInfo));
                }
                break;
            case DCApplyOperatorRejectedNtf:
                for (Object listener : listeners){
                    ((IOnOperatorEventListener)listener).onApplyOperatorRejected();
                }
                break;
        }
    }


    /**
     * 新建普通画板
     * @param creatorE164 创建者E164
     * @param listener 新建画板结果监听器
     *                  成功返回{@link BoardInfo}：
     *                  resultListener.onSuccess(boardInfo);
     *                  失败返回错误码：
     *                  {@link #ErrCode_Failed}
     *                  resultListener.onFailed(errorCode);
     *
     *                  注意：该请求会触发通知{@link IOnBoardOpListener#onBoardCreated(BoardInfo)}，
     *                  请求者自身也会收到该通知，避免在该通知监听器中和结果监听器中做重复的处理逻辑。
     * */
    public void newBoard(String creatorE164, IResultListener listener){
        KLog.p("creatorE164=%s, listener=%s", creatorE164, listener);
        req(Msg.DCNewBoard, listener, new TDCSNewWhiteBoard(curDcConfE164, new TDCSBoardInfo(UUID.randomUUID().toString(), creatorE164)));
    }

    /**
     * 新建文档模式画板
     * @param boardName 画板名
     * @param pageCount 文档总页数
     * @param curPageIndex 当前文档页
     * @param creatorE164 创建者E164
     * @param listener 新建画板结果监听器
     *                  成功返回{@link BoardInfo}：
     *                  resultListener.onSuccess(boardInfo);
     *                  失败返回错误码：
     *                  {@link #ErrCode_Failed}
     *                  resultListener.onFailed(errorCode);
     *
     *                  注意：该请求会触发通知{@link IOnBoardOpListener#onBoardCreated(BoardInfo)}，
     *                  请求者自身也会收到该通知，避免在该通知监听器中和结果监听器中做重复的处理逻辑。
     * */
    public void newDocBoard(String boardName, int pageCount, int curPageIndex, String creatorE164, IResultListener listener){
        req(Msg.DCNewBoard, listener, new TDCSNewWhiteBoard(curDcConfE164,
                new TDCSBoardInfo(EmDcsWbMode.emWBModeDOC, boardName, pageCount, UUID.randomUUID().toString(), curPageIndex, creatorE164)));
    }

    /**
     * 删除画板
     * @param boardId 待删除画板Id
     * @param listener 删除画板结果监听器
     *                  成功返回boardId：
     *                  resultListener.onSuccess(boardId);
     *                  失败返回错误码：
     *                  {@link #ErrCode_Failed}
     *                  resultListener.onFailed(errorCode);
     *
     *                  注意：该请求会触发通知{@link IOnBoardOpListener#onBoardDeleted(String)}，
     *                  请求者自身也会收到该通知，避免在该通知监听器中和结果监听器中做重复的处理逻辑。
     * */
    public void delBoard(String boardId, IResultListener listener){
        req(Msg.DCDelBoard, listener, curDcConfE164, boardId);
    }


    /**
     * 删除所有画板
     * @param listener 结果监听器
     *                  成功返回会议e164：
     *                  resultListener.onSuccess(confE164);
     *                  失败返回错误码：
     *                  {@link #ErrCode_Failed}
     *                  resultListener.onFailed(errorCode);
     * */
    public void delAllBoard(IResultListener listener){
        req(Msg.DCDelAllBoard, listener, curDcConfE164);
    }

    /**
     * 切换画板
     * @param boardId 目标画板Id
     * @param listener 切换画板结果监听器
     *                  成功返回boardId：
     *                  resultListener.onSuccess(boardId);
     *                  失败返回错误码：
     *                  {@link #ErrCode_Failed}
     *                  resultListener.onFailed(errorCode);
     *
     *                  注意：该请求会触发通知{@link IOnBoardOpListener#onBoardSwitched(String)}，
     *                  请求者自身也会收到该通知，避免在该通知监听器中和结果监听器中做重复的处理逻辑。
     * */
    public void switchBoard(String boardId, IResultListener listener){
        req(Msg.DCSwitchBoard, listener, new TDCSSwitchReq(curDcConfE164, boardId));
    }


    /**
     * 画板操作（创建/删除/切换/查询）响应处理
     * */
    private void onBoardOpRsps(Msg rspId, Object rspContent, IResultListener listener){
        KLog.p("listener==%s, rspId=%s, rspContent=%s", listener, rspId, rspContent);
        switch (rspId){
            case DCQueryCurBoardRsp:
            case DCQueryBoardRsp:
                DcsGetWhiteBoardRsp queryBoardsResult = (DcsGetWhiteBoardRsp) rspContent;
                if (queryBoardsResult.MainParam.bSuccess){
                    if (null != listener) listener.onSuccess(ToDoConverter.fromTransferObj(queryBoardsResult.AssParam));
                }else{
                    KLog.p(KLog.ERROR, "DCQueryBoard failed, errorCode=%s", queryBoardsResult.MainParam.dwErrorCode);
                    if (null != listener) listener.onFailed(ErrCode_Failed);
                }
                break;
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

            case DCNewBoardRsp:
                DcsNewWhiteBoardRsp newWhiteBoardRsp = (DcsNewWhiteBoardRsp) rspContent;
                if (newWhiteBoardRsp.MainParam.bSuccess) {
                    if (null != listener) listener.onSuccess(ToDoConverter.fromTransferObj(newWhiteBoardRsp.AssParam));
                } else {
                    KLog.p(KLog.ERROR, "new board failed, errorCode=%s", newWhiteBoardRsp.MainParam.dwErrorCode);
                    if (null != listener) listener.onFailed(ErrCode_Failed);
                }
                break;
            case DCDelBoardRsp:
                TDCSBoardResult boardResult = (TDCSBoardResult) rspContent;
                if (boardResult.bSuccess){
                    if (null != listener) listener.onSuccess(boardResult.achTabId);
                }else {
                    KLog.p(KLog.ERROR, "del board failed, errorCode=%s", boardResult.dwErrorCode);
                    if (null != listener) listener.onFailed(ErrCode_Failed);
                }
                break;
            case DCDelAllBoardRsp:
                TDCSBoardResult allBoardRes = (TDCSBoardResult) rspContent;
                if (allBoardRes.bSuccess){
                    if (null != listener) listener.onSuccess(allBoardRes.achConfE164);
                }else {
                    KLog.p(KLog.ERROR, "del all board failed, errorCode=%s", allBoardRes.dwErrorCode);
                    if (null != listener) listener.onFailed(ErrCode_Failed);
                }
                break;
            case DCSwitchBoardRsp:
                DcsSwitchRsp switchRsp = (DcsSwitchRsp) rspContent;
                if (switchRsp.MainParam.bSuccess){
                    if (null != listener) listener.onSuccess(switchRsp.AssParam.achTabId);
                }else {
                    KLog.p(KLog.ERROR, "switch board failed, errorCode=%s", switchRsp.MainParam.dwErrorCode);
                    if (null != listener) listener.onFailed(ErrCode_Failed);
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
        } else if (Msg.DCAllBoardDeletedNtf.equals(ntfId)) {
            for (Object listener : listeners) {
                ((IOnBoardOpListener) listener).onAllBoardDeleted();
            }
        }

    }


    private void onRsps(Msg rspId, Object rspContent, IResultListener listener){
        KLog.p("rspContent=%s", rspContent);
        switch (rspId){
            case DCQueryPicUrlRsp:
                DcsDownloadImageRsp queryPicUrlResult = (DcsDownloadImageRsp) rspContent;
                if (queryPicUrlResult.MainParam.bSucces){
                    listener.onSuccess(queryPicUrlResult.AssParam);
                }else{
                    listener.onFailed(ErrCode_Failed);
                }
                break;

            case DCDownloadRsp:
                TDCSFileLoadResult result = (TDCSFileLoadResult) rspContent;
                if (result.bSuccess){
                    listener.onSuccess(result);
                }else{
                    listener.onFailed(ErrCode_Failed);
                }
                break;

            case DCQueryPicUploadUrlRsp:
                DcsUploadImageRsp queryPicUploadUrlResult = (DcsUploadImageRsp) rspContent;
                if (queryPicUploadUrlResult.MainParam.bSucces){
                    listener.onSuccess(queryPicUploadUrlResult.AssParam);
                }else{
                    listener.onFailed(ErrCode_Failed);
                }
                break;
//            case DCUploadRsp:
//                break;
            case DCQueryAllMembersRsp:
                DcsGetUserListRsp userListRsp = (DcsGetUserListRsp) rspContent;
                if (userListRsp.MainParam.bSucces){
                    List<DCMember> dcMembers = new ArrayList<>();
                    for (TDCSConfUserInfo user : userListRsp.AssParam.atUserList){
                        dcMembers.add(ToDoConverter.fromTransferObj(user));
                    }
                    listener.onSuccess(dcMembers);
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
     * 收到绘制操作通知处理
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
                                TDCSImageUrl picUrl = (TDCSImageUrl) result;
                                // 下载图片
                                req(Msg.DCDownload,
                                    new IResultListener() {
                                        @Override
                                        public void onSuccess(Object result) {
//                                            KLog.p("download pic %s for board %s success! save path=%s", picUrl.picId, picUrl.boardId, getPicPath(picUrl.picId));
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

                                    new BaseTypeString(picUrl.achPicUrl),
                                    new TDCSFileInfo(getPicSavePath(picUrl.achWbPicentityId), picUrl.achWbPicentityId, picUrl.achTabId, false, 0)
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


    /**发布绘制操作
     * @param op 绘制操作*/
    public void publishPaintOp(OpPaint op){
        KLog.p("publish op=%s", op);
        Object to = ToDoConverter.toPaintTransferObj(op);
        if (null != to) {
            req(ToDoConverter.opTypeToReqMsg(op.getType()), null,
                    ToDoConverter.toCommonPaintTransferObj(op), to);
        }else{
            req(ToDoConverter.opTypeToReqMsg(op.getType()), null,
                    ToDoConverter.toCommonPaintTransferObj(op));
        }

        // 对于图片插入操作还需上传图片。
        if (EOpType.INSERT_PICTURE == op.getType()){
            req(Msg.DCQueryPicUploadUrl, new IResultListener() {
                @Override
                public void onSuccess(Object result) {
                    TDCSImageUrl picUploadUrl = (TDCSImageUrl) result;
                    req(Msg.DCUpload, new IResultListener() {
                                @Override
                                public void onSuccess(Object result) {
                                    KLog.p(KLog.ERROR, "upload pic %s for board %s success!", ((OpInsertPic) op).getPicId(), op.getBoardId());
                                }

                                @Override
                                public void onFailed(int errorCode) {
                                    KLog.p(KLog.ERROR, "upload pic %s for board %s failed!", ((OpInsertPic) op).getPicId(), op.getBoardId());
                                }

                                @Override
                                public void onTimeout() {
                                    KLog.p(KLog.ERROR, "upload pic %s for board %s timeout!", ((OpInsertPic) op).getPicId(), op.getBoardId());
                                }
                            },
                            new BaseTypeString(picUploadUrl.achPicUrl),
                            new TDCSFileInfo(((OpInsertPic) op).getPicPath(), picUploadUrl.achWbPicentityId,
                                    picUploadUrl.achTabId, false, (int) new File(((OpInsertPic) op).getPicPath()).length()));
                }

                @Override
                public void onFailed(int errorCode) {
                    KLog.p(KLog.ERROR, "query upload url of pic %s for board %s failed!", ((OpInsertPic) op).getPicId(), op.getBoardId());
                }

                @Override
                public void onTimeout() {
                    KLog.p(KLog.ERROR, "query upload url of pic %s for board %s timeout!", ((OpInsertPic) op).getPicId(), op.getBoardId());
                }
            }, new TDCSImageUrl(op.getConfE164(), op.getBoardId(), op.getPageId(), ((OpInsertPic) op).getPicId()));
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
                    ((OpInsertPic)op).setPicPath(opUpdatePic.getPicSavePath()); // 更新图片的所在路径
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
                        new TDCSFileInfo(getPicSavePath(dcPicUrl.achWbPicentityId), dcPicUrl.achWbPicentityId, dcPicUrl.achTabId, false, 0)
                    );
                }else{
                    KLog.p("pic already exists: %s", getPicSavePath(dcPicUrl.achWbPicentityId));
                }
                break;
        }
    }



    /**
     * 注册数据协作会话监听器
     * @param onSessionEventListener 数据协作会话监听器。{@link IOnSessionEventListener}
     * */
    public void addSessionEventListener(IOnSessionEventListener onSessionEventListener){
        subscribe(sessionNtfs, onSessionEventListener);
    }
    /**
     * 注册协作权相关通知监听器
     * @param onOperatorEventListener 协作权相关通知监听器
     * */
    public void addOperatorEventListener(IOnOperatorEventListener onOperatorEventListener){
        subscribe(operatorNtfs, onOperatorEventListener);
    }

    /**
     * 注册画板操作监听器
     * @param onBoardOpListener 画板操作通知监听器。{@link IOnBoardOpListener}
     * */
    public void addBoardOpListener(IOnBoardOpListener onBoardOpListener){
        subscribe(boardOpNtfs, onBoardOpListener);
    }

    /**
     * 注册绘制操作监听器
     * @param onPaintOpListener 绘制操作通知监听器
     * */
    public void addPaintOpListener(IOnPaintOpListener onPaintOpListener){
        subscribe(paintOpNtfs, onPaintOpListener);
    }


    /**
     * 绘制操作通知监听器。
     * 绘制操作包括所有影响画板内容展示的操作，如画线画圆、插图片、擦除、撤销等等。
     * */
    public interface IOnPaintOpListener extends ILifecycleOwner {
        /**
         * @param op 绘制操作*/
        void onPaintOp(OpPaint op);
    }

    /**
     * 画板操作通知监听器
     * */
    public interface IOnBoardOpListener extends ILifecycleOwner{
        /**画板创建通知
         * @param boardInfo 画板信息 {@link BoardInfo}*/
        void onBoardCreated(BoardInfo boardInfo);
        /**
         * 画板删除通知
         * @param boardId 画板Id {@link BoardInfo#id}*/
        void onBoardDeleted(String boardId);
        /**
         * 画板切换通知
         * @param boardId 画板Id {@link BoardInfo#id}*/
        void onBoardSwitched(String boardId);
        /**
         * 所有画板删除通知*/
        void onAllBoardDeleted();
    }

    /**
     * 数据协作会话监听器
     * */
    public interface IOnSessionEventListener extends ILifecycleOwner{
        /**
         * 加入数据协作成功
         * @param dcConfInfo 数据协作会议信息。{@link DcConfInfo}
         * */
        void onJoinDcSuccess(DcConfInfo dcConfInfo);
        /**
         * 加入数据协作失败
         * @param errCode 失败原因 {@link #ErrCode_Failed}
         * */
        void onJoinDcFailed(int errCode);
        /**
         * 数据协作结束
         * */
        void onDcReleased();

    }

    /**
     * 协作权相关通知监听器
     * */
    public interface IOnOperatorEventListener extends ILifecycleOwner{
        /**
         * （所有与会方收到）成员加入数据协作会议通知
         * @param member 成员信息
         * */
        default void onUserJoinedNtf(DCMember member){}
        /**
         * （管理方收到）成员申请协作权通知
         * @param member 申请者信息
         * */
        default void onApplyOperator(DCMember member){}
        /**
         *  （申请方收到）申请协作权被拒通知。{@link #applyForOperator(String, IResultListener)}
         * */
        default void onApplyOperatorRejected(){}
        /**
         * （所有与会方收到）协作方被添加通知。
         * 特别地，申请方申请协作权通过后也会导致自己收到该通知。{@link #applyForOperator(String, IResultListener)}
         * @param members 被添加的协作方信息
         * */
        default void onOperatorAdded(List<DCMember> members){}
        /**
         * （所有与会方收到）协作方被删除通知
         * @param members 被删除的协作方信息
         * */
        default void onOperatorDeleted(List<DCMember> members){}
    }

    private class QueryAllBoardsInnerListener implements IResultListener{
    }

}
