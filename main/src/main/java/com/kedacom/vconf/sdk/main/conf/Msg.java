package com.kedacom.vconf.sdk.main.conf;

import com.kedacom.vconf.sdk.annotation.Message;
import com.kedacom.vconf.sdk.annotation.Request;
import com.kedacom.vconf.sdk.annotation.Response;
import com.kedacom.vconf.sdk.main.conf.bean.*;

/**
 * Created by Sissi on 2019/7/29
 */
@Message(
        module = "CONF"
)
enum Msg {

    @Request(method = "GetCSUCfg",
            owner = MethodOwner.ConfigCtrl,
            paras = StringBuffer.class,
            userParas = TGkSvrAddr.class,
            type = Request.GET)
    GetGkServerAddr,

    @Request(method = "GetSipSvrCfg",
            owner = MethodOwner.ConfigCtrl,
            paras = StringBuffer.class,
            userParas = TSipSvrAddr.class,
            type = Request.GET)
    GetSipServerAddr,

    @Request(method = "SetCSUCfgCmd",
            owner = MethodOwner.ConfigCtrl,
            paras = StringBuffer.class,
            userParas = TLoginGkPara.class,
            rspSeq = "LoginGkRsp")
    LoginGk,

    @Response(id = "RegResultNtf",
            clz = TLoginGkResult.class)
    LoginGkRsp,

//    @Request(method = "SetCSUCfgCmd",
//            owner = MethodOwner.ConfigCtrl,
//            paras = StringBuffer.class,
//            userParas = TDCSRegInfo.class,
//            rspSeq = "LoginGkRsp")
//    LoginSip,
//
//
//    @Response(id = "RegResultNtf",
//            clz = TLoginGkResult.class)
//    LoginSipRsp,

    END;

    private static class MethodOwner {
        private static final String PKG = "com.kedacom.kdv.mt.mtapi.";

        private static final String KernalCtrl = PKG + "KernalCtrl";
        private static final String ConfigCtrl = PKG + "ConfigCtrl";
        private static final String DcsCtrl = PKG + "DcsCtrl";
    }

}
