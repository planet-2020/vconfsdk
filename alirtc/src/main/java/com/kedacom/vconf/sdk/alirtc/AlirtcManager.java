package com.kedacom.vconf.sdk.alirtc;

import android.app.Application;

import androidx.annotation.NonNull;

import com.alibaba.alimeeting.uisdk.AMUIClientStatusEvent;
import com.alibaba.alimeeting.uisdk.AMUIErrorCode;
import com.alibaba.alimeeting.uisdk.AMUIFinishCode;
import com.alibaba.alimeeting.uisdk.AMUIMeetingCallBack;
import com.alibaba.alimeeting.uisdk.AMUIMeetingDetailConfig;
import com.alibaba.alimeeting.uisdk.AMUIMeetingJoinConfig;
import com.alibaba.alimeeting.uisdk.AliMeetingUIManager;
import com.aliwork.meeting.api.member.AMSDKMeetingClient;
import com.kedacom.vconf.sdk.alirtc.bean.ConfInvitationInfo;
import com.kedacom.vconf.sdk.alirtc.bean.transfer.AliConfParam;
import com.kedacom.vconf.sdk.alirtc.bean.transfer.TConfInvitation;
import com.kedacom.vconf.sdk.alirtc.bean.transfer.TCreateAliConfParam;
import com.kedacom.vconf.sdk.alirtc.bean.transfer.TCreateAliConfResult;
import com.kedacom.vconf.sdk.alirtc.bean.transfer.TJoinConfPara;
import com.kedacom.vconf.sdk.alirtc.bean.transfer.TJoinConfResult;
import com.kedacom.vconf.sdk.alirtc.bean.transfer.TMtRegistCsvInfo;
import com.kedacom.vconf.sdk.amulet.Caster;
import com.kedacom.vconf.sdk.amulet.IResultListener;
import com.kedacom.vconf.sdk.common.bean.TerminalType;
import com.kedacom.vconf.sdk.common.bean.transfer.TRegResultNtf;
import com.kedacom.vconf.sdk.common.constant.EmConfProtocol;
import com.kedacom.vconf.sdk.common.constant.EmRegFailedReason;
import com.kedacom.vconf.sdk.common.type.TNetAddr;
import com.kedacom.vconf.sdk.utils.lifecycle.ILifecycleOwner;
import com.kedacom.vconf.sdk.utils.log.KLog;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

public class AlirtcManager extends Caster<Msg> {
    private static AlirtcManager instance = null;
    private Application context;

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


    /**
     * 登录
     * @param type 终端类型
     * @param version 终端软件版本
     * @param resultListener 成功返回null；
     *                       失败返回错误码。
     * */
    public void login(@NonNull TerminalType type, @NonNull String version, IResultListener resultListener){
        TNetAddr addr = (TNetAddr) get(Msg.GetServerAddr);
        if (null == addr){
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
                if (EmRegFailedReason.emRegSuccess.getValue() == result.AssParam.basetype) {
                    reportSuccess(null, resultListener);
                } else {
                    reportFailed(-1, resultListener);
                }
            }
        }, resultListener, addr, new TMtRegistCsvInfo(type.getVal(), version, true));
    }


    /**
     * 注销
     * @param resultListener 成功返回null；
     *                       失败返回错误码。
     * */
    public void logout(IResultListener resultListener){
        TNetAddr addr = (TNetAddr) get(Msg.GetServerAddr);
        if (null == addr){
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
                if (EmRegFailedReason.emUnRegSuc.getValue() == result.AssParam.basetype) {
                    reportSuccess(null, resultListener);
                } else {
                    reportFailed(-1, resultListener);
                }
            }
        }, resultListener, addr);
    }


    /**
     * 创建会议
     * @param confName 会议名称
     * @param duration 会议时长。单位：分钟
     * @param resultListener
     *          成功返回： {@link CreateConfResult}
     *          失败返回：错误码
     * */
    public void createConf(@NonNull String confName, int duration, IResultListener resultListener){
        req(Msg.CreateConf, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                TCreateAliConfResult result = (TCreateAliConfResult) rspContent;
                if (1000 == result.MainParam.dwErrorID){
                    String confNum = result.AssParam.basetype;
                    reportSuccess(new CreateConfResult(confName, confNum), resultListener);
                }else{
                    reportFailed(-1, resultListener);
                }
            }
        }, resultListener, new TCreateAliConfParam(2, confName, duration, true));
    }


    /**
     * 加入会议
     * @param confNum 会议号码
     * @param password 会议密码。没有密码则置空
     * @param resultListener
     *          成功返回： null
     *          失败返回：错误码
     * */
    public void joinConf(@NonNull String confNum, String password, IResultListener resultListener){
        req(Msg.JoinConf, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                TJoinConfResult joinConfResult = (TJoinConfResult) rspContent;
                if (joinConfResult.bSuccess){
                    AliConfParam para = joinConfResult.tAliJoinConfParam;

                    AMUIMeetingJoinConfig.Builder builder = new AMUIMeetingJoinConfig.Builder()
                            .setMeetingCode(para.achConfCode)
                            .setUserId(para.achUsrId)
                            .setUserName("userName")
                            .setMeetingDetailConfig(
                                    new AMUIMeetingDetailConfig(
                                            para.achClientAppid,
                                            para.achConfToken,
                                            para.achConfDomain,
                                            para.achConfCode,
                                            para.achConfuuid,
                                            para.achMemuuid
                                    )
                            )
                            .setOpenBeautifyDefault(true)
                            .setAppIdentifier("appTag")
                            .setAppVersion("0.1.0")
                            .setAppNotifyIconRes(R.drawable.notification_icon)
                            .setOpenCameraDefault(true)
                            .setMuteAudioDefault(false)
//                            .setMeetingDetail(detail)
                            .setMeetingCallBack(new AMUIMeetingCallBack() {
                                @Override
                                public void onClientStatusChanged(@NotNull AMSDKMeetingClient amsdkMeetingClient, @NotNull AMUIClientStatusEvent amuiClientStatusEvent) {
                                    KLog.p("#####onClientStatusChanged %s", amuiClientStatusEvent);
                                }

                                @Override
                                public void onMeetingJoined() {
                                    KLog.p("#####onMeetingJoined");
                                    req(Msg.ReportConfState, null,null, confNum, true);
                                    req(Msg.ReportVoiceState, null,null, confNum, false, false);  // FIXME 根据配置填
                                    reportSuccess(null, resultListener);
                                }

                                @Override
                                public void onMeetingFinished(@NotNull AMUIFinishCode amuiFinishCode, @Nullable String s) {
                                    KLog.p("#####onMeetingFinished %s, %s", amuiFinishCode, s);
                                    req(Msg.ReportConfState, null,null, confNum, false);
                                }

                                @Override
                                public void onJoinMeetingError(@NotNull AMUIErrorCode amuiErrorCode, @Nullable String s) {
                                    KLog.p("#####onJoinMeetingError %s, %s", amuiErrorCode, s);
                                    reportFailed(-1, resultListener);
                                }
                            });

                    AliMeetingUIManager.joinMeeting(context, builder.builder());

                }else{
                    reportFailed(AliRtcResultCode.trans(req, joinConfResult.dwErrorCode), resultListener);
                }
            }
        }, resultListener, new TJoinConfPara(confNum, password));

    }


    /**
     * 退出会议
     * */
    public void quitConf(String confNum, IResultListener resultListener){
        // TODO 调用阿里接口退出会议
        req(Msg.ReportConfState, null, null, confNum, false); // FIXME 等退出阿里会议成功后再上报
    }


    private void initAliRtcSDK() {
        //初始化分为两部分，
        //2. UI配置，目前仅支持邀请人定制，如果不设置AliMeetingUIManager.uiController， 则没有参会人列表不会有邀请人选项
        AliMeetingUIManager.initManager(context);


//        AliMeetingUIManager.setUiController(new AliMeetingUIManager.AliMeetingUiController() {
//            @Override
//            public void onInviteAction(@NotNull View view, @NotNull FragmentActivity fragmentActivity, @NotNull AliMeetingBrief aliMeetingBrief) {
//                KLog.p("view =%s, frag=%s, userId=%s, meetingCode=%s", view, fragmentActivity, aliMeetingBrief.getUserId(), aliMeetingBrief.getMeetingCode());
//            }
//        });

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
            case ConfInviting:
                for (ILifecycleOwner listener : ntfListeners){
                    ((OnConfInvitingListener)listener).onConfInviting(ToDoConverter.TConfInvitation2ConfInvitationInfo((TConfInvitation) ntfContent));
                }
                break;
        }
    }

    /**
     * 会议邀请通知监听器
     * */
    public interface OnConfInvitingListener extends ILifecycleOwner{
        /**
         * 会议邀请。
         * 收到邀请后可调用{@link #joinConf(String, String, IResultListener)}加入会议，
         * 或者忽略表示拒绝邀请。
         * */
        void onConfInviting(ConfInvitationInfo confInvitationInfo);
    }
    /**
     * 添加会议邀请通知监听器
     * */
    public void addOnConfInvitingListener(OnConfInvitingListener listener){
        addNtfListener(Msg.ConfInviting, listener);
    }

}
