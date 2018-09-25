package com.sissi.vconfsdk.startup;

import com.sissi.vconfsdk.base.Msg;
import com.sissi.vconfsdk.base.MsgBeans;
import com.sissi.vconfsdk.base.RequestAgent;

/**
 * Created by Sissi on 2018/9/12.
 */

public class StartManager extends RequestAgent {

    public void startup(int mode, OnStartupResultListener listener){
        req(Msg.StartupReq, new MsgBeans.StartupInfo(), listener);
    }

    @Override
    protected void onRsp(Msg rspId, Object rspContent, Object listener) {
        if (Msg.StartupRsp.equals(rspId)){
            if (null != listener){
                ((OnStartupResultListener)listener).onStartupSuccess();
            }
        }
    }

    @Override
    protected void onTimeout(Msg reqId, Object listener) {

    }

    public interface OnStartupResultListener{
        void onStartupSuccess();
        void onStartupFailed(int errCode);
    }
}
