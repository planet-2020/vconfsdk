package com.kedacom.vconf.sdk.base;

/**
 * 请求结果监听器。
 * */
public interface IResultListener extends ILifecycleOwner{

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
