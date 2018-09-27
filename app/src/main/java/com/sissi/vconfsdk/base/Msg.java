package com.sissi.vconfsdk.base;


import com.sissi.vconfsdk.annotation.Get;
import com.sissi.vconfsdk.annotation.Message;
import com.sissi.vconfsdk.annotation.Notification;
import com.sissi.vconfsdk.annotation.Request;
import com.sissi.vconfsdk.annotation.Response;
import com.sissi.vconfsdk.annotation.Set;

/**
 * Created by Sissi on 2018/9/3.
 */

@Message
public enum Msg {

    // startup
    @Request(reqPara = MsgBeans.StartupInfo.class,
            rspSeq = {"StartupRsp"},
            timeout = 4)
    StartupReq,

    @Response(clz = MsgBeans.StartupResult.class)
    StartupRsp,

    // login
    @Request(reqPara = MsgBeans.LoginReq.class,
            rspSeq = {"LoginRsp", "LoginRspFin"},
            timeout = 6)
    LoginReq,

    @Response(clz = MsgBeans.LoginResult.class, delay = 5000)
    LoginRsp,
    @Response(clz = MsgBeans.LoginResult.class)
    LoginRspFin,

    // logout
    @Request(reqPara = MsgBeans.LogoutReq.class, rspSeq = {"LogoutRsp", "LogoutRspFin"}, timeout = 5)
    LogoutReq,
    @Response(clz = String.class)
    LogoutRsp,
    @Response(clz = String.class)
    LogoutRspFin,

    @Get(result = MsgBeans.XmppServerInfo.class)
    GetXmppServerInfo,

    @Set(MsgBeans.NetConfig.class)
    SetNetConfig,

    @Notification(clz = MsgBeans.MemberState.class, delay = 2000)
    MemberStateChangedNtf,
}
