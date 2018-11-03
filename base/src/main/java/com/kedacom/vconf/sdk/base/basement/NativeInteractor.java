/**
 * Created by gaofan_kd7331, 2018-10-25
 */


package com.kedacom.vconf.sdk.base.basement;

@SuppressWarnings("JniMissingFunction")
class NativeInteractor implements ICrystalBall, INativeCallback{
    private static NativeInteractor instance;
    private IYellback yb;

    private NativeInteractor(){
        setCallback(this);
    }

    synchronized static NativeInteractor instance() {
        if (null == instance) {
            instance = new NativeInteractor();
        }
        return instance;
    }

    @Override
    public void setYellback(IYellback yb) {
        this.yb = yb;
    }

    @Override
    public int yell(String msgName, String para) {
        return call(msgName, para);
    }

    @Override
    public int yell(String msgName, StringBuffer output) {
        return call(msgName, output);
    }

    @Override
    public int yell(String msgName, String para, StringBuffer output) {
        return call(msgName, para, output);
    }


    private native int call(String msgName, String para);  // request/set
    private native int call(String msgName, StringBuffer output); // get
    private native int call(String msgName, String para, StringBuffer output); // get

    private native int setCallback(INativeCallback callback);

    @Override
    public void callback(String msgName, String msgBody) {
        if (null != yb){
            yb.yellback(msgName, msgBody);
        }
    }
}
