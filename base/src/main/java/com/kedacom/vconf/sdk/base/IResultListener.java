package com.kedacom.vconf.sdk.base;

/**
 * 请求结果监听器。
 * */
public interface IResultListener extends ILifecycleOwner{
    /**结果已抵达，该接口返回后根据情况立即回调onSuccess/onFailed/onTimeout
     * 该接口主要用于做一些公共处理，比如消隐进度条，而不关心结果是成功还是失败。
     * */
    default void onArrive(){}

    /**请求成功。
     * @param result 请求对应的结果。具体定义在各自模块查阅*/
    default void onSuccess(Object result){}

    /**请求失败。
     * @param errorCode 错误码。具体定义在各自模块查阅
     * */
    default void onFailed(int errorCode){}

    /**请求超时*/
    default void onTimeout(){}
}
