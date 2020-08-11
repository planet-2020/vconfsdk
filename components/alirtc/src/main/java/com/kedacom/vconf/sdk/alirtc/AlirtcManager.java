package com.kedacom.vconf.sdk.alirtc;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.alibaba.alimeeting.uisdk.AMUIClientStatusEvent;
import com.alibaba.alimeeting.uisdk.AMUIErrorCode;
import com.alibaba.alimeeting.uisdk.AMUIFinishCode;
import com.alibaba.alimeeting.uisdk.AMUIMeetingCallBack;
import com.alibaba.alimeeting.uisdk.AMUIMeetingDetail;
import com.alibaba.alimeeting.uisdk.AMUIMeetingDetailConfig;
import com.alibaba.alimeeting.uisdk.AMUIMeetingJoinConfig;
import com.alibaba.alimeeting.uisdk.AliMeetingUIManager;
import com.alibaba.alimeeting.uisdk.widget.AMUIAvatarLayout;
import com.aliwork.meeting.api.member.AMSDKMeetingClient;
import com.annimon.stream.Stream;
import com.kedacom.vconf.sdk.alirtc.bean.ConfAboutToEnd;
import com.kedacom.vconf.sdk.alirtc.bean.ConfInvitationInfo;
import com.kedacom.vconf.sdk.alirtc.bean.JoinConfPara;
import com.kedacom.vconf.sdk.alirtc.bean.transfer.AliConfParam;
import com.kedacom.vconf.sdk.alirtc.bean.transfer.TAliConfWillEndInfo;
import com.kedacom.vconf.sdk.alirtc.bean.transfer.TConfInvitation;
import com.kedacom.vconf.sdk.alirtc.bean.transfer.TJoinConfPara;
import com.kedacom.vconf.sdk.alirtc.bean.transfer.TJoinConfResult;
import com.kedacom.vconf.sdk.alirtc.bean.transfer.TMtRegistCsvInfo;
import com.kedacom.vconf.sdk.amulet.Caster;
import com.kedacom.vconf.sdk.amulet.ILifecycleOwner;
import com.kedacom.vconf.sdk.amulet.IResultListener;
import com.kedacom.vconf.sdk.common.bean.TerminalType;
import com.kedacom.vconf.sdk.common.bean.transfer.TRegResultNtf;
import com.kedacom.vconf.sdk.common.constant.EmConfProtocol;
import com.kedacom.vconf.sdk.common.type.TNetAddr;
import com.kedacom.vconf.sdk.utils.log.KLog;
import com.kedacom.vconf.sdk.utils.net.NetAddrHelper;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AlirtcManager extends Caster<Msg> {
    private static AlirtcManager instance = null;
    private Application context;
    private int loginState = AliRtcResultCode.Failed;
    private static Handler loginStateChangedHandler = new Handler(Looper.getMainLooper());
    private TNetAddr rtcServerAddr;
    private String curConfNum;

    private AlirtcManager(Application ctx) {
        context = ctx;
    }

    public synchronized static AlirtcManager getInstance(Application ctx) {
        if (instance == null) {
            instance = new AlirtcManager(ctx);
            instance.initAliRtcSDK();
        }
        return instance;
    }


    @Override
    protected Map<Class<? extends ILifecycleOwner>, Msg> regNtfListenerType() {
        Map<Class<? extends ILifecycleOwner>, Msg> listenerType2CaredNtf = new HashMap<>();
        listenerType2CaredNtf.put(OnLoginStateChangedListener.class, Msg.LoginStateChanged);
        listenerType2CaredNtf.put(OnConfInvitingListener.class, Msg.ConfInviting);
        listenerType2CaredNtf.put(OnConfAboutToEndListener.class, Msg.ConfAboutToEnd);
        return listenerType2CaredNtf;
    }

    /**
     * 当前登录状态
     * @return {@link AliRtcResultCode#LoginSuccess}已成功登录，{@link AliRtcResultCode#LogoutSuccess} 已成功注销，其他登录失败。
     * */
    public int loginState(){
        return loginState;
    }

    /**
     * 登录
     * @param type 终端类型
     * @param version 终端软件版本
     * @param resultListener 成功返回null；
     *                       失败返回错误码。
     * */
    public void login(@NonNull TerminalType type, @NonNull String version, IResultListener resultListener){
        rtcServerAddr = (TNetAddr) get(Msg.GetServerAddr);
        if (null == rtcServerAddr){
            reportFailed(-1, resultListener);
            return;
        }

        req(Msg.Login, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                TRegResultNtf result = (TRegResultNtf) rspContent;
                if (EmConfProtocol.emaliyun.ordinal() != result.MainParam.basetype){
                    isConsumed[0] = false;
                    return;
                }
                loginState = AliRtcResultCode.trans(rsp, result.AssParam.basetype);
                if (loginState == AliRtcResultCode.LoginSuccess) {
                    reportSuccess(null, resultListener);
                } else {
                    reportFailed(loginState, resultListener);
                }
            }
        }, resultListener, rtcServerAddr, new TMtRegistCsvInfo(type.getVal(), version, true));
    }


    /**
     * 注销
     * @param resultListener 成功返回null；
     *                       失败返回错误码。
     * */
    public void logout(IResultListener resultListener){
        if (null == rtcServerAddr){
            reportFailed(-1, resultListener);
            return;
        }
        req(Msg.Logout, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                TRegResultNtf result = (TRegResultNtf) rspContent;
                if (EmConfProtocol.emaliyun.ordinal() != result.MainParam.basetype){
                    isConsumed[0] = false;
                    return;
                }
                loginState = AliRtcResultCode.trans(rsp, result.AssParam.basetype);
                if (loginState == AliRtcResultCode.LogoutSuccess) {
                    reportSuccess(null, resultListener);
                } else {
                    reportFailed(loginState, resultListener);
                }
            }

            @Override
            public void onTimeout(IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                loginState = AliRtcResultCode.LogoutSuccess;
            }
        }, resultListener, rtcServerAddr);
    }


    /**
     * 是否正在会议中
     * */
    public boolean isInConference(){
        return curConfNum != null;
    }

    /**
     * 加入会议
     * @param joinConfPara 入会参数
     * @param resultListener
     *          成功返回： null
     *          失败返回：错误码
     * */
    public void joinConf(@NonNull JoinConfPara joinConfPara, IResultListener resultListener){
        curConfNum = null;

        req(Msg.JoinConf, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                TJoinConfResult joinConfResult = (TJoinConfResult) rspContent;
                if (joinConfResult.bSuccess){
                    AliConfParam para = joinConfResult.tAliJoinConfParam;

                    AMUIMeetingDetail meetingDetail = new AMUIMeetingDetail();
                    meetingDetail.subject = para.achConfName;
                    meetingDetail.password = joinConfPara.password;
                    meetingDetail.shareLink = String.format("https://%s/login.html?%s", NetAddrHelper.ipLongLittleEndian2Str(rtcServerAddr.dwIp), joinConfPara.confNum); // 分享链接格式由平台指定
                    meetingDetail.beginDate = 0;
                    meetingDetail.endDate = 0;
                    meetingDetail.shareMessage = "您被邀请参加会议"+joinConfPara.confNum;

                    AMUIMeetingJoinConfig.Builder builder = new AMUIMeetingJoinConfig.Builder()
                            .setMeetingCode(para.achConfCode)
                            .setUserId(para.achUsrId)
                            .setUserName("userName")
                            .setMeetingDetailConfig( // 用于入会
                                    new AMUIMeetingDetailConfig(
                                            para.achClientAppid,
                                            para.achConfToken,
                                            para.achConfDomain,
                                            para.achConfCode,
                                            para.achConfuuid,
                                            para.achMemuuid
                                    )
                            )
                            .setMeetingDetail(// 用于邀请
                                    meetingDetail
                            )
                            .setOpenBeautifyDefault(true)
                            .setAppIdentifier("appTag")
                            .setAppVersion("0.1.0")
                            .setAppNotifyIconRes(R.drawable.notification_icon)
                            .setOpenCameraDefault(!joinConfPara.closeCamera)
                            .setMuteAudioDefault(joinConfPara.closeMic)
                            .setMeetingCallBack(new AMUIMeetingCallBack() {
                                @Override
                                public void onClientStatusChanged(@NotNull AMSDKMeetingClient amsdkMeetingClient, @NotNull AMUIClientStatusEvent amuiClientStatusEvent) {
                                    KLog.p("#####onClientStatusChanged %s", amuiClientStatusEvent);
                                }

                                @Override
                                public void onMeetingJoined() {
                                    KLog.p("#####onMeetingJoined");
                                    curConfNum = joinConfPara.confNum;
                                    req(Msg.ReportConfState, null,null, joinConfPara.confNum, true);
                                    req(Msg.ReportVoiceState, null,null, joinConfPara.confNum, joinConfPara.closeMic, false);  // FIXME 根据配置填
                                    reportSuccess(null, resultListener);
                                }

                                @Override
                                public void onMeetingFinished(@NotNull AMUIFinishCode amuiFinishCode, @Nullable String s) {
                                    KLog.p("#####onMeetingFinished %s, %s", amuiFinishCode, s);
                                    curConfNum = null;
                                    req(Msg.ReportConfState, null,null, joinConfPara.confNum, false);
                                }

                                @Override
                                public void onJoinMeetingError(@NotNull AMUIErrorCode amuiErrorCode, @Nullable String s) {
                                    KLog.p("#####onJoinMeetingError %s, %s", amuiErrorCode, s);
                                    reportFailed(-1, resultListener);
                                }
                            });

                    AliMeetingUIManager.joinMeeting(context, builder.builder());

                }else{
                    reportFailed(AliRtcResultCode.trans(rsp, joinConfResult.dwErrorCode), resultListener);
                }
            }
        }, resultListener, new TJoinConfPara(joinConfPara.confNum, joinConfPara.password));

    }


//    /**
//     * 退出会议
//     * */
//    // ali ui sdk将退会封在了sdk内部自己做了，所以该接口用不着了
//    public void quitConf(String confNum, IResultListener resultListener){
//        // TODO 调用阿里接口退出会议
//        req(Msg.ReportConfState, null, null, confNum, false);
//    }


    private void initAliRtcSDK() {
        //初始化分为两部分，
        //2. UI配置，目前仅支持邀请人定制，如果不设置AliMeetingUIManager.uiController， 则没有参会人列表不会有邀请人选项
        AliMeetingUIManager.initManager(context);

        AliMeetingUIManager.setUiController(new AliMeetingUIManager.IAMUIMeetingUIController() {
            @Override
            public boolean onInviteAction(@NotNull View view, @NotNull FragmentActivity activity, @Nullable AMUIMeetingDetail detail) {
                return false;
            }

            @Override
            public boolean loadAvatar(@NotNull AMUIAvatarLayout view, @Nullable String url, @NotNull AMSDKMeetingClient client, @Nullable String userName) {
                return super.loadAvatar(view, url, client, userName);
            }

            @Override
            public boolean showMeetingDetail(@NotNull View view, @NotNull FragmentActivity activity, @Nullable AMUIMeetingDetail detail) {
                return super.showMeetingDetail(view, activity, detail);
            }
        });

//        AliMeetingUIManager.uiController = object : AliMeetingUIManager.AliMeetingUiController {
//            override fun onInviteAction(
//                    view: View, activity: FragmentActivity, meetingBrief: AliMeetingBrief
//            ) {
//                MaterialDialog.Builder(activity)
//                        .items("分享到钉钉", "复制邀请信息")
//                        .itemsCallback { _, _, which, _ ->
//                        when (which) {
//                    0 -> {
//                        // 分享到钉钉
//                        Toast.makeText(activity, "分享到钉钉", Toast.LENGTH_SHORT).show()
//                    }
//                    1 -> {
//                        // 复制邀请信息
//                        Toast.makeText(activity, "复制邀请信息", Toast.LENGTH_SHORT).show()
//                    }
//                            else -> {
//                    }
//                }
//                }
//                    .show()
//            }
//
//        }
    }


    @Override
    protected void onNotification(Msg ntf, Object ntfContent, Set<ILifecycleOwner> ntfListeners) {
        switch (ntf){
            case LoginStateChanged:
                TRegResultNtf regResultNtf = (TRegResultNtf) ntfContent;
                if(regResultNtf.MainParam.basetype == 6){ // alirtc服务器
                    int state = AliRtcResultCode.trans(ntf, regResultNtf.AssParam.basetype);
                    if (loginState != state){ // 登录状态有变
                        loginState = state;
                        loginStateChangedHandler.removeCallbacksAndMessages(null);
                        // 延迟处理避免频繁上报用户
                        loginStateChangedHandler.postDelayed(() -> Stream.of(ntfListeners).forEach(it -> {
                            if (containsNtfListener(it)) { // 因为是延迟通知，可能在延迟的时间段内监听器已销毁了，所以需判断该监听器是否仍存在
                                ((OnLoginStateChangedListener) it).onLoginStateChanged(loginState);
                            }
                        }), 3000);
                    }
                }
                break;
            case ConfInviting:
                Stream.of(ntfListeners).forEach(it ->
                        ((OnConfInvitingListener)it).onConfInviting(ToDoConverter.TConfInvitation2ConfInvitationInfo((TConfInvitation) ntfContent))
                );
            case ConfAboutToEnd:
                Stream.of(ntfListeners).forEach(it ->
                        ((OnConfAboutToEndListener)it).onConfAboutToEnd(ToDoConverter.TAliConfWillEndInfo2ConfAboutToEnd((TAliConfWillEndInfo) ntfContent))
                );
                break;
        }
    }


    /**
     * 登录状态变更监听器
     * */
    public interface OnLoginStateChangedListener extends ILifecycleOwner{
        /**
         * 登录状态变更
         * @param state 当前登录状态。{@link AliRtcResultCode#OK}已登录，其他：未登录。
         * */
        void onLoginStateChanged(int state);
    }

    /**
     * 会议邀请通知监听器
     * */
    public interface OnConfInvitingListener extends ILifecycleOwner{
        /**
         * 会议邀请。
         * 收到邀请后可调用{@link #joinConf(JoinConfPara, IResultListener)}加入会议，
         * 或者忽略表示拒绝邀请。
         * */
        void onConfInviting(ConfInvitationInfo confInvitationInfo);
    }

    /**
     * 会议即将结束监听器
     * */
    public interface OnConfAboutToEndListener extends ILifecycleOwner{
        /**
         * 会议即将结束
         * */
        void onConfAboutToEnd(ConfAboutToEnd confAboutToEnd);
    }

}
