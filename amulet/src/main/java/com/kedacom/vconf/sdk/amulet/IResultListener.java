package com.kedacom.vconf.sdk.amulet;

import com.kedacom.vconf.sdk.utils.lifecycle.ILifecycleOwner;

/**
 * 请求结果监听器。
 * */
public interface IResultListener extends ILifecycleOwner {

    /**
     * 请求进度。
     * 对于过程较长的请求，可能需要体现请求进度。常见的如下载/上传。
     * NOTE: 并非所有请求均有请求进度，请看具体请求接口说明。
     * @param progressInfo 进度信息。具体定义在各自模块查阅
     * */
    default void onProgress(Object progressInfo){};

    /**结果已抵达。
     * 该接口返回后根据情况立即回调onSuccess/onFailed/onTimeout
     * 该接口主要用于做一些公共处理，比如不论成功失败均需消隐进度条，不论failed还是timeout均需做一些善后处理。
     * @param bSuccess 请求结果是否成功，若成功随后会回调onSuccess，否则回调onFailed/onTimeout。
     * */
    default void onArrive(boolean bSuccess){}

    /**成功。
     * @param result 请求对应的结果。具体定义在各自模块查阅*/
    default void onSuccess(Object result){}

    /**失败。
     * @param errorCode 错误码。具体定义在各自模块查阅
     * */
    default void onFailed(int errorCode){}

    /**超时*/
    default void onTimeout(){}
}
