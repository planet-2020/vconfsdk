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
        return processorMap;
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
            MsgBeans.DcsLinkCreationResult linkCreationResult = (MsgBeans.DcsLinkCreationResult) rspContent;
            if (!linkCreationResult.bSuccess
                    && null != listener){
                cancelReq(Msg.DCSLoginSrvReq, listener);  // 后续不会有DcsLoginSrv_Rsp上来，取消该请求以防等待超时。
                listener.onResponse(ResultCode.FAILED, null);
            }
        }else if (Msg.DcsLoginSrv_Rsp.equals(rspId)){
            MsgBeans.DcsLoginResult loginRes = (MsgBeans.DcsLoginResult) rspContent;
            if (null != listener){
                if (loginRes.bSucces) {
                    listener.onResponse(ResultCode.SUCCESS, null);
                }else{
                    listener.onResponse(ResultCode.FAILED, null);
                }
            }
        }
    }
}
