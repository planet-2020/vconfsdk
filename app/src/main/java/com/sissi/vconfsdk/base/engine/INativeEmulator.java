package com.sissi.vconfsdk.base.engine;

/**
 * Created by Sissi on 2018/9/14.
 */

public interface INativeEmulator {
    void setCallback(INativeCallback cb);
    void ejectNotification(String ntfId, final Object ntfContent);
    int invoke(String methodName, String reqPara);
}
