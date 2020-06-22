package com.kedacom.vconf.sdk.alirtc;

import com.kedacom.vconf.sdk.alirtc.bean.transfer.TCreateAliConfParam;
import com.kedacom.vconf.sdk.alirtc.bean.transfer.TCreateAliConfResult;
import com.kedacom.vconf.sdk.alirtc.bean.transfer.TJoinConfResult;
import com.kedacom.vconf.sdk.alirtc.bean.transfer.TMtRegistCsvInfo;
import com.kedacom.vconf.sdk.amulet.Atlas;
import com.kedacom.vconf.sdk.annotation.Message;
import com.kedacom.vconf.sdk.annotation.Request;
import com.kedacom.vconf.sdk.annotation.Response;
import com.kedacom.vconf.sdk.common.bean.transfer.TRegResultNtf;
import com.kedacom.vconf.sdk.common.type.TNetAddr;


@Message(
        module = "ALIRTC"
)
enum Msg {

    /**获取CSV服务器（用于对接alirtc的平台服务器）地址
     * */
    @Request(method = "GetCsvAddr",
            owner = Atlas.ConfigCtrl,
            paras = StringBuffer.class,
            userParas = TNetAddr.class,
            isGet = true
    )
    GetServerAddr,

    /**
     * 登录CSV
     * */
    @Request(method = "LoginCsvCmd",
            owner = Atlas.ConfCtrl,
            paras = {StringBuffer.class,
                    StringBuffer.class},
            userParas = {TNetAddr.class,
                    TMtRegistCsvInfo.class},
            rspSeq = "LoginRsp"
            )
    Login,

    @Response(id = "RegResultNtf",
            clz = TRegResultNtf.class)
    LoginRsp,

    /**
     * 注销CSV
     * */
    @Request(method = "LogoutCsvCmd",
            owner = Atlas.ConfCtrl,
            paras = StringBuffer.class,
            userParas = TNetAddr.class,
            rspSeq = "LogoutRsp"
    )
    Logout,

    @Response(id = "RegResultNtf",
            clz = TRegResultNtf.class)
    LogoutRsp,


    /** 创会
     * */
    @Request(method = "MGCreateAliConfCmd",
            owner = Atlas.MeetingCtrl,
            paras = StringBuffer.class,
            userParas = TCreateAliConfParam.class,
            rspSeq = "CreateConfRsp"
    )
    CreateConf,

    @Response(id = "RestCreateConference_Rsp",
            clz = TCreateAliConfResult.class)
    CreateConfRsp,


    /** 加入会议
     * */
    @Request(method = "JoinAliConfCmd",
            owner = Atlas.ConfCtrl,
            paras = StringBuffer.class,
            userParas = String.class,  // 会议号
            rspSeq = "JoinConfRsp"
    )
    JoinConf,

    @Response(id = "JoinAliConfParam_Ntf",
            clz = TJoinConfResult.class)
    JoinConfRsp,

    /**上报会议状态。
     * NOTE: keda平台和ali平台不通，keda平台无法感知终端的ali会议状态。
     *      终端在ali平台上入会成功后需要主动上报自身会议状态给keda平台。
     * */
    @Request(method = "NotifyCsvConfState",
            owner = Atlas.ConfCtrl,
            paras = {StringBuffer.class,
                    boolean.class},
            userParas = {String.class, // 会议号
                    boolean.class   // 是否在会议中。入会后true，退会后false
            }
    )
    ReportConfState,

    /**上报静音哑音状态。
     * */
    @Request(method = "NotifyCsvVoiceState",
            owner = Atlas.ConfCtrl,
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


    END;

}
