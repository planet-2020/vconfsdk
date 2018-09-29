package com.sissi.vconfsdk.base;


import android.support.annotation.RestrictTo;

import com.sissi.vconfsdk.annotation.Get;
import com.sissi.vconfsdk.annotation.Message;
import com.sissi.vconfsdk.annotation.Notification;
import com.sissi.vconfsdk.annotation.Request;
import com.sissi.vconfsdk.annotation.Response;
import com.sissi.vconfsdk.annotation.Set;

/**
 * Created by Sissi on 2018/9/3.
 */

@RestrictTo(RestrictTo.Scope.LIBRARY)
@Message
public enum Msg {
    // common
    Timeout,
    NetworkUnavaliable,

    // startup
    @Request(para = MsgBeans.StartupInfo.class,
            rspSeq = {"StartupRsp"},
            timeout = 4)
    StartupReq,

    @Response(clz = MsgBeans.StartupResult.class)
    StartupRsp,

    // login
    @Request(para = MsgBeans.LoginReq.class,
            rspSeq = {"LoginRsp", "LoginRspFin"},
            timeout = 6)
    LoginReq,

    @Response(clz = MsgBeans.LoginResult.class, delay = 5000)
    LoginRsp,
    @Response(clz = MsgBeans.LoginResult.class, delay = 500)
    LoginRspFin,

    // logout
    @Request(para = MsgBeans.LogoutReq.class, rspSeq = {"LogoutRsp", "LogoutRspFin"}, timeout = 5)
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
