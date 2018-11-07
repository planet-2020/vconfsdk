/**
 * Created by gaofan_kd7331, 2018-10-25
 */


package com.kedacom.vconf.sdk.base.basement;

public interface ICrystalBall {
    interface IYellback {
        void yellback(String msgName, String msgBody);
    }
    void setYellback(IYellback yb);
    int yell(String msgName, String para);  // request/processSet
    int yell(String msgName, StringBuffer output); // processGet
    int yell(String msgName, String para, StringBuffer output); // processGet

    default boolean eject(String ntfName) { //NOTE: 仅用于仿真模式
        return false;
    }

    default boolean eject(String[] ntfNames) { //NOTE: 仅用于仿真模式
        return false;
    }
}
