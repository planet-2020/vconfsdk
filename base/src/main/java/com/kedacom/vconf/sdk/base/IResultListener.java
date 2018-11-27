package com.kedacom.vconf.sdk.base;

import androidx.lifecycle.LifecycleOwner;

/**
 * 请求结果监听器。
 * */
public interface IResultListener {
    /**获取监听器绑定的生命周期组件。
     *
     * 若返回不为null，则框架将该监听器与返回的组件的生命周期绑定。
     * 绑定后该监听器将自动适配该组件的生命周期，当该组件销毁时，
     * 监听器将自动销毁不再上报请求结果（即不再回调onSuccess/onFailed/onTimeout）。
     * 这样使用者不用再手动管理监听器的生命周期。
     *
     * @return 生命周期组件*/
    default LifecycleOwner getLifecycleOwner(){return null;}

    /**结果已抵达，后续会根据情况回调onSuccess/onFailed/onTimeout
     * 此接口主要用于在收到结果后做一些公共处理，比如消隐进度条。
     * */
    default void onResultArrived(){}

    /**请求成功。
     * @param result 请求对应的结果。具体定义在各自模块查阅*/
    void onSuccess(Object result);

    /**请求失败。
     * @param errorCode 错误码。具体定义在各自模块查阅*/
    void onFailed(int errorCode);

    /**请求超时*/
    void onTimeout();
}
