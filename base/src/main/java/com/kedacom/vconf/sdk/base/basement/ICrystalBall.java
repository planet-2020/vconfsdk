/**
 * Created by gaofan_kd7331, 2018-10-25
 */


package com.kedacom.vconf.sdk.base.basement;

public interface ICrystalBall {
    interface IYellback {
        void yellback(String msgId, String msgBody);
    }
    void setYellback(IYellback yb);
    int yell(String methodName, String reqPara);  // request/processSet
    int yell(String methodName, StringBuffer output); // processGet
    int yell(String methodName, String para, StringBuffer output); // processGet

    default boolean ejectNotification(String ntfId) {
        return false;
    }
}
