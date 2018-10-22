package com.sissi.vconfsdk.datacollaborate;

import com.sissi.vconfsdk.base.IResultListener;
import com.sissi.vconfsdk.base.Msg;
import com.sissi.vconfsdk.base.MsgBeans;
import com.sissi.vconfsdk.base.MsgConst;
import com.sissi.vconfsdk.base.RequestAgent;
import com.sissi.vconfsdk.base.ResultCode;
import com.sissi.vconfsdk.utils.KLog;

import java.util.HashMap;
import java.util.Map;

public class DataCollaborateManager extends RequestAgent {

    @Override
    protected Map<Msg, RspProcessor> rspProcessors() {
        Map<Msg, RspProcessor> processorMap = new HashMap<>();
        processorMap.put(Msg.DCSLoginSrvReq, this::processLoginResponses);
        return null;
    }

    @Override
    protected Map<Msg, NtfProcessor> ntfProcessors() {
        return null;
    }

    public void login(String serverIp, int port, MsgConst.EmDcsType type, IResultListener resultListener){
        req(Msg.DCSLoginSrvReq, new MsgBeans.TDCSRegInfo(serverIp, port, type), resultListener);
    }

    private void processLoginResponses(Msg rspId, Object rspContent, IResultListener listener){
        KLog.p("rspId=%s, rspContent=%s, listener=%s",rspId, rspContent, listener);
        if (Msg.DcsLoginResult_Ntf.equals(rspId)){

        }else if (Msg.DcsLoginSrv_Rsp.equals(rspId)){
            MsgBeans.LoginResult loginRes = (MsgBeans.LoginResult) rspContent;
            if (null != listener){
                if (0 == loginRes.result) {
                    listener.onResponse(ResultCode.SUCCESS, null);
                }else{
                    listener.onResponse(ResultCode.FAILED, null);
                }
            }
        }
    }
}
