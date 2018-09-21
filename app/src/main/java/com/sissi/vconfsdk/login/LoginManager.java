package com.sissi.vconfsdk.login;

import com.sissi.vconfsdk.base.Msg;
import com.sissi.vconfsdk.base.MsgBeans;
import com.sissi.vconfsdk.base.Requester;
import com.sissi.vconfsdk.utils.KLog;

/**
 * Created by Sissi on 2018/9/12.
 */

public class LoginManager extends Requester {

    private LoginManager(){
    }

    public void login(String server, String account, String passwd, OnLoginResultListener loginResultListener){
        setConfig(Msg.SetNetConfig, new MsgBeans.NetConfig(1234555, 65530));
        MsgBeans.XmppServerInfo xmppServerInfo = (MsgBeans.XmppServerInfo) getConfig(Msg.GetXmppServerInfo);
        KLog.p("xmppServerInfo{%s, %d}",xmppServerInfo.domain, xmppServerInfo.ip);
        sendReq(Msg.LoginReq, new MsgBeans.LoginReq(server, account, passwd, MsgBeans.SetType.Phone), loginResultListener);
    }

    @Override
    protected void onRsp(Object listener, Msg rspId, Object rspContent) {
        KLog.p("rspId=%s, rspContent=%s",rspId, rspContent);
        if (Msg.LoginRsp.equals(rspId)){
            MsgBeans.LoginResult loginRes = (MsgBeans.LoginResult) rspContent;
            if (null != listener){
                if (0 == loginRes.result) {
                    ((OnLoginResultListener) listener).onLoginSuccess();
                }else{
                    ((OnLoginResultListener) listener).onLoginFailed(loginRes.result);
                }
            }
        }
    }

    @Override
    protected void onTimeout(Object listener, Msg reqId) {
        KLog.p("listener=%s, reqId=%s",listener, reqId);
    }


    public interface OnLoginResultListener{
        void onLoginSuccess();
        void onLoginFailed(int errorCode);
        void onLoginTimeout();
    }

    public interface OnLogoutResultListener{
        void onLogoutSuccess();
        void onLogoutFailed(int errorCode);
        void onLogoutTimeout();
    }
}
