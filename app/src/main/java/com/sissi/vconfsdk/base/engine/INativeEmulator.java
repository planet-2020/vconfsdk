package com.sissi.vconfsdk.base.engine;

/**
 * Created by Sissi on 2018/9/14.
 */

interface INativeEmulator {
    void setCallback(INativeCallback cb);
    int call(String methodName, String reqPara);  // request/set
    int call(String methodName, StringBuffer output); // get
    int call(String methodName, String para, StringBuffer output); // get
    void ejectNotification(String ntfId);
}
