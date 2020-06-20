package com.kedacom.vconf.sdk.alirtc;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;

import com.kedacom.vconf.sdk.alirtc.bean.transfer.TCreateAliConfParam;
import com.kedacom.vconf.sdk.alirtc.bean.transfer.TCreateAliConfResult;
import com.kedacom.vconf.sdk.alirtc.bean.transfer.TJoinConfResult;
import com.kedacom.vconf.sdk.alirtc.bean.transfer.TMtRegistCsvInfo;
import com.kedacom.vconf.sdk.amulet.Caster;
import com.kedacom.vconf.sdk.amulet.IResultListener;
import com.kedacom.vconf.sdk.common.bean.TerminalType;
import com.kedacom.vconf.sdk.common.bean.transfer.TRegResultNtf;
import com.kedacom.vconf.sdk.common.constant.EmConfProtocol;
import com.kedacom.vconf.sdk.common.constant.EmRegFailedReason;
import com.kedacom.vconf.sdk.common.type.TNetAddr;

import java.util.Map;

public class AlirtcManager extends Caster<Msg> {
    private static AlirtcManager instance = null;
    private Context context;

    private AlirtcManager(Context ctx) {
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
    protected Map<Msg[], NtfProcessor<Msg>> subscribeNtfs() {
        return null;
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
            public boolean onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas) {
                TRegResultNtf result = (TRegResultNtf) rspContent;
                if (EmConfProtocol.emaliyun.ordinal() != result.MainParam.basetype){
                    return false;
                }
                if (EmRegFailedReason.emRegSuccess.getValue() == result.AssParam.basetype) {
                    reportSuccess(null, resultListener);
                } else {
                    reportFailed(-1, resultListener);
                }
                return true;
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
            public boolean onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas) {
                TRegResultNtf result = (TRegResultNtf) rspContent;
                if (EmConfProtocol.emaliyun.ordinal() != result.MainParam.basetype){
                    return false;
                }
                if (EmRegFailedReason.emUnRegSuc.getValue() == result.AssParam.basetype) {
                    reportSuccess(null, resultListener);
                } else {
                    reportFailed(-1, resultListener);
                }
                return true;
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
            public boolean onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas) {
                TCreateAliConfResult result = (TCreateAliConfResult) rspContent;
                if (1000 == result.MainParam.dwErrorID){
                    String confNum = result.AssParam.basetype;
                    reportSuccess(new CreateConfResult(confName, confNum), resultListener);
                }else{
                    reportFailed(-1, resultListener);
                }
                return true;
            }
        }, resultListener, new TCreateAliConfParam(2, confName, duration, true));
    }


    /**
     * 加入会议
     * @param confNum 会议号码
     * @param resultListener
     *          成功返回： null
     *          失败返回：错误码
     * */
    public void joinConf(String confNum, IResultListener resultListener){
        req(Msg.JoinConf, new SessionProcessor<Msg>() {
            @Override
            public boolean onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas) {
                TJoinConfResult joinConfResult = (TJoinConfResult) rspContent;
                if (joinConfResult.bSuccess){
//                    AliConfParam para = joinConfResult.tAliJoinConfParam;
//                    AliMeetingJoinConfig aliMeetingJoinConfig = new AliMeetingJoinConfig.Builder()
//                            .userId(para.achUsrId)
//                            .securityTransport(false)
//                            .openCameraDefault(true)
//                            .muteAudioDefault(false)
//                            .onlyMasterCanHangUp(true)
//                            .onlyMasterCanMuteAudio(true)
//                            .meetingCode(para.achConfCode)
//                            .meetingDetailConfig(para.achConfAppid, para.achConfToken, para.achConfDomain,
//                                    para.achConfCode, para.achConfuuid, para.achMemuuid, null)
//                            .builder();
//
//                    AliMeetingUIManager.joinMeeting(context, aliMeetingJoinConfig);

                    reportSuccess(null, resultListener); // FIXME 等joinMeeting加入阿里会议成功后再上报
                    req(Msg.ReportConfState, null,null, confNum, true); // FIXME 等加入阿里会议成功后再上报
                    req(Msg.ReportVoiceState, null,null, confNum, false, false); // FIXME 等加入阿里会议成功后再上报

                }else{
                    reportFailed(-1, resultListener);
                }
                return true;
            }
        }, resultListener, confNum);
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

}
