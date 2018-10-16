package com.sissi.vconfsdk.login;

import com.sissi.vconfsdk.base.IOnResponseListener;
import com.sissi.vconfsdk.base.Msg;
import com.sissi.vconfsdk.base.MsgBeans;
import com.sissi.vconfsdk.base.RequestAgent;
import com.sissi.vconfsdk.base.ResultCode;
import com.sissi.vconfsdk.utils.KLog;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Sissi on 2018/9/12.
 */

public class LoginManager extends RequestAgent {

    private LoginManager(){
    }

    @Override
    protected Map<Msg, RspProcessor> rspProcessors() {
        Map<Msg, RspProcessor> rspProcessorMap = new HashMap<>();

        rspProcessorMap.put(Msg.Login, this::processLoginResponses);

        return rspProcessorMap;
    }

    @Override
    protected Map<Msg, NtfProcessor> ntfProcessors() {
        return null;
    }

    public void login(String server, String account, String passwd, IOnResponseListener loginResultListener){
//        set(Msg.SetNetConfig, new MsgBeans.NetConfig(1234555, 65530));
//        MsgBeans.XmppServerInfo xmppServerInfo = (MsgBeans.XmppServerInfo) get(Msg.GetXmppServerInfo);
//        KLog.p("xmppServerInfo{%s, %d}",xmppServerInfo.domain, xmppServerInfo.ip);
        req(Msg.Login, new MsgBeans.LoginPara(server, account, passwd, MsgBeans.SetType.Phone), loginResultListener);
    }



    private void processLoginResponses(Msg rspId, Object rspContent, IOnResponseListener listener){
        KLog.p("rspId=%s, rspContent=%s, listener=%s",rspId, rspContent, listener);
        if (Msg.LoginRsp.equals(rspId)){

        }else if (Msg.LoginRspFin.equals(rspId)){
            MsgBeans.LoginResult loginRes = (MsgBeans.LoginResult) rspContent;
            if (null != listener){
                if (0 == loginRes.result) {
                    listener.onResponse(ResultCode.SUCCESS, null);
                }else{
                    listener.onResponse(ResultCode.FAILED, null);
                }
            }
        }
/*        else if (Msg.Timeout.equals(rspId)){
            if (null != listener){
                listener.onResponse(ResultCode.TIMEOUT, null); // agent会通知上层已超时，此处不需要再次通知，只需要做一些善后工作，比如保存/清空数据等。
            }
        }*/
    }

}
