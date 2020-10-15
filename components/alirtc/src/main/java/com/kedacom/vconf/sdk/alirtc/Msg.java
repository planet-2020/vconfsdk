package com.kedacom.vconf.sdk.alirtc;

import com.kedacom.vconf.sdk.alirtc.bean.transfer.TAliConfWillEndInfo;
import com.kedacom.vconf.sdk.alirtc.bean.transfer.TConfInvitation;
import com.kedacom.vconf.sdk.alirtc.bean.transfer.TCreateAliConfParam;
import com.kedacom.vconf.sdk.alirtc.bean.transfer.TCreateAliConfResult;
import com.kedacom.vconf.sdk.alirtc.bean.transfer.TJoinConfPara;
import com.kedacom.vconf.sdk.alirtc.bean.transfer.TJoinConfResult;
import com.kedacom.vconf.sdk.alirtc.bean.transfer.TMtRegistCsvInfo;
import com.kedacom.vconf.sdk.annotation.Module;
import com.kedacom.vconf.sdk.annotation.Notification;
import com.kedacom.vconf.sdk.annotation.Request;
import com.kedacom.vconf.sdk.annotation.Response;
import com.kedacom.vconf.sdk.common.type.transfer.TRegResultNtf;
import com.kedacom.vconf.sdk.common.type.transfer.TNetAddr;

import static com.kedacom.vconf.sdk.annotation.Request.*;

@Module(name = "ALIRTC")
enum Msg {

    /**获取CSV服务器（用于对接alirtc的平台服务器）地址
     * */
    @Request(name = "GetCsvAddr",
            owner = ConfigCtrl,
            paras = StringBuffer.class,
            userParas = TNetAddr.class,
            outputParaIndex = LastIndex
    )
    GetServerAddr,

    /**
     * 登录CSV
     * */
    @Request(name = "LoginCsvCmd",
            owner = ConfCtrl,
            paras = {StringBuffer.class,
                    StringBuffer.class},
            userParas = {TNetAddr.class,
                    TMtRegistCsvInfo.class},
            rspSeq = "LoginStateChanged"
            )
    Login,

    /**
     * 注销CSV
     * */
    @Request(name = "LogoutCsvCmd",
            owner = ConfCtrl,
            paras = StringBuffer.class,
            userParas = TNetAddr.class,
            rspSeq = "LoginStateChanged"
    )
    Logout,

    /**
     * 登录状态变更
     */
    @Notification
    @Response(name = "RegResultNtf", clz = TRegResultNtf.class)
    LoginStateChanged,

    /** 创会
     * */
    @Request(name = "MGCreateAliConfCmd",
            owner = MeetingCtrl,
            paras = StringBuffer.class,
            userParas = TCreateAliConfParam.class,
            rspSeq = "CreateConfRsp"
    )
    CreateConf,

    @Response(name = "RestCreateConference_Rsp",
            clz = TCreateAliConfResult.class)
    CreateConfRsp,


    /** 加入会议
     * */
    @Request(name = "JoinAliConfCmd",
            owner = ConfCtrl,
            paras = StringBuffer.class,
            userParas = TJoinConfPara.class,
            rspSeq = "JoinConfRsp"
    )
    JoinConf,

    @Response(name = "JoinAliConfParam_Ntf",
            clz = TJoinConfResult.class)
    JoinConfRsp,

    /**上报会议状态。
     * NOTE: keda平台和ali平台不通，keda平台无法感知终端的ali会议状态。
     *      终端在ali平台上入会成功后需要主动上报自身会议状态给keda平台。
     * */
    @Request(name = "NotifyCsvConfState",
            owner = ConfCtrl,
            paras = {StringBuffer.class,
                    boolean.class},
            userParas = {String.class, // 会议号
                    boolean.class   // 是否在会议中。入会后true，退会后false
            }
    )
    ReportConfState,

    /**上报静音哑音状态。
     * */
    @Request(name = "NotifyCsvVoiceState",
            owner = ConfCtrl,
            paras = {StringBuffer.class,
                    boolean.class,
                    boolean.class
            },
            userParas = {String.class, // 会议号
                    boolean.class,   // 是否哑音。true哑音
                    boolean.class,   // 是否静音。true静音
            }
    )
    ReportVoiceState,

    /**
     * 会议邀请通知
     * */
    @Notification(name = "RcvInvitedFromCsv_Ntf",
            clz = TConfInvitation.class)
    ConfInviting,

    /**
     * 会议即将结束通知
     * */
    @Notification(name = "AliConfWillEnd_Ntf",
            clz = TAliConfWillEndInfo.class)
    ConfAboutToEnd,

}
