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


    private native int call(String methodName, String reqPara);  // request/processSet
    private native int call(String methodName, StringBuffer output); // processGet
    private native int call(String methodName, String para, StringBuffer output); // processGet

    private native int setCallback(INativeCallback callback);

    @Override
    public void callback(String msgId, String msgBody) {
        if (null != yb){
            yb.yellback(msgId, msgBody);
        }
    }
}
