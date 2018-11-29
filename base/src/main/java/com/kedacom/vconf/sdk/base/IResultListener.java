package com.kedacom.vconf.sdk.base;

/**
 * 请求结果监听器。
 * */
public interface IResultListener extends ILifecycleOwner{

    /**结果已抵达，后续会根据情况回调onSuccess/onFailed/onTimeout
     * 此接口主要用于在收到结果后做一些公共处理，比如消隐进度条。
     * */
    default void onResultArrived(){}

    /**请求成功。
     * @param result 请求对应的结果。具体定义在各自模块查阅*/
    default void onSuccess(Object result){}

    /**请求失败。
     * @param errorCode 错误码。具体定义在各自模块查阅
     * @param errorInfo 错误信息。具体定义在各自模块查阅*/
    default void onFailed(int errorCode, Object errorInfo){}

    /**请求超时*/
    default void onTimeout(){}
}
