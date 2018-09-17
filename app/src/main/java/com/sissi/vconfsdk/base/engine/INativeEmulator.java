package com.sissi.vconfsdk.base.engine;

/**
 * Created by Sissi on 2018/9/14.
 */

interface INativeEmulator {
    void setCallback(INativeCallback cb);
    int call(String methodName, String reqPara);
    void ejectNotification(String ntfId, Object ntfContent);
}
