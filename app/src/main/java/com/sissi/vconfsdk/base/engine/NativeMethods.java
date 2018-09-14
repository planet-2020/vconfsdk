package com.sissi.vconfsdk.base.engine;

/**
 * Created by Sissi on 1/20/2017.
 */
@SuppressWarnings({"JniMissingFunction", "unused"})
final class NativeMethods {

    private NativeMethods(){}

    static native int setCallback(Object callback);

    static native int invoke(String methodName, String reqPara);  // request/set
    static native int invoke(String methodName, StringBuffer output); // get
    static native int invoke(String methodName, String para, StringBuffer output); // get

}
