package com.kedacom.vconf.webrtc;

import com.kedacom.vconf.sdk.annotation.Message;
import com.kedacom.vconf.sdk.annotation.Request;
import com.kedacom.vconf.sdk.annotation.Response;
import com.kedacom.vconf.webrtc.been.trans.*;

/**
 * Created by Sissi on 2019/10/24
 */
@Message(
        module = "RTC"
)
enum Msg {
    /**获取Rtc服务器地址*/
    @Request(method = "GetRtcSvrCfg",
            owner = MethodOwner.ConfigCtrl,
            paras = StringBuffer.class,
            userParas = TMtRtcSvrAddr.class,
            type = Request.GET)
    GetSvrAddr,

    /**
     * 登录Rtc服务器
     */
    @Request(method = "SetRtcSvrCfgCmd",
            owner = MethodOwner.ConfigCtrl,
            paras = StringBuffer.class,
            userParas = TMtRtcSvrAddr.class,
            rspSeq = "LoginRsp")
    Login,

    /**
     * 登录Rtc服务器响应
     */
    @Response(clz = TLoginResult.class,
            id = "RegResultNtf")
    LoginRsp,


    /**获取流列表*/
    @Request(method = "GetRtcStreamList",
            owner = MethodOwner.MonitorCtrl,
            paras = StringBuffer.class,
            userParas = TRtcStreamInfoList.class,
            type = Request.GET)
    GetStreamList,

    /**
     * 获取流数量
     */
    @Request(method = "GetRtcStreamListNum",
            owner = MethodOwner.MonitorCtrl,
            paras = StringBuffer.class,
            userParas = int.class,
            type = Request.GET)
    GetStreamCount,

    /**
     * 设置播放参数
     */
    @Request(method = "SetRtcPlayCmd",
            owner = MethodOwner.MonitorCtrl,
            paras = StringBuffer.class,
            userParas = TRtcPlayParam.class,
            type = Request.SET)
    SetPlayPara,


    @Response(clz = TRtcStreamInfoList.class,
            id = "RtcStreamList_Ntf")
    StreamListReady,

    @Response(clz = TRtcStreamInfoList.class,
            id = "RtcStreamAdd_Ntf")
    StreamJoined,

    @Response(clz = TRtcStreamInfoList.class,
            id = "RtcStreamLeft_Ntf")
    StreamLeft,

    END;

    private static class MethodOwner {
        private static final String PKG = "com.kedacom.kdv.mt.mtapi.";

        private static final String MonitorCtrl = PKG + "MonitorCtrl";
        private static final String ConfigCtrl = PKG + "ConfigCtrl";
    }

}
