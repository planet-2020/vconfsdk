package com.sissi.vconfsdk.login;

import com.sissi.vconfsdk.base.DmMsg;
import com.sissi.vconfsdk.base.engine.Requester;
import com.sissi.vconfsdk.base.MsgBeans;
import com.sissi.vconfsdk.utils.KLog;

/**
 * Created by Sissi on 2018/9/12.
 */

public class LoginManager extends Requester {

    private LoginManager(){
    }

    public void login(String server, String account, String passwd, OnLoginResultListener loginResultListener){
        sendReq(DmMsg.LoginReq, new MsgBeans.LoginReq(server, account, passwd, MsgBeans.SetType.Phone), loginResultListener);
    }

    @Override
    protected void onRsp(Object listener, DmMsg rspId, Object rspContent) {
        KLog.p("rspId=%s, rspContent=%s",rspId, rspContent);
        if (DmMsg.LoginRsp.equals(rspId)){
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
