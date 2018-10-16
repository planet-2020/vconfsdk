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

    @Response
    Timeout,

    // startup
    @Request(para = MsgBeans.StartupPara.class,  // para表示传入参数
            rspSeq = {"StartupRsp"},
            timeout = 4)
    Startup, // 请求不带Req后缀

    @Response(clz = MsgBeans.StartupResult.class, // Result表示反馈结果，注意区别Info
            delay = 3000)
    StartupRsp,

    // login
    @Request(para = MsgBeans.LoginPara.class,
            rspSeq = {"LoginRsp", "LoginRspFin"},
            timeout = 6)
    Login,

    @Response(clz = MsgBeans.LoginResult.class, delay = 5000)
    LoginRsp,
    @Response(clz = MsgBeans.LoginResult.class, delay = 500)
    LoginRspFin,

    // logout
    @Request(para = MsgBeans.LogoutPara.class, rspSeq = {"LogoutRsp", "LogoutRspFin"}, timeout = 5)
    Logout,
    @Response(clz = String.class)
    LogoutRsp,
    @Response(clz = String.class)
    LogoutRspFin,

    @Get(result = MsgBeans.XmppServerInfo.class)
    GetXmppServerInfo,

    @Set(MsgBeans.NetConfig.class)
    SetNetConfig,

    @Notification(clz = MsgBeans.MemberState.class, delay = 6000)
    MemberStateChanged,
}
