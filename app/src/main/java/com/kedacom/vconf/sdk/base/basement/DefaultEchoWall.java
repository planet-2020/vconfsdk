/**
 * Created by gaofan_kd7331, 2018-10-25
 */


package com.kedacom.vconf.sdk.base.basement;

@SuppressWarnings("JniMissingFunction")
class DefaultEchoWall implements IEchoWall {
    private static DefaultEchoWall instance;

    synchronized static DefaultEchoWall instance() {
        if (null == instance) {
            instance = new DefaultEchoWall();
        }
        return instance;
    }

    @Override
    public void setYellback(IYellback yb) {
        setCallback(yb);
    }

    @Override
    public int yell(String methodName, String reqPara) {
        return call(methodName, reqPara);
    }

    @Override
    public int yell(String methodName, StringBuffer output) {
        return call(methodName, output);
    }

    @Override
    public int yell(String methodName, String para, StringBuffer output) {
        return call(methodName, para, output);
    }


    private native int setCallback(IYellback callback);

    private native int call(String methodName, String reqPara);  // request/set
    private native int call(String methodName, StringBuffer output); // get
    private native int call(String methodName, String para, StringBuffer output); // get

}
