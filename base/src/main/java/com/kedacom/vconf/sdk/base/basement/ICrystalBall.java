/**
 * Created by gaofan_kd7331, 2018-10-25
 */


package com.kedacom.vconf.sdk.base.basement;

public interface ICrystalBall {
    interface IYellback {
        void yellback(String msgName, String msgBody);
    }
    void setYellback(IYellback yb);
//    int request(String msgName, Object... para);
//    int set(String msgName, String para);
//    int get(String msgName, StringBuffer output);
//    int get(String msgName, String para, StringBuffer output);
    int yell(String methodOwner, String methodName, Object[] para, Class[] paraType);

    default boolean eject(String ntfName) { //NOTE: 仅用于仿真模式
        return false;
    }

    default boolean eject(String[] ntfNames) { //NOTE: 仅用于仿真模式
        return false;
    }
}
