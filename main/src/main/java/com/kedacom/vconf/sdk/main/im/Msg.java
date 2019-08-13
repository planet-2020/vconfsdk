package com.kedacom.vconf.sdk.main.im;

import com.kedacom.vconf.sdk.annotation.Message;
import com.kedacom.vconf.sdk.annotation.Request;
import com.kedacom.vconf.sdk.annotation.Response;
import com.kedacom.vconf.sdk.main.im.bean.*;

/**
 * Created by Sissi on 2019/7/29
 */
@Message(
        module = "IM"
)
enum Msg {

    @Request(method = "GetXNUCfg",
            owner = MethodOwner.ConfigCtrl,
            paras = StringBuffer.class,
            userParas = TIMSvrAddr.class,
            type = Request.GET)
    GetServerAddr,


    @Request(method = "IMUserLoginReq",
            owner = MethodOwner.ImCtrl,
            paras = StringBuffer.class,
            userParas = TImUserLogin.class,
            rspSeq = "LoginRsp")
    Login,


    @Response(id = "ImLoginRsp",
            clz = TImLoginResult.class)
    LoginRsp,


    END;

    private static class MethodOwner {
        private static final String PKG = "com.kedacom.kdv.mt.mtapi.";

        private static final String ConfigCtrl = PKG + "ConfigCtrl";
        private static final String ImCtrl = PKG + "ImCtrl";
    }

}
