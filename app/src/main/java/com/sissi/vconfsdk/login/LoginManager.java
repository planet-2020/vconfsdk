package com.sissi.vconfsdk.login;

import android.os.Handler;

import com.sissi.vconfsdk.base.Msg;
import com.sissi.vconfsdk.base.MsgBeans;
import com.sissi.vconfsdk.base.RequestAgent;
import com.sissi.vconfsdk.utils.KLog;

/**
 * Created by Sissi on 2018/9/12.
 */

public class LoginManager extends RequestAgent {

    private LoginManager(){
    }

    public void login(String server, String account, String passwd, OnLoginResultListener loginResultListener){
//        set(Msg.SetNetConfig, new MsgBeans.NetConfig(1234555, 65530));
//        MsgBeans.XmppServerInfo xmppServerInfo = (MsgBeans.XmppServerInfo) get(Msg.GetXmppServerInfo);
//        KLog.p("xmppServerInfo{%s, %d}",xmppServerInfo.domain, xmppServerInfo.ip);
        req(Msg.LoginReq, new MsgBeans.LoginReq(server, account, passwd, MsgBeans.SetType.Phone), loginResultListener);
    }

    @Override
    protected void onRsp(Msg rspId, Object rspContent, Object listener) {
        KLog.p("rspId=%s, rspContent=%s, listener=%s",rspId, rspContent, listener);
        if (Msg.LoginRsp.equals(rspId)){

        }else if (Msg.LoginRspFin.equals(rspId)){
            MsgBeans.LoginResult loginRes = (MsgBeans.LoginResult) rspContent;
            if (null != listener){
                if (0 == loginRes.result) {
//                    new Handler().postDelayed(((OnLoginResultListener) listener)::onLoginSuccess, 3000);
                    ((OnLoginResultListener) listener).onLoginSuccess();
                }else{
                    ((OnLoginResultListener) listener).onLoginFailed(loginRes.result);
                }
            }
        }
    }

    @Override
    protected void onTimeout(Msg reqId, Object listener) {
        KLog.p("listener=%s, reqId=%s",listener, reqId);
        if (Msg.LoginReq.equals(reqId)) {
            if (null != listener) {
                ((OnLoginResultListener) listener).onLoginTimeout();
            }
        }
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
