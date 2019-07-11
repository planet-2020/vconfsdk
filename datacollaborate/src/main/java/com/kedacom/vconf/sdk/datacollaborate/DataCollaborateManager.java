/**
 * 数据协作管理模块。
 * （对底层业务逻辑的封装）
 *
 * NOTE: 数据协作目前并非完全独立的模块——
 * 您必须在登录APS成功后才能登录数据协作，并且您必须已经入会才能开始数据协作（目前数据协作是依附于会议的）。
 *
 * */



package com.kedacom.vconf.sdk.datacollaborate;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;

import com.google.common.io.Files;
import com.google.common.net.InetAddresses;
import com.google.common.primitives.Ints;
import com.kedacom.vconf.sdk.base.Caster;
import com.kedacom.vconf.sdk.base.ILifecycleOwner;
import com.kedacom.vconf.sdk.base.IResultListener;
import com.kedacom.vconf.sdk.base.Msg;
import com.kedacom.vconf.sdk.base.KLog;
import com.kedacom.vconf.sdk.base.bean.dc.BaseTypeString;
import com.kedacom.vconf.sdk.base.bean.dc.DcsDownloadImageRsp;
import com.kedacom.vconf.sdk.base.bean.dc.DcsGetAllWhiteBoardRsp;
import com.kedacom.vconf.sdk.base.bean.dc.DcsGetConfAddrRsp;
import com.kedacom.vconf.sdk.base.bean.dc.DcsGetUserListRsp;
import com.kedacom.vconf.sdk.base.bean.dc.DcsGetWhiteBoardRsp;
import com.kedacom.vconf.sdk.base.bean.dc.DcsNewWhiteBoardRsp;
import com.kedacom.vconf.sdk.base.bean.dc.DcsSetConfInfoRsp;
import com.kedacom.vconf.sdk.base.bean.dc.DcsSwitchRsp;
import com.kedacom.vconf.sdk.base.bean.dc.DcsUploadImageRsp;
import com.kedacom.vconf.sdk.base.bean.dc.EmDcsType;
import com.kedacom.vconf.sdk.base.bean.dc.EmDcsWbMode;
import com.kedacom.vconf.sdk.base.bean.dc.EmServerState;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSBoardInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSBoardResult;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSConfInfo;
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
import com.kedacom.vconf.sdk.base.bean.dc.TDCSSrvState;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSSvrAddr;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSSwitchReq;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSUserInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSUserInfos;
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
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;

import androidx.annotation.Nullable;


@SuppressWarnings({"unused", "WeakerAccess"})
public class DataCollaborateManager extends Caster {

    static {
        KLog.p("\n========================================" +
                        "\n======== DataCollaborateManager version=%s, timestamp=%s" +
                        "\n========================================",
                BuildConfig.ARTIFACT_VERSION, BuildConfig.TIMESTAMP);
    }

    private static DataCollaborateManager instance;

    /*同步过程中缓存的操作*/
    private Map<String, PriorityQueue<OpPaint>> cachedPaintOps = new HashMap<>();
    private Map<String, Long> syncTimestamps = new HashMap<>();

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
    // 会议服务器中途断链
    public static final int ErrCode_Disconnect = -4;
    // 协作方数量已达上限
    public static final int ErrCode_Operator_Amount_Reach_Limit = -5;
    // 申请协作权被拒
    public static final int ErrCode_Apply_Operator_Rejected = -6;
    // 没有权限执行该操作
    public static final int ErrCode_No_Permission = -7;
    // 数据协作个数达上限
    public static final int ErrCode_DcAmount_Reach_Limit = -8;
    // 白板数量达上限
    public static final int ErrCode_BoardAmount_Reach_Limit = -9;


    // 当前会议e164号
    private String curDcConfE164;

    // 当前终端类型
    private EmDcsType curTerminalType;

    private Handler assHandler;


    // 通知监听器
    private IOnDcCreatedListener onDcCreatedListener;
    private IOnSynchronizeProgressListener onSynchronizeProgressListener;
    private IOnSessionEventListener onSessionEventListener;
    private IOnOperatorEventListener onOperatorEventListener;
    private IOnBoardOpListener onBoardOpListener;
    private IOnPaintOpListener onPaintOpListener;

    // 会话相关通知
    private static final Msg[] sessionNtfs = new Msg[]{
            Msg.DCBuildLink4ConfRsp,
            Msg.DCConfCreated,
            Msg.DCReleaseConfNtf,
            Msg.DCConfigModified,
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
//            Msg.DCElementEndNtf,
    };


    private static final Msg[] sessionReqs = new Msg[]{
            Msg.DCLogin,
            Msg.DCLogout,
            Msg.DCCreateConf,
            Msg.DCQuitConf,
            Msg.DCReleaseConf,
            Msg.DCModifyConfig,
            Msg.DCQueryConfig,
    };

    private static final Msg[] boardReqs = new Msg[]{
            Msg.DCQueryCurBoard,
            Msg.DCQueryBoard,
            Msg.DCQueryAllBoards,
            Msg.DCNewBoard,
            Msg.DCDelBoard,
            Msg.DCDelAllBoard,
            Msg.DCSwitchBoard,
    };

    private static final Msg[] operatorReqs = new Msg[]{
            Msg.DCAddOperator,
            Msg.DCDelOperator,
            Msg.DCRejectApplyOperator,
            Msg.DCApplyOperator,
            Msg.DCCancelOperator,
            Msg.DCQueryAllMembers,
    };

    private static final Msg[] downUploadReqs = new Msg[]{
            Msg.DCQueryPicUrl,
            Msg.DCDownload,
            Msg.DCQueryPicUploadUrl,
            Msg.DCUpload,
    };

    private static final Msg[] paintReqs = new Msg[]{
            Msg.DCDrawLine,
            Msg.DCDrawOval,
            Msg.DCDrawRect,
            Msg.DCDrawPath,
            Msg.DCUndo,
            Msg.DCRedo,
            Msg.DCClearScreen,
            Msg.DCErase,
            Msg.DCRectErase,
//        Msg.DCZoom,
//        Msg.DCRotateLeft,
//        Msg.DCRotateRight,
//        Msg.DCScrollScreen,
            Msg.DCMatrix,
            Msg.DCInsertPic,
            Msg.DCDeletePic,
            Msg.DCDragPic,
    };

    @Override
    protected Map<Msg, RspProcessor> rspProcessors() {
        Map<Msg, RspProcessor> processorMap = new HashMap<>();
        processorMap.put(Msg.DCQueryAddr, this::onRsp);
        return processorMap;
    }

    @Override
    protected Map<Msg[], RspProcessor> rspsProcessors() {
        Map<Msg[], RspProcessor> processorMap = new HashMap<>();

        processorMap.put(sessionReqs, this::onSessionRsps);
        processorMap.put(boardReqs, this::onBoardOpRsps);
        processorMap.put(operatorReqs, this::onOperatorRsps);
        processorMap.put(downUploadReqs, this::onDownUpLoadRsps);
        processorMap.put(paintReqs, this::onPublishPaintOpRsps);

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

    private DataCollaborateManager(){
        HandlerThread handlerThread = new HandlerThread("DcAss", Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
        assHandler = new Handler(handlerThread.getLooper()){
            @Override
            public void handleMessage(Message msg) {
            }
        };
    }
    /**
     * 获取数据协作管理类实例
     * @param ctx 应用上下文
     * */
    public static DataCollaborateManager getInstance(Application ctx) {
        if (null == context
                && null != ctx){
            context = ctx;
            File dir = new File(ctx.getFilesDir(), "dc_pic");
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

        if (null == instance) {
            instance = new DataCollaborateManager();
        }

        return instance;
    }


    /**登录数据协作
     * @param terminalType 己端终端类型
     * @param resultListener 登陆结果监听器。
     *                       成功返回结果null：
     *                       resultListener.onSuccess(null);
     *
     *                       失败返回错误码：
     *                       {@link #ErrCode_BuildLink4LoginFailed }
     *                       {@link #ErrCode_Failed}
     *                       resultListener.onFailed(errorCode);
     *
     * NOTE: 请务必在登录APS成功后登录数据协作！（登录APS的接口在其他模块）
     **/
    public void login(ETerminalType terminalType, IResultListener resultListener){
        TDCSSrvState srvState = (TDCSSrvState) get(Msg.DCGetState);
        if (null != srvState && EmServerState.emSrvLogin_Succ == srvState.emState){
            KLog.p(KLog.WARN, "already logined!");
            // 已登录状态则直接返回成功（若此状态下直接请求登录下层不会有任何响应 (─.─||| ）
            if (null != resultListener) reportSuccess(null, resultListener);
            return;
        }
        TDCSSvrAddr svrAddr = (TDCSSvrAddr) get(Msg.DCGetServerAddr);
        curTerminalType = ToDoConverter.toTransferObj(terminalType);
        String ip = null;
        try {
            // 将整型ip转为点分十进制
            ip = InetAddresses.fromLittleEndianByteArray(Ints.toByteArray((int) svrAddr.dwIp)).getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        req(Msg.DCLogin, resultListener, new TDCSRegInfo(ip, svrAddr.dwPort, curTerminalType));
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
    }


    /**
     * 查询会议中是否存在数据协作（数据协作目前依附于会议）
     * @param confE164 会议号
     * @param resultListener 结果监听器。
     *                       始终成功，返回结果boolean bExists，指示是否存在：
     *                       resultListener.onSuccess(bExists);
     * */
    public void queryCollaborateExistsOrNot(String confE164, IResultListener resultListener){
        req(Msg.DCQueryAddr, resultListener, confE164);
    }

    /**
     * 当前用户是否正在协作中。
     * */
    public boolean isCollaborating(){
        TDCSSrvState srvState = (TDCSSrvState) get(Msg.DCGetState);
        return null != srvState && srvState.bInConference;
    }


    /**开启数据协作
     * 若数据协作已存在于会议中则直接加入，否则创建数据协作。
     *
     * 可通过{@link #queryCollaborateExistsOrNot(String, IResultListener)}查询会议中是否存在协作。
     *
     * @param confE164 会议e164（数据协作目前依附于会议——只能在会议中开启数据协作）
     * @param confName 会议名称
     * @param dcMode 数据协作模式
     * @param confType 会议类型
     * @param adminE164 主席e164
     * @param members 与会成员
     * @param resultListener 结果监听器。
     *                       成功返回数据协作信息{@link DcConfInfo}
     *                       resultListener.onSuccess(DcConfInfo);
     *                       失败返回错误码：
     *                       {@link #ErrCode_Failed}
     *                       {@link #ErrCode_BuildLink4ConfFailed}
     *                       {@link #ErrCode_DcAmount_Reach_Limit}
     *                       resultListener.onFailed(errorCode);
     * @param synchronizeProgressListener 同步进度监听器（开启协作成功后会同步协作中已有内容）
     * @param sessionEventListener 数据协作会话事件监听器
     * @param operatorEventListener 协作权相关通知监听器
     * @param boardOpListener 画板操作通知监听器
     * @param paintOpListener 绘制操作通知监听器
     * */
    public void startCollaborate(String confE164, String confName, EDcMode dcMode, EConfType confType,String adminE164, List<DCMember> members,
                                 IResultListener resultListener,
                                 IOnSynchronizeProgressListener synchronizeProgressListener,
                                 IOnSessionEventListener sessionEventListener,
                                 IOnOperatorEventListener operatorEventListener,
                                 IOnBoardOpListener boardOpListener,
                                 IOnPaintOpListener paintOpListener ){

        unsubscribeNtfListeners();

        TDCSSrvState srvState = (TDCSSrvState) get(Msg.DCGetState);
        if (null != srvState  && srvState.bInConference) curDcConfE164 = srvState.achConfE164;
        if (null != curDcConfE164) {
            // 正在协作中，先退出协作（不然组件那边会紊乱报24001错误码）
            req(Msg.DCQuitConf, new IResultListener() {
                @Override
                public void onArrive(boolean bSuccess) {
                    curDcConfE164 = null;
                    req(Msg.DCCreateConf, resultListener,
                            new TDCSCreateConf(ToDoConverter.toTransferObj(confType),
                                    confE164, confName, ToDoConverter.toTransferObj(dcMode),
                                    ToDoConverter.toDcUserList(members), adminE164, curTerminalType),
                            synchronizeProgressListener, sessionEventListener, operatorEventListener, boardOpListener, paintOpListener
                    );
                }
            }, curDcConfE164, 1);
        }else{
            req(Msg.DCCreateConf, resultListener,
                    new TDCSCreateConf(ToDoConverter.toTransferObj(confType),
                            confE164, confName, ToDoConverter.toTransferObj(dcMode),
                            ToDoConverter.toDcUserList(members), adminE164, curTerminalType),
                    synchronizeProgressListener, sessionEventListener, operatorEventListener, boardOpListener, paintOpListener // “开启数据协作”并不需要这组监听器作为请求参数，
                    // 我们此处只是利用框架替我们缓存这组监听器，在“开启数据协作”结束后再获取它们以做处理。
            );
        }

    }

    /**结束数据协作
     * @param resultListener 结果监听器。
     *                       成功返回null
     *                       resultListener.onSuccess(null);
     *                       失败返回错误码：
     *                       {@link #ErrCode_Failed}
     *                       resultListener.onFailed(errorCode);*/
    public void finishCollaborate(IResultListener resultListener){
        unsubscribeNtfListeners();
        req(Msg.DCReleaseConf, resultListener, curDcConfE164);
    }

    /**退出数据协作。
     * 注：仅自己退出，协作仍存在，不影响其他人继续
     * @param bQuitConf 是否同时退出会议
     * @param resultListener 结果监听器。
     *                       成功返回null
     *                       resultListener.onSuccess(null);
     *                       失败返回错误码：
     *                       {@link #ErrCode_Failed}
     *                       resultListener.onFailed(errorCode);*/
    public void quitCollaborate(boolean bQuitConf, IResultListener resultListener){
        unsubscribeNtfListeners();
        req(Msg.DCQuitConf, resultListener, curDcConfE164, bQuitConf?0:1);
    }

    /** 修改协作模式
     * @param mode 协作模式
     * @param resultListener 结果监听器。
     *                       成功返回DcConfInfo
     *                       resultListener.onSuccess(DcConfInfo);
     *                       失败返回错误码：
     *                       {@link #ErrCode_Failed}
     *                       resultListener.onFailed(errorCode);*/
    public void modifyCollaborateMode(EDcMode mode, IResultListener resultListener){
        // 获取已有的配置
        req(Msg.DCQueryConfig, new IResultListener() {
            @Override
            public void onSuccess(Object result) {
                DcConfInfo confInfo = (DcConfInfo) result;
                confInfo.setConfMode(mode); // 修改协作模式
                req(Msg.DCModifyConfig, resultListener, ToDoConverter.toTransferObj(confInfo)); // 下设配置
            }

            @Override
            public void onFailed(int errorCode) {
                if (null != resultListener) reportFailed(errorCode, resultListener);
            }

            @Override
            public void onTimeout() {
                if (null != resultListener) reportTimeout(resultListener);
            }
        });
    }



    /**（管理方）添加协作方
     * @param memberE164 待添加对象e164
     * @param resultListener 结果监听器。
     *                       成功返回结果null：
     *                       resultListener.onSuccess(null);
     *                       失败返回错误码：
     *                       {@link #ErrCode_Failed}
     *                       {@link #ErrCode_Operator_Amount_Reach_Limit}
     *                       resultListener.onFailed(errorCode);
     */
    public void addOperator(String memberE164, IResultListener resultListener){
        List<TDCSConfUserInfo> tdcsConfUserInfos = new ArrayList<>(1);
        tdcsConfUserInfos.add(new TDCSConfUserInfo(memberE164, "", curTerminalType, true, true, false));
        req(Msg.DCAddOperator, resultListener, new TDCSOperator(curDcConfE164, tdcsConfUserInfos));
    }
    /**（管理方）批量添加协作方
     * @param memberE164List 待添加对象e164列表
     * @param resultListener 结果监听器。
     *                       成功返回结果null：
     *                       resultListener.onSuccess(null);
     *                       失败返回错误码：
     *                       {@link #ErrCode_Failed}
     *                       {@link #ErrCode_Operator_Amount_Reach_Limit}
     *                       resultListener.onFailed(errorCode);
     **/
    public void addOperator(List<String> memberE164List, IResultListener resultListener){
        List<TDCSConfUserInfo> tdcsConfUserInfos = new ArrayList<>();
        for (String e164 : memberE164List){
            tdcsConfUserInfos.add(new TDCSConfUserInfo(e164, "", curTerminalType, true, true, false));
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
        tdcsConfUserInfos.add(new TDCSConfUserInfo(memberE164, "", curTerminalType, true, true, false));
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
            tdcsConfUserInfos.add(new TDCSConfUserInfo(e164, "", curTerminalType, true, true, false));
        }
        req(Msg.DCDelOperator, resultListener, new TDCSOperator(curDcConfE164, tdcsConfUserInfos));
    }
    /**
     * （管理方）拒绝协作权申请
     * @param memberE164 被拒绝对象的e164
     * */
    public void rejectApplyOperator(String memberE164){
        List<TDCSConfUserInfo> tdcsConfUserInfos = new ArrayList<>();
        tdcsConfUserInfos.add(new TDCSConfUserInfo(memberE164, "", curTerminalType, true, false, false));
        req(Msg.DCRejectApplyOperator, null, new TDCSOperator(curDcConfE164, tdcsConfUserInfos));
    }
    /**
     * （管理方）批量拒绝协作权申请
     * @param memberE164List 被拒绝对象的e164列表
     * */
    public void rejectApplyOperator(List<String> memberE164List){
        List<TDCSConfUserInfo> tdcsConfUserInfos = new ArrayList<>();
        for (String memberE164 : memberE164List) {
            tdcsConfUserInfos.add(new TDCSConfUserInfo(memberE164, "", curTerminalType, true, false, false));
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
     *                       {@link #ErrCode_Apply_Operator_Rejected}
     *                       resultListener.onFailed(errorCode);
     *
     *                       注意：申请协作权需等管理方审批，很可能出现等待超时然后管理方才审批的场景。
     *                       此场景下该监听器会回onTimeout，然后待管理方审批通过后上报通知{@link IOnOperatorEventListener#onOperatorAdded(List)}。
     *                       至于超时后管理方拒绝的情形无需处理。
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

    /**获取所有成员
     * @param resultListener 结果监听器。
     *                       成功返回结果List<{@link DCMember}>：
     *                       resultListener.onSuccess(List<DCMember>);
     *                       失败返回错误码：
     *                       {@link #ErrCode_Failed}
     *                       resultListener.onFailed(errorCode);*/
    public void queryAllMembers(IResultListener resultListener){
        req(Msg.DCQueryAllMembers, resultListener, curDcConfE164);
    }



    /**
     * 新建普通画板
     * @param creatorE164 创建者E164
     * @param listener 新建画板结果监听器
     *                  成功返回{@link BoardInfo}：
     *                  resultListener.onSuccess(boardInfo);
     *                  失败返回错误码：
     *                  {@link #ErrCode_Failed}
     *                  {@link #ErrCode_BoardAmount_Reach_Limit}
     *                  resultListener.onFailed(errorCode);
     * */
    public void newBoard(String creatorE164, IResultListener listener){
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
     * */
    public void switchBoard(String boardId, IResultListener listener){
        req(Msg.DCSwitchBoard, listener, new TDCSSwitchReq(curDcConfE164, boardId));
    }



    /**发布绘制操作
     * @param op 绘制操作
     * @param resultListener 结果监听器
     *                       成功返回平台广播的发布成功的OpPaint，否则返回超时
     * */
    public void publishPaintOp(OpPaint op, IResultListener resultListener){
        Object to = ToDoConverter.toPaintTransferObj(op);
        if (null != to) {
            req(ToDoConverter.opTypeToReqMsg(op.getType()), resultListener,
                    ToDoConverter.toCommonPaintTransferObj(op), to,
                    op.getAuthorE164() /* NOTE: 这个参数并非请求真正需要的（查看Msg中的消息参数定义可发现并没有这个参数），
                    传入的目的是想框架替我们缓存（虽然Msg中定义不需该参数，但框架能接受多余的参数并缓存下来在响应抵达时上报用户），
                    在响应抵达时我们需要使用该参数判断响应的合法性*/
            );
        }else{
            req(ToDoConverter.opTypeToReqMsg(op.getType()), resultListener,
                    ToDoConverter.toCommonPaintTransferObj(op), op.getAuthorE164());
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
                                    OpInsertPic insertPic = (OpInsertPic) op;
                                    assHandler.post(() ->
                                            {
                                                try {
                                                    // 插入图片成功后我们把图片拷贝到本地图片缓存目录，这样下次加入协作时能直接从本地加载图片。
                                                    Files.copy(new File(insertPic.getPicPath()), new File(getPicSavePath(insertPic.getPicId())) );
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                    );
                                }

                                @Override
                                public void onFailed(int errorCode) {
                                    KLog.p(KLog.ERROR, "upload pic %s for board %s failed, errorCode=%s", ((OpInsertPic) op).getPicId(), op.getBoardId(), errorCode);
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
                    KLog.p(KLog.ERROR, "query upload url of pic %s for board %s failed, errorCode=%s", ((OpInsertPic) op).getPicId(), op.getBoardId(), errorCode);
                }

                @Override
                public void onTimeout() {
                    KLog.p(KLog.ERROR, "query upload url of pic %s for board %s timeout!", ((OpInsertPic) op).getPicId(), op.getBoardId());
                }
            }, new TDCSImageUrl(op.getConfE164(), op.getBoardId(), op.getPageId(), ((OpInsertPic) op).getPicId()));
        }
    }




    /**
     * 会话响应处理
     * @param rspId 响应消息Id
     * @param rspContent 响应内容
     * @param listener 结果监听器（为请求时传下的）
     * */
    private boolean onSessionRsps(Msg rspId, Object rspContent, IResultListener listener, Msg reqId, Object[] reqParas){
        switch (rspId){
            case DCBuildLink4LoginRsp:
                TDCSConnectResult result = (TDCSConnectResult) rspContent;
                if (Msg.DCLogin == reqId) {
                    if (!result.bSuccess) { // 链路建立失败
                        cancelReq(Msg.DCLogin, listener);  // 后续不会有DCLoginRsp上来，取消该请求以防等待超时。
                        reportFailed(ErrCode_BuildLink4LoginFailed, listener);
                    }
                }else if (Msg.DCLogout == reqId){
                    if (!result.bSuccess) { // 链路已断开
                        reportSuccess(null, listener);
                    }else{
                        // 链路处于连接状态，该消息不是该请求期望的
                        return false;
                    }
                }else{
                    return false;
                }
                break;

            case DCLoginRsp:
                TDCSResult loginRes = (TDCSResult) rspContent;
                if (loginRes.bSucces) {
                    reportSuccess(null, listener);
                }else{
                    reportFailed(convertErrorCode(loginRes.dwErrorCode), listener);
                }
                break;

            case DCLogoutRsp:
                TDCSResult logoutRes = (TDCSResult) rspContent;
                if (!logoutRes.bSucces){
                    cancelReq(Msg.DCLogout, listener);  // 后续不会有DCBuildLink4LoginRsp上来，取消该请求以防等待超时。
                    reportFailed(convertErrorCode(logoutRes.dwErrorCode), listener);
                }
                break;


            case DCBuildLink4ConfRsp:
                result = (TDCSConnectResult) rspContent;
                if (Msg.DCCreateConf == reqId) {
                    if (!result.bSuccess) { // 开启数据协作失败（链路建立失败）
                        cancelReq(Msg.DCCreateConf, listener);  // 后续不会有DCConfCreated上来，取消该请求以防等待超时。
                        reportFailed(ErrCode_BuildLink4ConfFailed, listener);
                        curDcConfE164 = null;
                    }
                }else if (Msg.DCQuitConf == reqId
                        || Msg.DCReleaseConf == reqId){
                    if (!result.bSuccess) { // 链路已断开，退出/结束协作成功
                        reportSuccess(null, listener);
                        curDcConfE164 = null;
                    }else{ // 链路未断开，该消息不是期望的
                        return false;
                    }
                }else{
                    return false;
                }
                break;
            case DCConfCreated:
                TDCSCreateConfResult createConfResult = (TDCSCreateConfResult) rspContent;
                if (createConfResult.bSuccess) { // 开启数据协作成功
                    curDcConfE164 = createConfResult.achConfE164;
                    reportSuccess(ToDoConverter.fromTransferObj(createConfResult), listener);

                    // 注册通知监听器
                    onSynchronizeProgressListener = (IOnSynchronizeProgressListener) reqParas[1];
                    onSessionEventListener = (IOnSessionEventListener) reqParas[2];
                    onOperatorEventListener = (IOnOperatorEventListener) reqParas[3];
                    onBoardOpListener = (IOnBoardOpListener) reqParas[4];
                    onPaintOpListener = (IOnPaintOpListener) reqParas[5];
                    subscribeNtfListeners();

                    // 同步协作中已有内容
                    synchronizeCachedStuff(createConfResult);

                }else{
                    reportFailed(convertErrorCode(createConfResult.dwErrorCode), listener);
                }
                break;

            case DCReleaseConfRsp:
            case DCReleaseConfNtf:
                break;
            case DCQuitConfRsp:
                TDCSResult quitRes = (TDCSResult) rspContent;
                if (!quitRes.bSucces){
                    cancelReq(Msg.DCQuitConf, listener);
                    reportFailed(convertErrorCode(quitRes.dwErrorCode), listener);
                }
                break;

            case DCQueryConfigRsp:
                TDCSCreateConfResult dcConfig = (TDCSCreateConfResult) rspContent;
                if (dcConfig.bSuccess) {
                    reportSuccess(ToDoConverter.fromTransferObj(dcConfig), listener);
                }else{
                    reportFailed(convertErrorCode(dcConfig.dwErrorCode), listener);
                }
                break;

            case DCModifyConfigRsp:
                DcsSetConfInfoRsp setConfInfoRsp = (DcsSetConfInfoRsp) rspContent;
                if (!setConfInfoRsp.bSuccess) {
                    cancelReq(Msg.DCModifyConfig, listener);
                    reportFailed(convertErrorCode(setConfInfoRsp.dwErrorCode), listener);
                }
                break;

            case DCConfigModified:
                if (((TDCSConfInfo)rspContent).achConfE164.equals(curDcConfE164)){
                    reportSuccess(ToDoConverter.fromTransferObj((TDCSConfInfo)rspContent), listener);
                }else {
                    return false;
                }
                break;

            default:
                return false;
        }

        return true;

    }

    private void onSessionNtfs(Msg ntfId, Object ntfContent, Set<Object> listeners){
        switch (ntfId){
            case DCBuildLink4ConfRsp:
                TDCSConnectResult tdcsConnectResult = (TDCSConnectResult) ntfContent;
                if (!tdcsConnectResult.bSuccess){ // 用户所属的数据协作链路状态异常
                    if (null != onSessionEventListener) onSessionEventListener.onDcFinished(); // 通知用户（对于他来说）数据协作已结束
                    curDcConfE164 = null;
                    unsubscribeNtfListeners();
                }
                break;

            case DCConfCreated:
                TDCSCreateConfResult tdcsCreateConfResult = (TDCSCreateConfResult) ntfContent;
                if (tdcsCreateConfResult.bSuccess && null == curDcConfE164) {
                    curDcConfE164 = tdcsCreateConfResult.achConfE164;
                    if (null != onDcCreatedListener)
                        onDcCreatedListener.onDcCreated(ToDoConverter.fromTransferObj(tdcsCreateConfResult));
                }
                break;

            case DCConfigModified:
                DcConfInfo dcConfInfo = ToDoConverter.fromTransferObj((TDCSConfInfo)ntfContent);
                if (dcConfInfo.getConfE164().equals(curDcConfE164)){
                    if (null != onSessionEventListener) onSessionEventListener.onDCConfParaChanged(dcConfInfo);
                }
                break;

        }

    }


    private String getCachedOpsBoardId(String boardId){
        for (String id : cachedPaintOps.keySet()){
            if (id.equals(boardId)){
                return id;
            }
        }
        return null;
    }

    // 同步数据协作中已有内容
    private void synchronizeCachedStuff(TDCSCreateConfResult dcConfInfo){
        if (null == onBoardOpListener) {
            KLog.p(KLog.ERROR, "null == onBoardOpListener");
            return;
        }

        // 入会成功后准备同步会议中已有的图元。
        bPreparingSync = true;
        cachedPaintOps.clear();

        // 获取所有画板
        req(Msg.DCQueryAllBoards, new QueryAllBoardsInnerListener() {
                    @Override
                    public void onArrive(boolean bSuccess) {
                        /* 获取所有画板结束，准备阶段结束*/
                        bPreparingSync = false;
                    }

                    @Override
                    public void onSuccess(Object result) {
                        if (null == onBoardOpListener) {
                            KLog.p(KLog.WARN, "null == onBoardOpListener");
                            return;
                        }
                        bGotAllBoard = true;
                        List<TDCSBoardInfo> dcBoards = (List<TDCSBoardInfo>) result;
                        // 检查准备阶段缓存的图元所在画板是否仍存在，若不存在则删除之。
                        Iterator it = cachedPaintOps.keySet().iterator();
                        while (it.hasNext()) {
                            boolean bMatched = false;
                            String tmpId = (String) it.next();
                            for (TDCSBoardInfo board : dcBoards) {
                                if (tmpId.equals(board.achTabId)) {
                                    bMatched = true;
                                    break;
                                }
                            }
                            if (!bMatched) {
                                it.remove();
                            }
                        }

                        // 上报用户协作中所有画板
                        for (TDCSBoardInfo board : dcBoards) {
                            onBoardOpListener.onBoardCreated(ToDoConverter.fromTransferObj(board, curDcConfE164));
                        }

                        if (null != curBoardId) { // “当前画板”通知已早于此到达，彼时还无法通知用户“切换画板”，因为彼时尚未上报用户画板已创建，所以此时我们补上通知“切换画板”。
                            for (TDCSBoardInfo board : dcBoards) {
                                if (board.achTabId.equals(curBoardId)) {
                                    // 上报用户切换到当前画板
                                    onBoardOpListener.onBoardSwitched(curBoardId);

                                    // 将当前画板放置在列表首位以优先同步
                                    dcBoards.remove(board);
                                    dcBoards.add(0, board);
                                    break;
                                }
                            }
                            curBoardId = null;
                        }

                        // 同步画板中已有内容
                        synchronizeBoards(dcBoards);
                    }

                    @Override
                    public void onFailed(int errorCode) {
                        KLog.p(KLog.ERROR, "DCQueryAllBoards for conf %s failed, errorCode=%s", dcConfInfo.achConfE164, errorCode);
                    }

                    @Override
                    public void onTimeout() {
                        KLog.p(KLog.ERROR, "DCQueryAllBoards for conf %s timeout!", dcConfInfo.achConfE164);
                    }
                },

                dcConfInfo.achConfE164
        );
    }

    private void synchronizeBoards(List<TDCSBoardInfo> dcBoards){
        if (dcBoards.isEmpty()){
            return;
        }

        // “逐个”画板同步（下层不支持一次性同步，会有问题）
        TDCSBoardInfo board = dcBoards.remove(0);
        if (null != onSynchronizeProgressListener)
            onSynchronizeProgressListener.onProgress(board.achTabId, 0, false);
        req(Msg.DCDownload, new IResultListener() {
                    @Override
                    public void onArrive(boolean bSuccess) {
                        synchronizeBoards(dcBoards); // 同步下一个画板
                    }

                    @Override
                    public void onSuccess(Object result) {

                        if (null != onSynchronizeProgressListener)
                            onSynchronizeProgressListener.onProgress(board.achTabId, 20, false);

                        PriorityQueue<OpPaint> ops = cachedPaintOps.get(board.achTabId);
                        if (null == ops) { // 若不为null则表明准备阶段已有该画板的实时图元到达，缓存队列在那时已创建，此处复用它即可
                            ops = new PriorityQueue<>();
                            cachedPaintOps.put(board.achTabId, ops);
                        }
                        /* 后续会收到画板缓存的图元。
                         * 由于下层的begin-final消息不可靠，我们定时检查当前是否仍在同步图元，若同步结束则上报用户*/
                        String boardId = getCachedOpsBoardId(board.achTabId);
                        Message msg = Message.obtain();
                        msg.what = MsgID_CheckSynchronizing;
                        msg.obj = boardId;
                        handler.sendMessageDelayed(msg, 2000);
                        KLog.p("start synchronizing ops for board %s", boardId);
                        syncTimestamps.put(boardId, System.currentTimeMillis());
                    }

                    @Override
                    public void onFailed(int errorCode) {
                        KLog.p(KLog.ERROR, "download paint element for board %s failed, errorCode=%s", board.achTabId, errorCode);
                        if (null != onSynchronizeProgressListener) onSynchronizeProgressListener.onProgress(board.achTabId, 0, true);
                    }

                    @Override
                    public void onTimeout() {
                        KLog.p(KLog.ERROR, "download paint element for board %s timeout!", board.achTabId);
                        if (null != onSynchronizeProgressListener) onSynchronizeProgressListener.onProgress(board.achTabId, 0, true);
                    }
                },

                new BaseTypeString(board.achElementUrl),
                new TDCSFileInfo(null, null, board.achTabId, true, 0)
        );

    }


    /**
     * 协作方变更（添加/删除/申请/取消）响应处理
     * */
    private boolean onOperatorRsps(Msg rspId, Object rspContent, IResultListener listener, Msg reqId, Object[] reqParas){
        switch (rspId){
            case DCAddOperatorRsp:
                TDCSResult result = (TDCSResult) rspContent;
                if (!result.bSucces){
                    KLog.p(KLog.ERROR, "add operator failed, errorCode=%s", result.dwErrorCode);
                    cancelReq(reqId, listener);
                    reportFailed(convertErrorCode(result.dwErrorCode), listener);
                }
                break;
            case DCOperatorAddedNtf:
                List<TDCSConfUserInfo> userInfos = ((TDCSUserInfos) rspContent).atUserInfoList;
                if (Msg.DCAddOperator == reqId) {
                    TDCSOperator para = (TDCSOperator) reqParas[0];
                    if (para.atOperList.equals(userInfos)) {
                        reportSuccess(null, listener);
                    } else {
                        return false;
                    }
                }else if (Msg.DCApplyOperator == reqId){
                    String e164 = (String) reqParas[0];
                    if (e164.equals(userInfos.get(0).achE164)){
                        reportSuccess(null, listener);
                    }else {
                        return false;
                    }
                }else {
                    return false;
                }
                break;

            case DCDelOperatorRsp:
                result = (TDCSResult) rspContent;
                if (!result.bSucces){
                    KLog.p(KLog.ERROR, "del operator failed, errorCode=%s", result.dwErrorCode);
                    cancelReq(reqId, listener);
                    reportFailed(convertErrorCode(result.dwErrorCode), listener);
                }
                break;
            case DCOperatorDeletedNtf:
                userInfos = ((TDCSUserInfos)rspContent).atUserInfoList;
                TDCSOperator para = (TDCSOperator) reqParas[0];
                if (para.atOperList.equals(userInfos)){
                    reportSuccess(null, listener);
                }else {
                    return false;
                }
                break;

            case DCApplyOperatorRsp:
                result = (TDCSResult) rspContent;
                if (!result.bSucces){
                    KLog.p(KLog.ERROR, "applying operator failed, errorCode=%s", result.dwErrorCode);
                    cancelReq(reqId, listener);
                    reportFailed(convertErrorCode(result.dwErrorCode), listener);
                }
                break;
            case DCApplyOperatorRejectedNtf:
                TDCSUserInfo userInfo = (TDCSUserInfo)rspContent;
                String e164 = (String) reqParas[0];
                if (e164.equals(userInfo.tUserInfo.achE164)){
                    reportFailed(ErrCode_Apply_Operator_Rejected, listener);
                }else {
                    return false;
                }
                break;

            case DCCancelOperatorRsp:
                result = (TDCSResult) rspContent;
                if (result.bSucces){
                    reportSuccess(null, listener);
                }else{
                    reportFailed(convertErrorCode(result.dwErrorCode), listener);
                }
                break;

            case DCQueryAllMembersRsp:
                DcsGetUserListRsp userListRsp = (DcsGetUserListRsp) rspContent;
                if (userListRsp.MainParam.bSucces){
                    List<DCMember> dcMembers = new ArrayList<>();
                    for (TDCSConfUserInfo user : userListRsp.AssParam.atUserList){
                        dcMembers.add(ToDoConverter.fromTransferObj(user));
                    }
                    reportSuccess(dcMembers, listener);
                }else{
                    reportFailed(convertErrorCode(userListRsp.MainParam.dwErrorCode), listener);
                }
                break;

            default:
                return false;
        }

        return true;

    }

    private void onOperatorNtfs(Msg ntfId, Object ntfContent, Set<Object> listeners){
        if (null == onOperatorEventListener){
            KLog.p(KLog.ERROR, "null == onOperatorEventListener");
            return;
        }
        switch (ntfId){
            case DCUserJoinedNtf:
                onOperatorEventListener.onUserJoined(ToDoConverter.fromTransferObj(((TDCSUserInfo)ntfContent).tUserInfo));
                break;
            case DCOperatorAddedNtf:
                onOperatorEventListener.onOperatorAdded(ToDoConverter.fromDcUserList(((TDCSUserInfos)ntfContent).atUserInfoList));
                break;
            case DCOperatorDeletedNtf:
                onOperatorEventListener.onOperatorDeleted(ToDoConverter.fromDcUserList(((TDCSUserInfos)ntfContent).atUserInfoList));
                break;
            case DCApplyOperatorNtf:
                onOperatorEventListener.onApplyOperator(ToDoConverter.fromTransferObj(((TDCSUserInfo)ntfContent).tUserInfo));
                break;
        }
    }



    /**
     *  画板操作（增/删/切/查）响应处理
     * */
    private boolean onBoardOpRsps(Msg rspId, Object rspContent, IResultListener listener, Msg reqId, Object[] reqParas){
        switch (rspId){
            case DCQueryCurBoardRsp:
            case DCQueryBoardRsp:
                DcsGetWhiteBoardRsp queryBoardsResult = (DcsGetWhiteBoardRsp) rspContent;
                if (queryBoardsResult.MainParam.bSuccess){
                    reportSuccess(ToDoConverter.fromTransferObj(queryBoardsResult.AssParam, curDcConfE164), listener);
                }else{
                    KLog.p(KLog.ERROR, "DCQueryBoard failed, errorCode=%s", queryBoardsResult.MainParam.dwErrorCode);
                    reportFailed(convertErrorCode(queryBoardsResult.MainParam.dwErrorCode), listener);
                }
                break;

            case DCQueryAllBoardsRsp:
                DcsGetAllWhiteBoardRsp queryAllBoardsResult = (DcsGetAllWhiteBoardRsp) rspContent;
                if (!queryAllBoardsResult.MainParam.bSucces){
                    KLog.p(KLog.ERROR, "DCQueryAllBoards failed, errorCode=%s", queryAllBoardsResult.MainParam.dwErrorCode);
                    reportFailed(convertErrorCode(queryAllBoardsResult.MainParam.dwErrorCode), listener);
                    return true;
                }

                if (null != listener) {
                    PriorityQueue<TDCSBoardInfo> priorityQueue = new PriorityQueue<>();
                    priorityQueue.addAll(queryAllBoardsResult.AssParam.atBoardInfo); // 将board按annoyId升序排序
                    List<TDCSBoardInfo> tdcsBoardInfos = new ArrayList<>();
                    while (!priorityQueue.isEmpty()){
                        tdcsBoardInfos.add(priorityQueue.poll());
                    }

                    Object result;
                    if (listener instanceof QueryAllBoardsInnerListener) {
                        result = tdcsBoardInfos;
                    } else {
                        List<BoardInfo> boardInfos = new ArrayList<>();
                        for (TDCSBoardInfo tdcsBoardInfo : tdcsBoardInfos) {
                            boardInfos.add(ToDoConverter.fromTransferObj(tdcsBoardInfo, curDcConfE164));
                        }
                        result = boardInfos;
                    }

                    reportSuccess(result, listener);
                }

                break;

            case DCNewBoardRsp:
                DcsNewWhiteBoardRsp newWhiteBoardRsp = (DcsNewWhiteBoardRsp) rspContent;
                if (!newWhiteBoardRsp.MainParam.bSuccess) {
                    cancelReq(reqId, listener); // 后续不会有DCBoardCreatedNtf，取消以防等待超时
                    reportFailed(convertErrorCode(newWhiteBoardRsp.MainParam.dwErrorCode), listener);
                }
                break;
            case DCBoardCreatedNtf:
                TDCSBoardInfo tdcsBoardInfo = (TDCSBoardInfo) rspContent;
                TDCSNewWhiteBoard newWhiteBoard = (TDCSNewWhiteBoard) reqParas[0];
                if (newWhiteBoard.tBoardinfo.achWbCreatorE164.equals(tdcsBoardInfo.achWbCreatorE164)) {
                    reportSuccess(ToDoConverter.fromTransferObj(tdcsBoardInfo, curDcConfE164), listener);
                }else{
                    return false; // 返回false表示未消费该条消息
                }
                break;

            case DCDelBoardRsp:
                TDCSBoardResult boardResult = (TDCSBoardResult) rspContent;
                if (!boardResult.bSuccess){
                    KLog.p(KLog.ERROR, "del board failed, errorCode=%s", boardResult.dwErrorCode);
                    cancelReq(reqId, listener);
                    reportFailed(convertErrorCode(boardResult.dwErrorCode), listener);
                }
                break;
            case DCBoardDeletedNtf:
                TDCSDelWhiteBoardInfo boardInfo = (TDCSDelWhiteBoardInfo) rspContent;
                String boardId = (String) reqParas[1];
                if (boardId.equals(boardInfo.strIndex)){
                    reportSuccess(boardInfo.strIndex, listener);
                }else{
                    return false;
                }
                break;

            case DCDelAllBoardRsp:
                TDCSBoardResult allBoardRes = (TDCSBoardResult) rspContent;
                if (!allBoardRes.bSuccess){
                    KLog.p(KLog.ERROR, "del all board failed, errorCode=%s", allBoardRes.dwErrorCode);
                    cancelReq(reqId, listener);
                    reportFailed(convertErrorCode(allBoardRes.dwErrorCode), listener);
                }
                break;
            case DCAllBoardDeletedNtf:
                TDCSDelWhiteBoardInfo delWhiteBoardInfo = (TDCSDelWhiteBoardInfo) rspContent;
                reportSuccess(delWhiteBoardInfo.strConfE164, listener);
                break;

            case DCSwitchBoardRsp:
                DcsSwitchRsp switchRsp = (DcsSwitchRsp) rspContent;
                if (!switchRsp.MainParam.bSuccess){
                    KLog.p(KLog.ERROR, "switch board failed, errorCode=%s", switchRsp.MainParam.dwErrorCode);
                    cancelReq(reqId, listener);
                    reportFailed(convertErrorCode(switchRsp.MainParam.dwErrorCode), listener);
                }
                break;
            case DCBoardSwitchedNtf:
                TDCSBoardInfo boardInfo1 = (TDCSBoardInfo) rspContent;
                TDCSSwitchReq para = (TDCSSwitchReq) reqParas[0];
                if (para.achTabId.equals(boardInfo1.achTabId)){
                    reportSuccess(boardInfo1.achTabId, listener);
                }else{
                    return false;
                }
                break;

            default:
                return false;
        }

        return true;
    }


    private String curBoardId;
    private boolean bGotAllBoard;
    /**
     * （其他与会者）画板操作通知处理。
     * */
    private void onBoardNtfs(Msg ntfId, Object ntfContent, Set<Object> listeners){
        if (null == onBoardOpListener){
            KLog.p(KLog.ERROR, "null == onBoardOpListener");
            return;
        }
        if (Msg.DCBoardCreatedNtf.equals(ntfId)) {
            onBoardOpListener.onBoardCreated(ToDoConverter.fromTransferObj((TDCSBoardInfo) ntfContent, curDcConfE164));
        } else if (Msg.DCBoardSwitchedNtf.equals(ntfId)) {
            onBoardOpListener.onBoardSwitched(((TDCSBoardInfo) ntfContent).achTabId);
        } else if (Msg.DCBoardDeletedNtf.equals(ntfId)) {
            onBoardOpListener.onBoardDeleted(((TDCSDelWhiteBoardInfo) ntfContent).strIndex);
        } else if (Msg.DCCurrentBoardNtf.equals(ntfId)) { //NOTE: 该通知仅在刚入会时会收到
            curBoardId = ((TDCSBoardInfo) ntfContent).achTabId;
            if (bGotAllBoard){ // 已获取当前会议中所有画板则我们可以上报用户“切换画板”。
                onBoardOpListener.onBoardSwitched(curBoardId);
                bGotAllBoard = false;
            }
        } else if (Msg.DCAllBoardDeletedNtf.equals(ntfId)) {
            onBoardOpListener.onAllBoardDeleted();
        }

    }


    private boolean onDownUpLoadRsps(Msg rspId, Object rspContent, IResultListener listener, Msg reqId, Object[] reqParas){
        switch (rspId){
            case DCQueryPicUrlRsp:
                DcsDownloadImageRsp queryPicUrlResult = (DcsDownloadImageRsp) rspContent;
                TDCSImageUrl para0 = (TDCSImageUrl) reqParas[0];
                if (!para0.achWbPicentityId.equals(queryPicUrlResult.AssParam.achWbPicentityId)) {
                    return false; // 不是我请求的图片的结果。（下层消息不可靠，可能乱序、重复，故需做这样的过滤）
                }
                if (queryPicUrlResult.MainParam.bSucces){
                    reportSuccess(queryPicUrlResult.AssParam, listener);
                }else{
                    reportFailed(convertErrorCode(queryPicUrlResult.MainParam.dwErrorCode), listener);
                }
                break;

            case DCDownloadRsp:
                TDCSFileLoadResult result = (TDCSFileLoadResult) rspContent;
                TDCSFileInfo para1 = (TDCSFileInfo) reqParas[1];
                if (null != para1.achWbPicentityId && !para1.achWbPicentityId.equals(result.achWbPicentityId)) {
                    return false; // 这是下载图片的响应且不是我请求的图片的结果。（下层消息不可靠，可能乱序、重复，故需做这样的过滤）
                }
                if (result.bSuccess){
                    reportSuccess(result, listener);
                }else{
                    reportFailed(ErrCode_Failed, listener);
                }
                break;

            case DCQueryPicUploadUrlRsp:
                DcsUploadImageRsp queryPicUploadUrlResult = (DcsUploadImageRsp) rspContent;
                if (queryPicUploadUrlResult.MainParam.bSucces){
                    reportSuccess(queryPicUploadUrlResult.AssParam, listener);
                }else{
                    reportFailed(convertErrorCode(queryPicUploadUrlResult.MainParam.dwErrorCode), listener);
                }
                break;

            case DCUploadNtf:
                result = (TDCSFileLoadResult) rspContent;
                if (!result.bSuccess){
                    KLog.p(KLog.ERROR, "upload file %s failed!", result.achWbPicentityId);
                    cancelReq(reqId, listener);
                    reportFailed(ErrCode_Failed, listener);
                }
                break;
            case DCPicDownloadableNtf:
                TDCSFileInfo uploadFileInfo = (TDCSFileInfo) reqParas[1];
                TDCSImageUrl downloadableFileInfo = (TDCSImageUrl) rspContent;
                if (uploadFileInfo.achWbPicentityId.equals(downloadableFileInfo.achWbPicentityId)) {
                    reportSuccess(rspContent, listener);
                }else{
                    return false;
                }
                break;

            default:
                return false;
        }

        return true;
    }


    private String getPicSavePath(String picId){
        return PIC_SAVE_DIR +"/"+ picId + ".jpg";
    }



    private final int MsgID_CheckSynchronizing = 10;
    private Handler handler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MsgID_CheckSynchronizing:
                    String boardId = (String) msg.obj;
                    long timestamp = syncTimestamps.get(boardId);
                    if (System.currentTimeMillis()-timestamp > 1000){ // 同步阶段若1s未收到后续绘制操作则认为同步结束
                        syncTimestamps.remove(boardId);
                        PriorityQueue<OpPaint> ops = cachedPaintOps.remove(boardId);
                        if (null == ops){
                            KLog.p(KLog.ERROR, "unexpected MsgID_CheckSynchronizing, no such synchronizing board(%s) exists", boardId);
                            return;
                        }
                        KLog.p("finish synchronizing ops for board %s", boardId);

                        if (null != onSynchronizeProgressListener)
                            onSynchronizeProgressListener.onProgress(boardId, 100, true);

                        if (null != onPaintOpListener){
                            /* 同步结束，上报用户该画板已同步的绘制操作。
                            NOTE：之所以同步结束时才上报而不是边收边上报，是因为同步过程中操作到达时序可能跟操作实际时序不一致，
                            所以需要收齐后排好序再上报给用户才能保证用户接收到的操作时序是正确的，进而正确绘制。
                            比如实际的操作时序是“画线、清屏、画圆”最终效果是一个圆，但同步过来的时序可能是“画圆、清屏、画线”，
                            若不做处理直接上报用户，用户界面展示的效果将是一条线。
                            此种时序错乱的情形只在同步过程中有，实时广播的操作没有这个问题。*/
                            List<OpPaint> toReportOps = new ArrayList<>();
                            while (!ops.isEmpty()) {
                                toReportOps.add(ops.poll()); // 排序
                            }
                            for (OpPaint op : toReportOps) {
                                onPaintOpListener.onPaint(op);
                            }
                        }

                    }else{
                        KLog.p("synchronizing ops for board %s", boardId);
                        handler.sendMessageDelayed(Message.obtain(msg), 500); // 同步正在进行中，稍后再做检查是否已结束
                    }

                    break;
            }
        }
    };



    private boolean onPublishPaintOpRsps(Msg rspId, Object rspContent, IResultListener listener, Msg reqId, Object[] reqParas){
        if (Msg.Timeout == rspId){
            return false;
        }

        OpPaint opPaint = ToDoConverter.fromPaintTransferObj(rspContent);
        String authorE164 = (String) reqParas[reqParas.length-1];
        if (null==opPaint || !opPaint.getAuthorE164().equals(authorE164)){
            return false;
        }

        reportSuccess(opPaint, listener);

        return true;
    }


    private boolean onRsp(Msg rspId, Object rspContent, IResultListener listener, Msg reqId, Object[] reqParas){
        switch (rspId){
            case DCQueryAddrRsp:
                reportSuccess(((DcsGetConfAddrRsp) rspContent).MainParam.bSucces, listener);
                break;

            case Timeout:
                if (Msg.DCQueryAddr == reqId){
                    reportSuccess(false, listener);
                    return true;
                }
                return false;

            default:
                return false;
        }
        return true;
    }


    /**
     * 收到绘制操作通知处理
     * */
    private void onPaintNtfs(Msg ntfId, Object ntfContent, Set<Object> listeners){
        if (null == onPaintOpListener){
            KLog.p(KLog.ERROR, "null == onPaintOpListener");
            return;
        }

        OpPaint opPaint = ToDoConverter.fromPaintTransferObj(ntfContent);
        if (null == opPaint){
            return;
        }

        switch (ntfId){
//            case DCElementBeginNtf:
//                break;
//            case DCElementEndNtf:  // NOTE: 下层“开始——结束”通知不可靠，时序数量均有问题，故弃用。
//                break;

            // 插入图片通知。 NOTE:插入图片比较特殊，通知中只有插入图片操作的基本信息，图片本身可能还需进一步下载
            case DCPicInsertedNtf:
                OpInsertPic opInsertPic = (OpInsertPic) opPaint;
                String confE164 = opInsertPic.getConfE164();
                String boardId = opInsertPic.getBoardId();
                int pageId = opInsertPic.getPageId();
                String picId = opInsertPic.getPicId();

                if (new File(getPicSavePath(picId)).exists()){
                    KLog.p("pic already exists: %s", getPicSavePath(picId));
                    // 图片已本地缓存则不用去服务器下载，直接将图片赋给该插入操作
                    opInsertPic.setPicPath(getPicSavePath(picId));

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
                                            // 图片下载成功，更新“插入图片”操作
                                            TDCSFileLoadResult downRst = (TDCSFileLoadResult) result;
                                            updateInsertPicOp(downRst.achTabid, downRst.achWbPicentityId, downRst.achFilePathName);
                                        }

                                        @Override
                                        public void onFailed(int errorCode) {
                                            KLog.p(KLog.ERROR, "download pic %s for board %s failed, errorCode=%s", picUrl.achWbPicentityId, picUrl.achTabId, errorCode);
                                        }

                                        @Override
                                        public void onTimeout() {
                                            KLog.p(KLog.ERROR, "download pic %s for board %s timeout!", picUrl.achWbPicentityId, picUrl.achTabId);
                                        }
                                    },

                                    new BaseTypeString(picUrl.achPicUrl),
                                    new TDCSFileInfo(getPicSavePath(picUrl.achWbPicentityId), picUrl.achWbPicentityId, picUrl.achTabId, false, 0)
                                );
                            }

                            @Override
                            public void onFailed(int errorCode) {
                                KLog.p(KLog.ERROR, "query url of pic %s for board %s failed, errorCode=%s", picId, boardId, errorCode);
                            }

                            @Override
                            public void onTimeout() {
                                KLog.p(KLog.ERROR, "query url of pic %s for board %s timeout!", picId, boardId);
                            }
                        },

                        new TDCSImageUrl(confE164, boardId, pageId, picId)
                    );

                }

                cacheOrReportPaintOp(opInsertPic);

                break;

            default:

                cacheOrReportPaintOp(opPaint);

                break;
        }

    }


    private void cacheOrReportPaintOp(OpPaint op){
        PriorityQueue<OpPaint> cachedOps = cachedPaintOps.get(op.getBoardId());
        if (null != cachedOps){ // 当前正在同步该画板的图元则缓存图元
            syncTimestamps.put(op.getBoardId(), System.currentTimeMillis()); // 更新时间戳
            for (OpPaint cachedOp : cachedOps){
                if (cachedOp.isDuplicate(op)){
                    KLog.p(KLog.WARN, "duplicated op %s", op);
                    return;
                }
            }
            cachedOps.offer(op);
//                KLog.p("cached op %s", op);
        } else {
            if (bPreparingSync){ // 入会后同步前收到的图元也需缓存下来
                PriorityQueue<OpPaint> ops1 = new PriorityQueue<>();
                ops1.offer(op);
                cachedPaintOps.put(op.getBoardId(), ops1);
                KLog.p("preparingSync, cached op %s", op);
            }else {
//                KLog.p("report op %s", op);
                // 过了同步阶段，直接上报用户图元操作
                if (null != onPaintOpListener){
                    onPaintOpListener.onPaint(op);
                }
            }
        }
    }

    private void updateInsertPicOp(String boardId, String picId, String picPath){
        PriorityQueue<OpPaint> cachedOps = cachedPaintOps.get(boardId);
        if (null != cachedOps){ // 当前正在同步中，插入图片的操作被缓存尚未上报给用户，故我们直接更新“插入图片”的操作
            boolean bUpdated = false;
            for (OpPaint op : cachedOps){
                if (!(op instanceof OpInsertPic)){
                    continue;
                }
                OpInsertPic opInsertPic = (OpInsertPic) op;
                if (opInsertPic.getBoardId().equals(boardId)
                        && opInsertPic.getPicId().equals(picId)){
                    opInsertPic.setPicPath(picPath); // 更新图片的所在路径。
                    bUpdated = true;
                    KLog.p("during sync, updated insertPicOp %s of board %s", opInsertPic, boardId);
                    break;
                }
            }
            if (!bUpdated){
                KLog.p(KLog.ERROR, "during sync, update pic %s of board %s failed", picId, boardId);
            }

        }else{ // 同步已结束则上报用户“更新图片”
            if (null != onPaintOpListener){
                OpUpdatePic opUpdatePic = new OpUpdatePic(boardId, picId, picPath);
                KLog.p("report user opUpdatePic %s", opUpdatePic);
                onPaintOpListener.onPaint(opUpdatePic);  // 前面我们插入图片的操作并无实际效果，因为图片是“置空”的，此时图片已下载完成，通知用户更新图片。
            }

        }
    }



    private void onNtfs(Msg ntfId, Object ntfContent, Set<Object> listeners) {
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
                                TDCSFileLoadResult downRst = (TDCSFileLoadResult) result;
                                updateInsertPicOp(downRst.achTabid, downRst.achWbPicentityId, downRst.achFilePathName);
                            }

                            @Override
                            public void onFailed(int errorCode) {
                                KLog.p(KLog.ERROR,"download pic %s for board %s failed, errorCode=%s",dcPicUrl.achWbPicentityId, dcPicUrl.achTabId, errorCode);
                            }

                            @Override
                            public void onTimeout() {
                                KLog.p(KLog.ERROR,"download pic %s for board %s timeout",dcPicUrl.achWbPicentityId, dcPicUrl.achTabId);
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



    private void subscribeNtfListeners(){
        subscribe(sessionNtfs, onSessionEventListener);
        subscribe(operatorNtfs, onOperatorEventListener);
        subscribe(boardOpNtfs, onBoardOpListener);
        subscribe(paintOpNtfs, onPaintOpListener);
    }

    private void unsubscribeNtfListeners(){
        unsubscribe(sessionNtfs, onSessionEventListener);
        unsubscribe(operatorNtfs, onOperatorEventListener);
        unsubscribe(boardOpNtfs, onBoardOpListener);
        unsubscribe(paintOpNtfs, onPaintOpListener);
        onSessionEventListener = null;
        onOperatorEventListener = null;
        onBoardOpListener = null;
        onPaintOpListener = null;
        onSynchronizeProgressListener = null;
    }

    private int convertErrorCode(int remoteErrorCode){
        switch (remoteErrorCode){
            case 25603:
                return ErrCode_DcAmount_Reach_Limit;
            case 25606:
                return ErrCode_No_Permission;
            case 25607:
                return ErrCode_Operator_Amount_Reach_Limit;
            case 25701:
                return ErrCode_BoardAmount_Reach_Limit;
            default:
                return ErrCode_Failed;
        }
    }


    /**
     * 数据协作会话事件监听器。
     * */
    public interface IOnSessionEventListener extends ILifecycleOwner{

        /**
         * （对己端而言）数据协作已结束。（协作本身可能仍存在也可能已不存在）
         * 数据协作被结束，或者己端被管理员从协作中删除均会触发该回调。
         * */
        void onDcFinished();

        /**
         * 数据协作会议参数设置变更（如协作模式被修改）
         * @param dcConfInfo 数据协作会议信息
         * */
        void onDCConfParaChanged(DcConfInfo dcConfInfo);

    }

    /**
     * 数据协作已创建通知监听器
     * */
    public interface IOnDcCreatedListener extends ILifecycleOwner{
        /**
         * 数据协作已创建
         * @param dcConfInfo 数据协作信息
         * */
        void onDcCreated(DcConfInfo dcConfInfo);
    }
    /**
     * 设置数据协作已创建通知监听器
     * */
    public void setOnDcCreatedListener(IOnDcCreatedListener onDcCreatedListener){
        this.onDcCreatedListener = onDcCreatedListener;
    }


    /**
     * 同步进度监听器
     * */
    public interface IOnSynchronizeProgressListener extends ILifecycleOwner{
        /**
         * 同步进度。
         * @param boardId 画板ID
         * @param percentage 画板中的同步百分比。0-100，0代表0%，100代表100%。
         * @param bFinished 同步是否结束。
         *                  NOTE: 同步结束不代表同步成功。
         *                  正常情况应是(percentage<100 && !bFinished) || (percentage==100 && bFinished)，
         *                  若同步失败则percentage<100 && bFinished
         * */
        void onProgress(String boardId, int percentage, boolean bFinished);
    }

    /**
     * 绘制操作通知监听器。
     * */
    public interface IOnPaintOpListener extends ILifecycleOwner {
        /**绘制通知
         * @param op 绘制操作*/
        void onPaint(OpPaint op);
    }

    /**
     * 画板操作通知监听器。
     * */
    public interface IOnBoardOpListener extends ILifecycleOwner{
        /**画板创建通知
         * @param boardInfo 画板信息 {@link BoardInfo}*/
        void onBoardCreated(BoardInfo boardInfo);
        /**
         * 画板删除通知
         * @param boardId 画板Id */
        void onBoardDeleted(String boardId);
        /**
         * 画板切换通知
         * @param boardId 画板Id */
        void onBoardSwitched(String boardId);
        /**
         * 所有画板删除通知*/
        void onAllBoardDeleted();
    }


    /**
     * 协作权相关通知监听器
     * */
    public interface IOnOperatorEventListener extends ILifecycleOwner{
        /**
         * （所有与会方收到）成员加入数据协作会议通知
         * @param member 成员信息
         * */
        default void onUserJoined(DCMember member){}
        /**
         * （管理方收到）成员申请协作权通知
         * @param member 申请者信息
         * */
        default void onApplyOperator(DCMember member){}
        /**
         * （所有与会方收到）协作方被添加通知。
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

    // FORDEBUG
//    public void setDcServerAddr(){
//        set(Msg.DCSetServerAddr, new TDCSConfAddr("127.0.0.1", "localhost", 12345));
//    }
//
//    public Object getDcServerAddr(){
//        TDCSConfAddr result = (TDCSConfAddr) get(Msg.DCGetServerAddr);
//        KLog.p("result="+result);
//        return result;
//    }
}
