package com.kedacom.vconf.sdk.startup;

import com.kedacom.vconf.sdk.base.IResponseListener;
import com.kedacom.vconf.sdk.base.Msg;
import com.kedacom.vconf.sdk.base.MsgBeans;
import com.kedacom.vconf.sdk.base.RequestAgent;
import com.kedacom.vconf.sdk.base.CommonResultCode;
import com.kedacom.vconf.sdk.utils.KLog;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Sissi on 2018/9/12.
 */

public class StartManager extends RequestAgent {

    static {
        KLog.enable(true);
        KLog.setLevel(KLog.INFO);
    }

    public void startup(int mode, IResponseListener listener){
        req(Msg.Startup, new MsgBeans.StartupPara(), listener);
    }

    @Override
    protected Map<Msg, RspProcessor> rspProcessors() {
        Map<Msg, RspProcessor> rspProcessorMap = new HashMap<>();

        rspProcessorMap.put(Msg.Startup, this::processStartupResponses);

        return rspProcessorMap;
    }

    @Override
    protected Map<Msg, NtfProcessor> ntfProcessors() {
        return null;
    }

    private void processStartupResponses(Msg rspId, Object rspContent, IResponseListener listener){
        if (Msg.StartupRsp.equals(rspId)){
            if (null != listener){
                listener.onResponse(CommonResultCode.SUCCESS, null);
            }
        }
    }


}
