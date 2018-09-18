package com.sissi.vconfsdk.login;

import com.sissi.vconfsdk.base.DmMsg;
import com.sissi.vconfsdk.base.engine.Requester;
import com.sissi.vconfsdk.base.MsgBeans;
import com.sissi.vconfsdk.utils.KLog;

/**
 * Created by Sissi on 2018/9/12.
 */

public class LoginManager extends Requester {

    private LoginManager(){ }

    public void login(String server, String account, String passwd, OnLoginResultListener loginResultListener){
        sendReq(DmMsg.LoginReq, new MsgBeans.LoginReq(server, account, passwd, MsgBeans.SetType.Phone),
                new Object[]{new MsgBeans.LoginRsp(), new MsgBeans.LoginRspFin()},
                loginResultListener);
    }

    @Override
    protected void onRsp(Object listener, DmMsg rspId, Object rspContent) {
        KLog.p("rspId=%s, rspContent=%s",rspId, rspContent);
    }

    @Override
    protected void onTimeout(Object listener, DmMsg reqId) {
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
