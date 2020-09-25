package com.kedacom.vconf.sdk.datacollaborate;

import android.app.Application;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;

import androidx.annotation.Nullable;

import com.annimon.stream.Stream;
import com.google.common.io.Files;
import com.google.common.net.InetAddresses;
import com.google.common.primitives.Ints;
import com.kedacom.vconf.sdk.amulet.Caster;
import com.kedacom.vconf.sdk.amulet.INtfListener;
import com.kedacom.vconf.sdk.amulet.IResultListener;
import com.kedacom.vconf.sdk.common.type.BaseTypeString;
import com.kedacom.vconf.sdk.datacollaborate.bean.BoardInfo;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCMember;
import com.kedacom.vconf.sdk.datacollaborate.bean.DcConfInfo;
import com.kedacom.vconf.sdk.datacollaborate.bean.EConfType;
import com.kedacom.vconf.sdk.datacollaborate.bean.EDcMode;
import com.kedacom.vconf.sdk.datacollaborate.bean.EOpType;
import com.kedacom.vconf.sdk.datacollaborate.bean.ETerminalType;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpInsertPic;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpPaint;
import com.kedacom.vconf.sdk.datacollaborate.bean.OpUpdatePic;
import com.kedacom.vconf.sdk.datacollaborate.bean.transfer.DcsDownloadImageRsp;
import com.kedacom.vconf.sdk.datacollaborate.bean.transfer.DcsGetAllWhiteBoardRsp;
import com.kedacom.vconf.sdk.datacollaborate.bean.transfer.DcsGetConfAddrRsp;
import com.kedacom.vconf.sdk.datacollaborate.bean.transfer.DcsGetUserListRsp;
import com.kedacom.vconf.sdk.datacollaborate.bean.transfer.DcsGetWhiteBoardRsp;
import com.kedacom.vconf.sdk.datacollaborate.bean.transfer.DcsNewWhiteBoardRsp;
import com.kedacom.vconf.sdk.datacollaborate.bean.transfer.DcsSetConfInfoRsp;
import com.kedacom.vconf.sdk.datacollaborate.bean.transfer.DcsSwitchRsp;
import com.kedacom.vconf.sdk.datacollaborate.bean.transfer.DcsUploadImageRsp;
import com.kedacom.vconf.sdk.datacollaborate.bean.transfer.EmDcsType;
import com.kedacom.vconf.sdk.datacollaborate.bean.transfer.EmServerState;
import com.kedacom.vconf.sdk.datacollaborate.bean.transfer.TDCSBoardInfo;
import com.kedacom.vconf.sdk.datacollaborate.bean.transfer.TDCSBoardResult;
import com.kedacom.vconf.sdk.datacollaborate.bean.transfer.TDCSConfInfo;
import com.kedacom.vconf.sdk.datacollaborate.bean.transfer.TDCSConfUserInfo;
import com.kedacom.vconf.sdk.datacollaborate.bean.transfer.TDCSConnectResult;
import com.kedacom.vconf.sdk.datacollaborate.bean.transfer.TDCSCreateConf;
import com.kedacom.vconf.sdk.datacollaborate.bean.transfer.TDCSCreateConfResult;
import com.kedacom.vconf.sdk.datacollaborate.bean.transfer.TDCSDelWhiteBoardInfo;
import com.kedacom.vconf.sdk.datacollaborate.bean.transfer.TDCSFileInfo;
import com.kedacom.vconf.sdk.datacollaborate.bean.transfer.TDCSFileLoadResult;
import com.kedacom.vconf.sdk.datacollaborate.bean.transfer.TDCSImageUrl;
import com.kedacom.vconf.sdk.datacollaborate.bean.transfer.TDCSNewWhiteBoard;
import com.kedacom.vconf.sdk.datacollaborate.bean.transfer.TDCSOperator;
import com.kedacom.vconf.sdk.datacollaborate.bean.transfer.TDCSRegInfo;
import com.kedacom.vconf.sdk.datacollaborate.bean.transfer.TDCSResult;
import com.kedacom.vconf.sdk.datacollaborate.bean.transfer.TDCSSrvState;
import com.kedacom.vconf.sdk.datacollaborate.bean.transfer.TDCSSvrAddr;
import com.kedacom.vconf.sdk.datacollaborate.bean.transfer.TDCSSwitchReq;
import com.kedacom.vconf.sdk.datacollaborate.bean.transfer.TDCSUserInfo;
import com.kedacom.vconf.sdk.datacollaborate.bean.transfer.TDCSUserInfos;
import com.kedacom.vconf.sdk.utils.log.KLog;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 数据协作管理模块。
 * （对底层业务逻辑的封装）
 *
 * NOTE: 数据协作目前并非完全独立的模块——
 * 您必须在登录APS成功后才能登录数据协作，并且您必须已经入会才能开始数据协作（目前数据协作是依附于会议的）。
 *
 * */

@SuppressWarnings({"unused", "WeakerAccess", "unchecked"})
public final class DataCollaborateManager extends Caster<Msg> {

    private static DataCollaborateManager instance;

    // 当前会议e164号
    private String curDcConfE164;

    // 当前终端类型
    private EmDcsType curTerminalType;

    private Handler assHandler;

    private static String PIC_SAVE_DIR;
    private Application context;

    private DataCollaborateManager(Application ctx){
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
        if (null == instance) {
            instance = new DataCollaborateManager(ctx);
        }

        return instance;
    }


    /**登录数据协作
     * @param terminalType 己端终端类型
     * @param resultListener 登陆结果监听器。
     *                       成功： resultListener.onSuccess(null);
     *
     *                       失败： resultListener.onFailed(int errorCode);
     *                              {@link DcErrorCode#BuildLink4LoginFailed}
     *                              {@link DcErrorCode#Failed}
     *
     * NOTE: 请务必在登录APS成功后登录数据协作！（登录APS的接口在其他模块）
     **/
    public void login(ETerminalType terminalType, IResultListener resultListener){
        req(Msg.GetState, new SessionProcessor<Msg>() {
            @Override
            public void onReqSent(IResultListener resultListener, Msg req, Object[] reqParas, Object output) {
                TDCSSrvState srvState = (TDCSSrvState) output;
                if (null != srvState && EmServerState.emSrvLogin_Succ == srvState.emState){
                    KLog.p(KLog.WARN, "already logined!");
                    // 已登录状态则直接返回成功（若此状态下直接请求登录下层不会有任何响应 (─.─||| ）
                    reportSuccess(null, resultListener);
                    return;
                }

                req(Msg.GetServerAddr, new SessionProcessor<Msg>() {
                    @Override
                    public void onReqSent(IResultListener resultListener, Msg req, Object[] reqParas, Object output) {
                        TDCSSvrAddr svrAddr = (TDCSSvrAddr) output;
                        if (null == svrAddr){
                            KLog.p(KLog.ERROR, "can not fetch DCS server address, have you logined APS yet? ");
                            reportFailed(DcErrorCode.GetServerAddrFailed, resultListener);
                            return;
                        }

                        curTerminalType = ToDoConverter.toTransferObj(terminalType);
                        String ip = null;
                        try {
                            // 将整型ip转为点分十进制
                            ip = InetAddresses.fromLittleEndianByteArray(Ints.toByteArray((int) svrAddr.dwIp)).getHostAddress();
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }

                        req(Msg.Login, new SessionProcessor<Msg>() {
                            @Override
                            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                                switch (rsp){
                                    case LoginLinkStateChanged:
                                        TDCSConnectResult result = (TDCSConnectResult) rspContent;
                                        if (!result.bSuccess) { // 链路建立失败
                                            cancelReq(Msg.Login, resultListener);  // 后续不会有DCLoginRsp上来，取消该请求以防等待超时。
                                            reportFailed(DcErrorCode.BuildLink4LoginFailed, resultListener);
                                        }
                                        break;
                                    case LoginRsp:
                                        TDCSResult loginRes = (TDCSResult) rspContent;
                                        if (loginRes.bSuccess) {
                                            reportSuccess(null, resultListener);
                                        }else{
                                            reportFailed(DcErrorCode.fromTransfer(loginRes.dwErrorCode), resultListener);
                                        }
                                        break;
                                }
                            }
                        }, resultListener, new TDCSRegInfo(ip, svrAddr.dwPort, curTerminalType));
                    }
                }, resultListener);
            }
        }, resultListener);

    }

    /**注销数据协作
     * @param resultListener 注销结果监听器。可以为null，若为null表示不关注注销结果（所有的结果监听器均可为null表示不关注请求结果）。
     *                        成功：null;
     *                        失败：{@link DcErrorCode#Failed}
     * */
    public void logout(@Nullable IResultListener resultListener){
        req(Msg.Logout, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                switch (rsp){
                    case LogoutRsp:
                        TDCSResult logoutRes = (TDCSResult) rspContent;
                        if (!logoutRes.bSuccess){
                            cancelReq(Msg.Logout, resultListener);  // 后续不会有DCBuildLink4LoginRsp上来，取消该请求以防等待超时。
                            reportFailed(DcErrorCode.fromTransfer(logoutRes.dwErrorCode), resultListener);
                        }
                        break;

                    case LoginLinkStateChanged:
                        TDCSConnectResult result = (TDCSConnectResult) rspContent;
                        if (!result.bSuccess) { // 链路已断开
                            reportSuccess(null, resultListener);
                        }else{
                            // 链路处于连接状态，该消息不是该请求期望的
                            isConsumed[0] = false;
                        }
                        break;
                }
            }
        }, resultListener);
    }


    /**
     * 查询会议中是否存在数据协作（数据协作目前依附于会议）
     * @param confE164 会议号
     * @param resultListener 结果监听器。
     *                       始终成功：boolean bExists，true表示存在。
     * */
    public void queryCollaborateExistsOrNot(String confE164, IResultListener resultListener){
        req(Msg.QueryAddr, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                reportSuccess(((DcsGetConfAddrRsp) rspContent).MainParam.bSuccess, resultListener);
            }

            @Override
            public void onTimeout(IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                reportSuccess(false, resultListener);
                isConsumed[0] = true;
            }
        }, resultListener, confE164);
    }

    /**
     * 当前用户是否正在协作中。
     * */
    public void isCollaborating(IResultListener resultListener){
        req(Msg.GetState, new SessionProcessor<Msg>() {
            @Override
            public void onReqSent(IResultListener resultListener, Msg req, Object[] reqParas, Object output) {
                TDCSSrvState srvState = (TDCSSrvState) output;
                reportSuccess(null != srvState && srvState.bInConference, resultListener);
            }
        }, resultListener);
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
     *                       成功: {@link DcConfInfo};
     *                       失败：{@link DcErrorCode#Failed}
     *                             {@link DcErrorCode#BuildLink4ConfFailed}
     *                             {@link DcErrorCode#DcAmountReachLimit}
     *                       NOTE:开启协作成功后SDK内部会去同步协作内容，并通过synchronizeProgressListener上报用户同步进度。
     *                       用户应该在同步结束后才去执行添加画板操作，否则可能导致新添加的画板被纳入同步的范畴（取决于平台下发的当前协作中已存在的画板列表是否包含了该新添加的画板），
     *                       进而导致用户有画板重复——一个是自己刚新建的，一个是SDK同步过程中推的。
     * */
    public void startCollaborate(String confE164, String confName, EDcMode dcMode, EConfType confType, String adminE164, List<DCMember> members, IResultListener resultListener){

        clearSession();

        curDcConfE164 = null;
        req(Msg.StartCollaborate, new SessionProcessor<Msg>() {
                    @Override
                    public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                        switch (rsp){
                            case LinkStateChanged:
                                if (!((TDCSConnectResult) rspContent).bSuccess) { // 开启数据协作失败（链路建立失败）
                                    cancelReq(Msg.StartCollaborate, resultListener);  // 后续不会有DCConfCreated上来，取消该请求以防等待超时。
                                    reportFailed(DcErrorCode.BuildLink4ConfFailed, resultListener);
                                    curDcConfE164 = null;
                                }
                                break;
                            case CollaborateStarted:
                                TDCSCreateConfResult createConfResult = (TDCSCreateConfResult) rspContent;
                                if (createConfResult.bSuccess) { // 开启数据协作成功
                                    curDcConfE164 = createConfResult.achConfE164;
                                    reportSuccess(ToDoConverter.fromTransferObj(createConfResult), resultListener);

                                    //准备同步协作内容
                                    //NOTE: 此时若用户newboard会导致该board在同步的过程中重复上报给用户（newboard的result listener中已经上报了），
                                    //规范用户的行为，不要在同步过程中newboard，用户可通过IOnSynchronizeProgressListener监听同步进度。
                                    handler.postDelayed(() -> {
                                        // 同步协作中已有内容
                                        synchronizeCachedStuff(createConfResult);

                                    }, 500);

                                }else{
                                    reportFailed(DcErrorCode.fromTransfer(createConfResult.dwErrorCode), resultListener);
                                }
                                break;
                        }
                    }

                    @Override
                    public void onTimeout(IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                        clearSession();
                    }
                }, resultListener,
                new TDCSCreateConf(ToDoConverter.toTransferObj(confType),
                        confE164, confName, ToDoConverter.toTransferObj(dcMode),
                        ToDoConverter.toDcUserList(members), adminE164, curTerminalType)
        );
    }

    /**结束数据协作
     * @param resultListener 结果监听器。
     *                       成功：null
     *                       失败：{@link DcErrorCode#Failed}
     **/
    public void finishCollaborate(IResultListener resultListener){
        clearSession();
        req(Msg.FinishCollaborate, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                switch (rsp){
                    case FinishCollaborateRsp:
                        break;
                    case CollaborateFinished:
                        break;
                    case LinkStateChanged:
                        if (!((TDCSConnectResult) rspContent).bSuccess) { // 链路已断开，结束协作成功
                            reportSuccess(null, resultListener);
                            curDcConfE164 = null;
                        }else{ // 链路未断开，该消息不是期望的
                            isConsumed[0] = false;
                        }
                        break;
                }
            }
        }, resultListener, curDcConfE164);
    }

    /**退出数据协作。
     * 注：仅自己退出，协作仍存在，不影响其他人继续
     * @param bQuitConf 是否同时退出会议
     * @param resultListener 结果监听器。
     *                       成功：null
     *                       失败：{@link DcErrorCode#Failed}
     **/
    public void quitCollaborate(boolean bQuitConf, IResultListener resultListener){
        clearSession();
        req(Msg.QuitCollaborate, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                switch (rsp){
                    case QuitCollaborateRsp:
                        TDCSResult quitRes = (TDCSResult) rspContent;
                        if (!quitRes.bSuccess){
                            cancelReq(Msg.QuitCollaborate, resultListener);
                            reportFailed(DcErrorCode.fromTransfer(quitRes.dwErrorCode), resultListener);
                        }
                        break;
                    case LinkStateChanged:
                        if (!((TDCSConnectResult) rspContent).bSuccess) { // 链路已断开，退出协作成功
                            reportSuccess(null, resultListener);
                            curDcConfE164 = null;
                        }else{ // 链路未断开，该消息不是期望的
                            isConsumed[0] = false;
                        }
                        break;
                }
            }
        }, resultListener, curDcConfE164, bQuitConf ? 0 : 1);
    }

    /**
     * 查询协作会议信息
     * @param resultListener 结果监听器。
     *                       成功：{@link DcConfInfo}
     *                       失败：{@link DcErrorCode#Failed}
     * */
    public void queryDcConfInfo(IResultListener resultListener){
        req(Msg.QueryConfig, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                TDCSCreateConfResult dcConfig = (TDCSCreateConfResult) rspContent;
                if (dcConfig.bSuccess) {
                    reportSuccess(ToDoConverter.fromTransferObj(dcConfig), resultListener);
                }else{
                    reportFailed(DcErrorCode.fromTransfer(dcConfig.dwErrorCode), resultListener);
                }
            }
        }, resultListener);
    }

    /** 修改协作模式
     * @param mode 协作模式
     * @param resultListener 结果监听器。
     *                       成功：DcConfInfo
     *                       失败：{@link DcErrorCode#Failed}
     **/
    public void modifyCollaborateMode(EDcMode mode, IResultListener resultListener){
        String confE164 = curDcConfE164;
        queryDcConfInfo(new IResultListener() {
            @Override
            public void onSuccess(Object result) {
                DcConfInfo confInfo = (DcConfInfo) result;
                confInfo.setConfMode(mode); // 修改协作模式
                req(Msg.ModifyConfig, new SessionProcessor<Msg>() {
                    @Override
                    public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                        switch (rsp){
                            case ModifyConfigRsp:
                                DcsSetConfInfoRsp setConfInfoRsp = (DcsSetConfInfoRsp) rspContent;
                                if (!setConfInfoRsp.bSuccess) {
                                    cancelReq(Msg.ModifyConfig, resultListener);
                                    reportFailed(DcErrorCode.fromTransfer(setConfInfoRsp.dwErrorCode), resultListener);
                                }
                                break;
                            case ConfigModified:
                                if (((TDCSConfInfo)rspContent).achConfE164.equals(confE164)){
                                    reportSuccess(ToDoConverter.fromTransferObj((TDCSConfInfo)rspContent), resultListener);
                                }else {
                                    isConsumed[0] = false;
                                }
                                break;
                        }
                    }
                }, resultListener, ToDoConverter.toTransferObj(confInfo)); // 下设配置
            }

            @Override
            public void onFailed(int errorCode, Object errorInfo) {
                reportFailed(errorCode, resultListener);
            }

            @Override
            public void onTimeout() {
                reportTimeout(resultListener);
            }
        });
    }



    /**（管理方）添加协作方
     * @param memberE164 待添加对象e164
     * @param resultListener 结果监听器。
     *                       成功：null：
     *                       失败： {@link DcErrorCode#Failed}
     *                              {@link DcErrorCode#OperatorAmountReachLimit}
     */
    public void addOperator(String memberE164, IResultListener resultListener){
        addOperator(Collections.singletonList(memberE164), resultListener);
    }

    /**（管理方）批量添加协作方
     * @param memberE164List 待添加对象e164列表
     * @param resultListener 结果监听器。
     *                       成功：null
     *                       失败：{@link DcErrorCode#Failed}
     *                             {@link DcErrorCode#OperatorAmountReachLimit}
     **/
    public void addOperator(List<String> memberE164List, IResultListener resultListener){
        String confE164 = curDcConfE164;
        List<TDCSConfUserInfo> tdcsConfUserInfos = new ArrayList<>();
        for (String e164 : memberE164List){
            tdcsConfUserInfos.add(new TDCSConfUserInfo(e164, "", curTerminalType, true, true, false));
        }
        req(Msg.AddOperator, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                switch (rsp){
                    case AddOperatorRsp:
                        TDCSResult result = (TDCSResult) rspContent;
                        if (!result.bSuccess){
                            cancelReq(req, resultListener);
                            reportFailed(DcErrorCode.fromTransfer(result.dwErrorCode), resultListener);
                        }
                        break;
                    case OperatorAdded:
                        break;
                }
            }
        }, resultListener, new TDCSOperator(confE164, tdcsConfUserInfos));
    }

    /**（管理方）删除协作方
     * @param memberE164 待删除对象e164
     * @param resultListener 结果监听器。
     *                       成功：null
     *                       失败：{@link DcErrorCode#Failed}
     */
    public void delOperator(String memberE164, IResultListener resultListener){
        delOperator(Collections.singletonList(memberE164), resultListener);
    }

    /**（管理方）批量删除协作方
     * @param memberE164List 待删除对象e164列表
     * @param resultListener 结果监听器。
     *                       成功：null
     *                       失败：{@link DcErrorCode#Failed}
     */
    public void delOperator(List<String> memberE164List, IResultListener resultListener){
        String confE164 = curDcConfE164;
        List<TDCSConfUserInfo> tdcsConfUserInfos = new ArrayList<>();
        for (String e164 : memberE164List){
            tdcsConfUserInfos.add(new TDCSConfUserInfo(e164, "", curTerminalType, true, true, false));
        }
        req(Msg.DelOperator, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                switch (rsp){
                    case DelOperatorRsp:
                        if (!((TDCSResult) rspContent).bSuccess){
                            cancelReq(req, resultListener);
                            reportFailed(DcErrorCode.fromTransfer(((TDCSResult) rspContent).dwErrorCode), resultListener);
                        }
                        break;
                    case OperatorDeleted:
                        TDCSOperator para = (TDCSOperator) reqParas[0];
                        if (para.atOperList.equals(((TDCSUserInfos)rspContent).atUserInfoList)){
                            reportSuccess(null, resultListener);
                        }else {
                            isConsumed[0] = false;
                        }
                        break;
                }
            }
        }, resultListener, new TDCSOperator(confE164, tdcsConfUserInfos));
    }
    /**
     * （管理方）拒绝协作权申请
     * @param memberE164 被拒绝对象的e164
     * */
    public void rejectApplyOperator(String memberE164){
        rejectApplyOperator(Collections.singletonList(memberE164));
    }
    /**
     * （管理方）批量拒绝协作权申请
     * @param memberE164List 被拒绝对象的e164列表
     * */
    public void rejectApplyOperator(List<String> memberE164List){
        String confE164 = curDcConfE164;
        List<TDCSConfUserInfo> tdcsConfUserInfos = new ArrayList<>();
        for (String memberE164 : memberE164List) {
            tdcsConfUserInfos.add(new TDCSConfUserInfo(memberE164, "", curTerminalType, true, false, false));
        }
        req(Msg.RejectApplyOperator, null, null, new TDCSOperator(confE164, tdcsConfUserInfos));
    }


    /**（普通方）申请协作权
     * @param e164 申请者e164
     * @param resultListener 结果监听器。
     *                       成功：null
     *                       失败：{@link DcErrorCode#Failed}
     *                             {@link DcErrorCode#ApplyOperatorRejected}
     *
     *                       NOTE：申请协作权需等管理方审批，很可能出现等待超时然后管理方才审批的场景。
     *                       此场景下该监听器会回onTimeout，然后待管理方审批通过后会上报通知{@link OperatorChangedListener#onOperatorAdded(List)}。
     */
    public void applyForOperator(String e164, IResultListener resultListener){
        req(Msg.ApplyOperator, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                switch (rsp){
                    case ApplyOperatorRsp:
                        TDCSResult result = (TDCSResult) rspContent;
                        if (!result.bSuccess){
                            cancelReq(req, resultListener);
                            reportFailed(DcErrorCode.fromTransfer(result.dwErrorCode), resultListener);
                        }
                        break;
                    case OperatorAdded:
                        List<TDCSConfUserInfo> userInfos = ((TDCSUserInfos) rspContent).atUserInfoList;
                        if (e164.equals(userInfos.get(0).achE164)){
                            reportSuccess(null, resultListener);
                        }else {
                            isConsumed[0] = false;
                        }
                        break;
                    case ApplyOperatorRejected:
                        TDCSUserInfo userInfo = (TDCSUserInfo)rspContent;
                        if (e164.equals(userInfo.tUserInfo.achE164)){
                            reportFailed(DcErrorCode.ApplyOperatorRejected, resultListener);
                        }else {
                            isConsumed[0] = false;
                        }
                        break;
                }
            }
        }, resultListener, e164);
    }

    /**（协作方）释放协作权
     * @param e164 申请者e164
     * @param resultListener 结果监听器。
     *                       成功：null
     *                       失败：{@link DcErrorCode#Failed}
     */
    public void cancelOperator(String e164, IResultListener resultListener){
        req(Msg.CancelOperator, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                TDCSResult result = (TDCSResult) rspContent;
                if (result.bSuccess){
                    reportSuccess(null, resultListener);
                }else{
                    reportFailed(-1, resultListener);
                }
            }
        }, resultListener, e164);
    }

    /**获取所有成员
     * @param dcConfE164 协作所在会议e164
     * @param resultListener 结果监听器。
     *                       成功：List<{@link DCMember}>
     *                       失败：{@link DcErrorCode#Failed}
     */
    public void queryAllMembers(String dcConfE164, IResultListener resultListener){
        req(Msg.QueryAllMembers, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                DcsGetUserListRsp userListRsp = (DcsGetUserListRsp) rspContent;
                if (userListRsp.MainParam.bSuccess){
                    List<DCMember> dcMembers = new ArrayList<>();
                    for (TDCSConfUserInfo user : userListRsp.AssParam.atUserList){
                        dcMembers.add(ToDoConverter.fromTransferObj(user));
                    }
                    reportSuccess(dcMembers, resultListener);
                }else{
                    reportFailed(DcErrorCode.fromTransfer(userListRsp.MainParam.dwErrorCode), resultListener);
                }
            }
        }, resultListener, dcConfE164);
    }



    /**
     * 查询当前画板
     * @param resultListener onSuccess {@link BoardInfo}
     *                       onFailed errorCode
     * */
    public void queryCurrentBoard(IResultListener resultListener){
        String confE164 = curDcConfE164;
        req(Msg.QueryCurBoard, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                DcsGetWhiteBoardRsp queryBoardsResult = (DcsGetWhiteBoardRsp) rspContent;
                if (queryBoardsResult.MainParam.bSuccess){
                    reportSuccess(ToDoConverter.fromTransferObj(queryBoardsResult.AssParam, confE164), resultListener);
                }else{
                    reportFailed(DcErrorCode.fromTransfer(queryBoardsResult.MainParam.dwErrorCode), resultListener);
                }
            }
        }, resultListener, confE164);
    }


    /**
     * 查询所有画板
     * @param resultListener onSuccess List<{@link BoardInfo}>
     *                       onFailed errorCode
     * */
    public void queryAllBoards(IResultListener resultListener){
        String confE164 = curDcConfE164;
        req(Msg.QueryAllBoards, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                DcsGetAllWhiteBoardRsp queryAllBoardsResult = (DcsGetAllWhiteBoardRsp) rspContent;
                if (!queryAllBoardsResult.MainParam.bSuccess){
                    reportFailed(DcErrorCode.fromTransfer(queryAllBoardsResult.MainParam.dwErrorCode), resultListener);
                    return;
                }

                PriorityQueue<TDCSBoardInfo> priorityQueue = new PriorityQueue<>();
                priorityQueue.addAll(queryAllBoardsResult.AssParam.atBoardInfo); // 将board按annoyId升序排序
                List<TDCSBoardInfo> tdcsBoardInfos = new ArrayList<>();
                while (!priorityQueue.isEmpty()){
                    tdcsBoardInfos.add(priorityQueue.poll());
                }
                List<BoardInfo> boardInfos = new ArrayList<>();
                for (TDCSBoardInfo tdcsBoardInfo : tdcsBoardInfos) {
                    boardInfos.add(ToDoConverter.fromTransferObj(tdcsBoardInfo, confE164));
                }
                reportSuccess(boardInfos, resultListener);
            }
        }, resultListener, confE164);
    }


    /**
     * 新建普通画板
     * @param creatorE164 创建者E164
     * @param listener 新建画板结果监听器
     *                  成功：{@link BoardInfo}
     *                  失败：{@link DcErrorCode#Failed}
     *                        {@link DcErrorCode#BoardAmountReachLimit}
     * */
    public void newBoard(String creatorE164, IResultListener listener){
        String confE164 = curDcConfE164;
        req(Msg.NewBoard, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                switch (rsp){
                    case NewBoardRsp:
                        DcsNewWhiteBoardRsp newWhiteBoardRsp = (DcsNewWhiteBoardRsp) rspContent;
                        if (!newWhiteBoardRsp.MainParam.bSuccess) {
                            cancelReq(req, resultListener); // 后续不会有DCBoardCreatedNtf，取消以防等待超时
                            reportFailed(DcErrorCode.fromTransfer(newWhiteBoardRsp.MainParam.dwErrorCode), resultListener);
                        }
                        break;
                    case BoardCreated:
                        TDCSBoardInfo tdcsBoardInfo = (TDCSBoardInfo) rspContent;
                        TDCSNewWhiteBoard newWhiteBoard = (TDCSNewWhiteBoard) reqParas[0];
                        if (newWhiteBoard.tBoardinfo.achWbCreatorE164.equals(tdcsBoardInfo.achWbCreatorE164)) {
                            reportSuccess(ToDoConverter.fromTransferObj(tdcsBoardInfo, confE164), resultListener);
                        }else{
                            isConsumed[0] = false;
                        }
                        break;
                }
            }
        }, listener, new TDCSNewWhiteBoard(confE164, new TDCSBoardInfo(UUID.randomUUID().toString(), creatorE164)));
    }


    /**
     * 删除画板
     * @param boardId 待删除画板Id
     * @param resultListener 删除画板结果监听器
     *                  成功： string boardId（画板id）
     *                  失败：{@link DcErrorCode#Failed}
     * */
    public void delBoard(String boardId, IResultListener resultListener){
        String confE164 = curDcConfE164;
        req(Msg.DelBoard, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                switch (rsp){
                    case DelBoardRsp:
                        TDCSBoardResult boardResult = (TDCSBoardResult) rspContent;
                        if (!boardResult.bSuccess){
                            cancelReq(req, resultListener);
                            reportFailed(DcErrorCode.fromTransfer(boardResult.dwErrorCode), resultListener);
                        }
                        break;
                    case BoardDeleted:
                        TDCSDelWhiteBoardInfo boardInfo = (TDCSDelWhiteBoardInfo) rspContent;
                        String boardId = (String) reqParas[1];
                        if (boardId.equals(boardInfo.strIndex)){
                            reportSuccess(boardInfo.strIndex, resultListener);
                        }else{
                            isConsumed[0] = false;
                        }
                        break;
                }
            }
        }, resultListener, confE164, boardId);
    }


    /**
     * 删除所有画板
     * @param resultListener 结果监听器
     *                  成功：String e164（会议e164号）
     *                  失败：{@link DcErrorCode#Failed}
     * */
    public void delAllBoard(IResultListener resultListener){
        String confE164 = curDcConfE164;
        req(Msg.DelAllBoards, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                switch (rsp){
                    case DelAllBoardsRsp:
                        TDCSBoardResult allBoardRes = (TDCSBoardResult) rspContent;
                        if (!allBoardRes.bSuccess){
                            cancelReq(req, resultListener);
                            reportFailed(DcErrorCode.fromTransfer(allBoardRes.dwErrorCode), resultListener);
                        }
                        break;
                    case AllBoardDeleted:
                        TDCSDelWhiteBoardInfo delWhiteBoardInfo = (TDCSDelWhiteBoardInfo) rspContent;
                        reportSuccess(delWhiteBoardInfo.strConfE164, resultListener);
                        break;
                }
            }
        }, resultListener, confE164);
    }

    /**
     * 切换画板
     * @param boardId 目标画板Id
     * @param resultListener 切换画板结果监听器
     *                  成功：string boardId，画板Id
     *                  失败：{@link DcErrorCode#Failed}
     * */
    public void switchBoard(String boardId, IResultListener resultListener){
        String confE164 = curDcConfE164;
        req(Msg.SwitchBoard, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                switch (rsp){
                    case SwitchBoardRsp:
                        DcsSwitchRsp switchRsp = (DcsSwitchRsp) rspContent;
                        if (!switchRsp.MainParam.bSuccess){
                            cancelReq(req, resultListener);
                            reportFailed(DcErrorCode.fromTransfer(switchRsp.MainParam.dwErrorCode), resultListener);
                        }
                        break;
                    case BoardSwitched:
                        TDCSBoardInfo boardInfo1 = (TDCSBoardInfo) rspContent;
                        TDCSSwitchReq para = (TDCSSwitchReq) reqParas[0];
                        if (para.achTabId.equals(boardInfo1.achTabId)){
                            reportSuccess(boardInfo1.achTabId, resultListener);
                        }else{
                            isConsumed[0] = false;
                        }
                        break;
                }
            }
        }, resultListener, new TDCSSwitchReq(confE164, boardId));
    }



    /**发布绘制操作
     * @param op 绘制操作
     * @param resultListener onSuccess 已发布的OpPaint
     *                       onFailed errorCode // TODO
     * */
    public void publishPaintOp(OpPaint op, IResultListener resultListener){
        Object to = ToDoConverter.toPaintTransferObj(op);
        Object[] paras;
        if (null != to) {
            paras = new Object[]{ToDoConverter.toCommonPaintTransferObj(op), to};
        }else{
            paras = new Object[]{ToDoConverter.toCommonPaintTransferObj(op)};
        }
        req(ToDoConverter.opTypeToReqMsg(op.getType()), new SessionProcessor<Msg>() {
                    @Override
                    public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                        OpPaint opPaint = ToDoConverter.fromPaintTransferObj(rspContent);
                        if (null==opPaint || !opPaint.getAuthorE164().equals(op.getAuthorE164())){
                            isConsumed[0] = false;
                            return;
                        }
                        if (EOpType.INSERT_PICTURE != op.getType()) { // 对于图片插入操作还需上传图片。
                            reportSuccess(op, resultListener);
                        }
                    }
                }, resultListener, paras
        );

        // 对于图片插入操作还需上传图片。
        if (EOpType.INSERT_PICTURE == op.getType()){
            // 查询上传地址
            queryPicUploadUrl(new TDCSImageUrl(op.getConfE164(), op.getBoardId(), op.getPageId(), ((OpInsertPic) op).getPicId()), new IResultListener() {
                @Override
                public void onSuccess(Object result) {
                    TDCSImageUrl picUploadUrl = (TDCSImageUrl) result;
                    // 上传
                    uploadPic(picUploadUrl.achPicUrl,
                            new TDCSFileInfo(((OpInsertPic) op).getPicPath(), picUploadUrl.achWbPicentityId, picUploadUrl.achTabId, false, (int) new File(((OpInsertPic) op).getPicPath()).length()),
                            new IResultListener() {
                                @Override
                                public void onSuccess(Object result) { // 上传成功
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
                                    reportSuccess(op, resultListener);
                                }

                                @Override
                                public void onFailed(int errorCode, Object errorInfo) {
                                    reportFailed(-1, resultListener);
                                }

                                @Override
                                public void onTimeout() {
                                    reportTimeout(resultListener);
                                }
                            });

                }

                @Override
                public void onFailed(int errorCode, Object errorInfo) {
                    reportFailed(-1, resultListener);
                }

                @Override
                public void onTimeout() {
                    reportTimeout(resultListener);
                }
            });

        }
    }


    /**
     * 查询图片上传地址
     * */
    private void queryPicUploadUrl(TDCSImageUrl tdcsImageUrl, IResultListener resultListener){
        req(Msg.QueryPicUploadUrl, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                DcsUploadImageRsp queryPicUploadUrlResult = (DcsUploadImageRsp) rspContent;
                if (queryPicUploadUrlResult.MainParam.bSuccess){
                    reportSuccess(queryPicUploadUrlResult.AssParam, resultListener);
                }else{
                    reportFailed(DcErrorCode.fromTransfer(queryPicUploadUrlResult.MainParam.dwErrorCode), resultListener);
                }
            }
        }, resultListener, tdcsImageUrl);
    }

    /**
     * 上传图片
     * */
    private void uploadPic(String uploadUrl, TDCSFileInfo fileInfo, IResultListener resultListener){
        req(Msg.Upload, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                switch (rsp){
                    case UploadRsp:
                        if (!((TDCSFileLoadResult) rspContent).bSuccess){
                            cancelReq(req, resultListener);
                            reportFailed(DcErrorCode.Failed, resultListener);
                        }
                        break;
                    case PicDownloadable:
                        TDCSFileInfo uploadFileInfo = (TDCSFileInfo) reqParas[1];
                        TDCSImageUrl downloadableFileInfo = (TDCSImageUrl) rspContent;
                        if (uploadFileInfo.achWbPicentityId.equals(downloadableFileInfo.achWbPicentityId)) {
                            reportSuccess(rspContent, resultListener);
                        }else{
                            isConsumed[0] = false;
                        }
                        break;
                }
            }
        }, resultListener, new BaseTypeString(uploadUrl), fileInfo);
    }


    /**
     * 查询图片下载地址
     * */
    private void queryPicDownloadUrl(TDCSImageUrl tdcsImageUrl, IResultListener resultListener){
        req(Msg.QueryPicUrl, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                DcsDownloadImageRsp queryPicUrlResult = (DcsDownloadImageRsp) rspContent;
                if (!tdcsImageUrl.achWbPicentityId.equals(queryPicUrlResult.AssParam.achWbPicentityId)) {
                    isConsumed[0] = false;
                    return;
                }
                if (queryPicUrlResult.MainParam.bSuccess){
                    reportSuccess(queryPicUrlResult.AssParam, resultListener);
                }else{
                    reportFailed(DcErrorCode.fromTransfer(queryPicUrlResult.MainParam.dwErrorCode), resultListener);
                }
            }
        }, resultListener, tdcsImageUrl);
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
        KLog.p("starting synchronizeCachedStuff...");

        String curConfE164 = curDcConfE164;

        // 查询当前画板
        queryCurrentBoard(new IResultListener() {
            @Override
            public void onSuccess(Object result) {

                final String curBoardId = ((BoardInfo) result).getId();

                // 入会成功后准备同步会议中已有的图元。
                bPreparingSync = true;
                cachedPaintOps.clear();

                // 查询所有画板
                queryAllBoards(new IResultListener() {
                    @Override
                    public void onArrive(boolean bSuccess) {
                        bPreparingSync = false;
                    }

                    @Override
                    public void onSuccess(Object result) {
                        List<BoardInfo> dcBoards = (List<BoardInfo>) result;
                        // 检查准备阶段缓存的图元所在画板是否仍存在，若不存在则删除之。
                        Iterator it = cachedPaintOps.keySet().iterator();
                        while (it.hasNext()) {
                            boolean bMatched = false;
                            String tmpId = (String) it.next();
                            for (BoardInfo board : dcBoards) {
                                if (tmpId.equals(board.getId())) {
                                    bMatched = true;
                                    break;
                                }
                            }
                            if (!bMatched) {
                                it.remove();
                            }
                        }

                        Set<BoardOpListener> boardOpListeners = getNtfListeners(BoardOpListener.class);

                        Stream.of(boardOpListeners).forEach(boardOpListener -> {
                            // 上报用户协作中所有画板
                            for (BoardInfo board : dcBoards) {
                                boardOpListener.onBoardCreated(board);
                            }
                        });


                        if (null != curBoardId) {
                            for (BoardInfo board : dcBoards) {
                                if (board.getId().equals(curBoardId)) {
                                    // 上报用户切换到当前画板
                                    Stream.of(boardOpListeners).forEach(boardOpListener -> boardOpListener.onBoardSwitched(curBoardId));

                                    // 将当前画板放置在列表首位以优先同步
                                    dcBoards.remove(board);
                                    dcBoards.add(0, board);
                                    break;
                                }
                            }
                        }

                        // 同步画板中已有内容
                        totalSynBoardCount = dcBoards.size();
                        syncTimestamps.clear();
                        synProgress.clear();
                        synchronizeBoards(dcBoards);
                    }

                });
            }

        });

        // 同步人员列表
        queryAllMembers(curConfE164, new IResultListener() {
            @Override
            public void onSuccess(Object result) {
                List<DCMember> members = (List<DCMember>) result;
                List<DCMember> operators = new ArrayList<>();
                for (DCMember member : members) {
                    if (member.isbOperator()) {
                        operators.add(member);
                    }
                }
                Stream.of(getNtfListeners(OperatorChangedListener.class))
                        .forEach(operatorChangedListener -> operatorChangedListener.onOperatorAdded(operators));
            }
        });

    }


    private void synchronizeBoards(List<BoardInfo> dcBoards){
        if (dcBoards.isEmpty()){
            return;
        }
        for (BoardInfo boardInfo : dcBoards){
            KLog.p("to synchronize board %s", boardInfo.getId());
        }

        BoardInfo board = dcBoards.remove(0);
        String boardId = board.getId();

        // “逐个”画板同步（下层不支持一次性同步，会有问题）
        download(board.getElementUrl(), new TDCSFileInfo(null, null, boardId, true, 0), new IResultListener() {
            @Override
            public void onArrive(boolean bSuccess) {
                // 同步下一个画板
                synchronizeBoards(dcBoards);
            }

            @Override
            public void onSuccess(Object result) {
                // 上报同步进度
                reportSyncProgress(boardId, 15, false);

                PriorityQueue<OpPaint> ops = cachedPaintOps.get(boardId);
                if (null == ops) { // 若不为null则表明准备阶段已有该画板的实时图元到达，缓存队列在那时已创建，此处复用它即可
                    ops = new PriorityQueue<>();
                    cachedPaintOps.put(boardId, ops);
                }
                /* 后续会收到画板缓存的图元。
                 * 由于下层的begin-final消息不可靠，我们定时检查当前是否仍在同步图元，若同步结束则上报用户*/
                String id = getCachedOpsBoardId(boardId);
                Message msg = Message.obtain();
                msg.what = MsgID_CheckSynchronizing;
                msg.obj = id;
                handler.sendMessageDelayed(msg, 1000);
                KLog.p("start synchronizing ops for board %s", id);
                syncTimestamps.put(id, System.currentTimeMillis());
            }

            @Override
            public void onFailed(int errorCode, Object errorInfo) {
                // 上报同步进度
                reportSyncProgress(board.getId(), 0, true);
            }

            @Override
            public void onTimeout() {
                // 上报同步进度
                reportSyncProgress(board.getId(), 0, true);
            }
        });

    }


    private void download(String downloadUrl, TDCSFileInfo fileInfo, IResultListener resultListener){
        req(Msg.Download, new SessionProcessor<Msg>() {
                    @Override
                    public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, boolean isFinal, Msg req, Object[] reqParas, boolean[] isConsumed) {
                        TDCSFileLoadResult result = (TDCSFileLoadResult) rspContent;
                        TDCSFileInfo para1 = (TDCSFileInfo) reqParas[1];
                        if (null != para1.achWbPicentityId && !para1.achWbPicentityId.equals(result.achWbPicentityId)) {
                            isConsumed[0] = false; // 这是下载图片的响应且不是我请求的图片的结果。（下层消息不可靠，可能乱序、重复，故需做这样的过滤）
                            return;
                        }
                        if (result.bSuccess){
                            reportSuccess(result, resultListener);
                        }else{
                            reportFailed(DcErrorCode.Failed, resultListener);
                        }
                    }
                },
                resultListener,
                new BaseTypeString(downloadUrl),
                fileInfo
        );
    }


    private void reportSyncProgress(String boardId, int boardProgress, boolean bBoardFin){
        SyncProgress progress = synProgress.get(boardId);
        if (progress == null) {
            progress = new SyncProgress();
            synProgress.put(boardId, progress);
        }
        progress.progress = boardProgress;
        progress.finished = bBoardFin;

        AtomicInteger progressSum = new AtomicInteger();
        AtomicBoolean allFin = new AtomicBoolean(true);
        Stream.of(synProgress).forEach(it -> {
            progressSum.addAndGet(it.getValue().progress);
            if (!it.getValue().finished){
                allFin.set(false);
            }
        });
        int overallProgress = progressSum.get()/totalSynBoardCount;
        boolean allFinished = allFin.get();

        int synBoardCount = synProgress.size();
        Stream.of(getNtfListeners(SyncProgressListener.class)).forEach(it -> {
            KLog.p("onProgress(%s, %s, %s)", boardId, boardProgress, bBoardFin);
            it.onProgress(boardId, boardProgress, bBoardFin);
            KLog.p("onOverallProgress(%s, %s, %s, %s)", synBoardCount, totalSynBoardCount, overallProgress, allFinished);
            it.onOverallProgress(synBoardCount, totalSynBoardCount, overallProgress, allFinished);
        });
    }


    /*同步过程中缓存的操作*/
    private Map<String, PriorityQueue<OpPaint>> cachedPaintOps = new HashMap<>();
    private Map<String, Long> syncTimestamps = new HashMap<>();

    /* 是否正在准备同步。
    标记从入会成功到开始同步会议中已有图元这段时间，对这段时间内到达的图元
    我们也需像同步图元一样先缓存起来而不是直接上报给用户。*/
    private boolean bPreparingSync = false;

    /*同步的画板总数*/
    private int totalSynBoardCount;
    /*各画板的同步进度*/
    private Map<String, SyncProgress> synProgress = new HashMap<>();
    private class SyncProgress{
        int progress; // [0, 100]
        boolean finished;
    }

    private final int MsgID_CheckSynchronizing = 10;
    private Handler handler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MsgID_CheckSynchronizing:
                    String boardId = (String) msg.obj;
                    long timestamp = syncTimestamps.get(boardId);
                    if (System.currentTimeMillis()-timestamp > 500){ // 同步阶段若1s未收到后续绘制操作则认为同步结束
                        syncTimestamps.remove(boardId);
                        PriorityQueue<OpPaint> ops = cachedPaintOps.remove(boardId);
                        if (null == ops){
                            KLog.p(KLog.ERROR, "unexpected MsgID_CheckSynchronizing, no such synchronizing board(%s) exists", boardId);
                            return;
                        }
                        KLog.p("finish synchronizing ops for board %s", boardId);

                        reportSyncProgress(boardId, 100, true);

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
                        Stream.of(getNtfListeners(PaintOpListener.class)).forEach(paintOpListener -> {
                            for (OpPaint op : toReportOps) {
                                paintOpListener.onPaintOp(op);
                            }
                        });

                    }else{
                        KLog.p("synchronizing ops for board %s", boardId);
                        handler.sendMessageDelayed(Message.obtain(msg), 200); // 同步正在进行中，稍后再做检查是否已结束
                        int progress = Math.min(synProgress.get(boardId).progress+15, 99);
                        reportSyncProgress(boardId, progress, false);
                    }

                    break;
            }
        }
    };



    @Override
    protected void onNtf(Msg ntf, Object ntfContent) {

        switch (ntf){
            case LinkStateChanged:
                TDCSConnectResult tdcsConnectResult = (TDCSConnectResult) ntfContent;
                if (!tdcsConnectResult.bSuccess){ // 用户所属的数据协作链路状态异常
                    Stream.of(getNtfListeners(DcFinishedListener.class)).forEach(DcFinishedListener::onDcFinished);
                    curDcConfE164 = null;
                    clearSession();
                }
                break;

            case CollaborateStarted:
                TDCSCreateConfResult tdcsCreateConfResult = (TDCSCreateConfResult) ntfContent;
                if (tdcsCreateConfResult.bSuccess && null == curDcConfE164) {
                    curDcConfE164 = tdcsCreateConfResult.achConfE164;
                    Stream.of(getNtfListeners(DcCreatedListener.class)).forEach(it-> it.onDcCreated(ToDoConverter.fromTransferObj(tdcsCreateConfResult)));
                }
                break;

            case ConfigModified:
                DcConfInfo dcConfInfo = ToDoConverter.fromTransferObj((TDCSConfInfo)ntfContent);
                if (dcConfInfo.getConfE164().equals(curDcConfE164)){
                    Stream.of(getNtfListeners(SettingsChangedListener.class)).forEach(it-> it.onSettingsChanged(dcConfInfo));
                }
                break;
            case CollaborateFinished:
                break;

                // 协作方相关
            case OperatorAdded:
                Stream.of(getNtfListeners(OperatorChangedListener.class))
                        .forEach(it -> it.onOperatorAdded(ToDoConverter.fromDcUserList(((TDCSUserInfos)ntfContent).atUserInfoList)));
                break;
            case OperatorDeleted:
                Stream.of(getNtfListeners(OperatorChangedListener.class))
                        .forEach(it -> it.onOperatorDeleted(ToDoConverter.fromDcUserList(((TDCSUserInfos)ntfContent).atUserInfoList)));
                break;
            case ApplyOperatorNtf:
                Stream.of(getNtfListeners(ApplyOperatorListener.class))
                        .forEach(it -> it.onApplyOperator(ToDoConverter.fromTransferObj(((TDCSUserInfo)ntfContent).tUserInfo)));
                break;

                // 画板操作
            case BoardCreated:
                Stream.of(getNtfListeners(BoardOpListener.class))
                        .forEach(it -> it.onBoardCreated(ToDoConverter.fromTransferObj((TDCSBoardInfo) ntfContent, curDcConfE164)));
                break;
            case BoardDeleted:
                Stream.of(getNtfListeners(BoardOpListener.class))
                        .forEach(it -> it.onBoardDeleted(((TDCSDelWhiteBoardInfo) ntfContent).strIndex));
                break;
            case AllBoardDeleted:
                Stream.of(getNtfListeners(BoardOpListener.class))
                        .forEach(BoardOpListener::onAllBoardDeleted);
                break;
            case BoardSwitched:
                Stream.of(getNtfListeners(BoardOpListener.class))
                        .forEach(it -> it.onBoardSwitched(((TDCSBoardInfo) ntfContent).achTabId));
                break;

                // 绘制操作
            case LineDrawn:
            case OvalDrawn:
            case RectDrawn:
            case PathDrawn:
            case PicDragged:
            case PicDeleted:
            case Erased:
            case RectErased:
            case Matrixed:
            case Undone:
            case Redone:
            case ScreenCleared:
                OpPaint opPaint = ToDoConverter.fromPaintTransferObj(ntfContent);
                if (null != opPaint){
                    cacheOrReportPaintOp(opPaint);
                }
                break;

            case PicInserted: // NOTE:插入图片比较特殊，通知中只有插入图片操作的基本信息，图片本身可能还需进一步下载

                opPaint = ToDoConverter.fromPaintTransferObj(ntfContent);
                if (null != opPaint){
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
                        queryPicDownloadUrl(new TDCSImageUrl(confE164, boardId, pageId, picId), new IResultListener() {
                            @Override
                            public void onSuccess(Object result) {
                                TDCSImageUrl picUrl = (TDCSImageUrl) result;
                                // 下载图片
                                download(picUrl.achPicUrl,
                                        new TDCSFileInfo(getPicSavePath(picUrl.achWbPicentityId), picUrl.achWbPicentityId, picUrl.achTabId, false, 0),
                                        new IResultListener() {
                                            @Override
                                            public void onSuccess(Object result) {
                                                // 图片下载成功，更新“插入图片”操作
                                                TDCSFileLoadResult downRst = (TDCSFileLoadResult) result;
                                                updateInsertPicOp(downRst.achTabid, downRst.achWbPicentityId, downRst.achFilePathName);
                                            }
                                        });
                            }
                        });
                    }

                    cacheOrReportPaintOp(opPaint);
                }

                break;

            case PicDownloadable:
                /*己端展示图片的过程：
                协作方发出“插入图片”的操作并将图片上传服务器；
                己端先收到“插入图片”的通知，然后需等待“图片可下载”通知；
                一会后图片上传成功，平台广播各与会方“图片可下载”通知；
                己端收到该通知后主动去下载图片到本地；
                己端下载图片完成后结合此前收到的“插入图片”通知内的信息方可展示该图片。
                NOTE：有例外。己端刚加入数据协作时，平台不会给己端发送该通知，己端需要先拉取协作中已有的图元操作
                并针对其中的“插入图片”操作主动查询图片下载地址再根据下载地址下载图片。
                */
                TDCSImageUrl dcPicUrl = (TDCSImageUrl) ntfContent;
                if (!new File(getPicSavePath(dcPicUrl.achWbPicentityId)).exists()){ // 图片尚未下载到本地
                    // 下载图片
                    download(dcPicUrl.achPicUrl,
                            new TDCSFileInfo(getPicSavePath(dcPicUrl.achWbPicentityId), dcPicUrl.achWbPicentityId, dcPicUrl.achTabId, false, 0),
                            new IResultListener() {
                                @Override
                                public void onSuccess(Object result) {
                                    TDCSFileLoadResult downRst = (TDCSFileLoadResult) result;
                                    updateInsertPicOp(downRst.achTabid, downRst.achWbPicentityId, downRst.achFilePathName);
                                }
                            });

                }else{
                    KLog.p("pic already exists: %s", getPicSavePath(dcPicUrl.achWbPicentityId));
                }
                break;

            default:
                break;
        }
    }


    private String getPicSavePath(String picId){
        return PIC_SAVE_DIR +"/"+ picId + ".jpg";
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
                Stream.of(getNtfListeners(PaintOpListener.class)).forEach(it-> it.onPaintOp(op));
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
            OpUpdatePic opUpdatePic = new OpUpdatePic(boardId, picId, picPath);
            KLog.p("report user opUpdatePic %s", opUpdatePic);
            // 前面我们插入图片的操作并无实际效果，因为图片是“置空”的，此时图片已下载完成，通知用户更新图片。
            Stream.of(getNtfListeners(PaintOpListener.class)).forEach(it-> it.onPaintOp(opUpdatePic));
        }
    }

    private void clearSession(){
        handler.removeCallbacksAndMessages(null);
        assHandler.removeCallbacksAndMessages(null);
        cancelReq(null, null);
    }


    /**
     * 数据协作已创建通知监听器
     * */
    public interface DcCreatedListener extends INtfListener{
        /**
         * 数据协作已创建
         * 用户收到该通知后可调用{@link #startCollaborate(String, String, EDcMode, EConfType, String, List, IResultListener)}加入协作。
         * @param dcConfInfo 数据协作信息
         * */
        void onDcCreated(DcConfInfo dcConfInfo);
    }

    /**
     * 数据协作已结束监听器。
     * */
    public interface DcFinishedListener extends INtfListener {
        /**
         * （对己端而言）数据协作已结束。（协作本身可能仍存在也可能已不存在）
         * 数据协作被结束，或者己端被管理员从协作中删除均会触发该回调。
         * */
        void onDcFinished();
    }

    /**
     * 数据协作设置变更（如协作模式被修改）监听器
     * */
    public interface SettingsChangedListener extends INtfListener {
        /**
         * @param dcConfInfo 数据协作会议信息
         * */
        void onSettingsChanged(DcConfInfo dcConfInfo);
    }

    /**
     * 同步进度监听器。
     * 加入协作成功后会自动同步协作中已有内容。
     * */
    public interface SyncProgressListener extends INtfListener{
        /**
         * 单个画板同步进度。
         * @param boardId 画板ID
         * @param percentage 画板的同步百分比。0-100，0代表0%，100代表100%。
         * @param bFinished 同步是否结束。
         *                  NOTE: 同步结束不代表同步成功。
         *                  正常情况应是(percentage<100 && !bFinished) || (percentage==100 && bFinished)，
         *                  若同步失败则percentage<100 && bFinished
         * */
        void onProgress(String boardId, int percentage, boolean bFinished);

        /**
         * 整体同步进度。
         * @param syn 已同步以及正在同步的画板数
         * @param total 画板总数
         * @param percentage 总体进度
         * @param bFinished 整体同步是否已结束
         *                  NOTE: 同步结束不代表同步成功。
         *                  正常情况应是(percentage<100 && !bFinished) || (percentage==100 && bFinished)，
         *                  若同步失败则percentage<100 && bFinished
         * */
        void onOverallProgress(int syn, int total, int percentage, boolean bFinished);
    }

    /**
     * 绘制操作通知监听器。
     * */
    public interface PaintOpListener extends INtfListener {
        void onPaintOp(OpPaint op);
    }

    /**
     * 画板操作通知监听器。
     * */
    public interface BoardOpListener extends INtfListener{
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
     * 协作方变更监听器
     * */
    public interface OperatorChangedListener extends INtfListener{
        /**
         * 协作方被添加通知。
         * @param members 被添加的协作方信息
         * */
        void onOperatorAdded(List<DCMember> members);
        /**
         * 协作方被删除通知
         * @param members 被删除的协作方信息
         * */
        void onOperatorDeleted(List<DCMember> members);
    }


    /**
     * （与会方向管理员）申请协作权通知监听器
     * */
    public interface ApplyOperatorListener extends INtfListener{
        /**
         * @param member 申请者信息
         * */
        void onApplyOperator(DCMember member);
    }

}
