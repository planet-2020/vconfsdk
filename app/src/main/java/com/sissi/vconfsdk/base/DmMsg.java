package com.sissi.vconfsdk.base;


import com.sissi.annotation.Get;
import com.sissi.annotation.Message;
import com.sissi.annotation.Notification;
import com.sissi.annotation.Request;
import com.sissi.annotation.Response;
import com.sissi.annotation.Set;

/**
 * Created by Sissi on 2018/9/3.
 */

@Message
public enum DmMsg { // Domain Message

    // login
    @Request(reqPara = MsgBeans.LoginReq.class,
            rspSeq = {"LoginRsp", "LoginRspFin"},
            timeout = 6)
    LoginReq,

    @Response(MsgBeans.LoginResult.class)
    LoginRsp,
    @Response(MsgBeans.LoginRspFin.class)
    LoginRspFin,

    // logout
    @Request(reqPara = MsgBeans.LogoutReq.class, rspSeq = {"LogoutRsp", "LogoutRspFin"}, timeout = 5)
    LogoutReq,
    @Response(String.class)
    LogoutRsp,
    @Response(String.class)
    LogoutRspFin,

    @Get(result = MsgBeans.XmppServerInfo.class)
    GetXmppServerInfo,

    @Set(MsgBeans.NetConfig.class)
    SetNetConfig,

    @Notification(MsgBeans.MemberState.class)
    MemberStateChangedNtf,
}
