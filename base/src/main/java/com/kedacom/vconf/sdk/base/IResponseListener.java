package com.kedacom.vconf.sdk.base;

public interface IResponseListener {
    void onSuccess(Object result);
    void onFailed(int errorCode);
    void onTimeout();
}
