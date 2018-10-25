/**
 * Created by gaofan_kd7331, 2018-10-25
 */


package com.kedacom.vconf.sdk.base.amulet;

public interface IEchoWall {
    interface IYellback {
        void yellback(String msgId, String msgBody);
    }
    void setYellback(IYellback yb);
    int yell(String methodName, String reqPara);  // request/set
    int yell(String methodName, StringBuffer output); // get
    int yell(String methodName, String para, StringBuffer output); // get

    default boolean ejectNotification(String ntfId) {
        return false;
    }
}
