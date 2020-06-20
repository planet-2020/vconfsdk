package com.kedacom.vconf.sdk.alirtc;

import com.kedacom.vconf.sdk.alirtc.bean.transfer.TCreateAliConfParam;
import com.kedacom.vconf.sdk.alirtc.bean.transfer.TCreateAliConfResult;
import com.kedacom.vconf.sdk.alirtc.bean.transfer.TJoinConfResult;
import com.kedacom.vconf.sdk.alirtc.bean.transfer.TMtRegistCsvInfo;
import com.kedacom.vconf.sdk.annotation.Message;
import com.kedacom.vconf.sdk.annotation.Request;
import com.kedacom.vconf.sdk.annotation.Response;
import com.kedacom.vconf.sdk.common.bean.transfer.TRegResultNtf;
import com.kedacom.vconf.sdk.common.type.TNetAddr;

import static com.kedacom.vconf.sdk.annotation.Request.GET;


/**
 * Created by Sissi on 2019/7/19.
 * 启动模块消息定义。
 */

@Message(
        module = "ALIRTC"
)
enum Msg {

    /**获取用于对接alirtc的平台服务器地址
     * */
    @Request(method = "GetCsvAddr",
            owner = MethodOwner.ConfigCtrl,
            paras = StringBuffer.class,
            userParas = TNetAddr.class,
            type = GET
    )
    GetServerAddr,

    /**
     * 登录用于对接alirtc的平台服务器
     * */
    @Request(method = "LoginCsvCmd",
            owner = MethodOwner.ConfCtrl,
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


    /** 创会
     * */
    @Request(method = "MGCreateAliConfCmd",
            owner = MethodOwner.MeetingCtrl,
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
            owner = MethodOwner.ConfCtrl,
            paras = StringBuffer.class,
            userParas = String.class,  // 会议号
            rspSeq = "JoinConfRsp"
    )
    JoinConf,

    @Response(id = "JoinAliConfParam_Ntf",
            clz = TJoinConfResult.class)
    JoinConfRsp,

    /**通知keda平台会议状态。
     * 入会退会时均需通知keda平台。
     * NOTE: keda平台和ali平台不通，keda平台无法感知终端的ali会议状态。
     *      终端在ali平台上入会成功后需要主动上报自身会议状态给keda平台。
     * */
    @Request(method = "NotifyCsvConfState",
            owner = MethodOwner.ConfCtrl,
            paras = {StringBuffer.class,
                    boolean.class},
            userParas = {String.class, // 会议号
                    boolean.class   // 是否在会议中。入会后true，退会后false
            }
    )
    ReportConfStateToCSV,

    END;

    private static class MethodOwner {
        private static final String PKG = "com.kedacom.kdv.mt.mtapi.";

        private static final String KernalCtrl = PKG+"KernalCtrl";
        private static final String MtcLib = PKG+"MtcLib";
        private static final String LoginCtrl = PKG+"LoginCtrl";
        private static final String MonitorCtrl = PKG+"MonitorCtrl";
        private static final String CommonCtrl = PKG+"CommonCtrl";
        private static final String ConfigCtrl = PKG+"ConfigCtrl";
        private static final String ConfCtrl = PKG+"ConfCtrl";
        private static final String MtServiceCfgCtrl = PKG+"MtServiceCfgCtrl";
        private static final String MeetingCtrl = PKG+"MeetingCtrl";
    }
}
