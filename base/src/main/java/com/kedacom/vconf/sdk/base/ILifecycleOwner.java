package com.kedacom.vconf.sdk.base;

import androidx.lifecycle.LifecycleOwner;

public interface ILifecycleOwner {
    /**获取生命周期组件。
     *
     * 若返回不为null，则框架将该监听器与返回的组件的生命周期绑定。
     * 绑定后该监听器将自动适配该组件的生命周期，当该组件销毁时，
     * 监听器将自动销毁不再上报请求结果。
     *
     * @return 生命周期组件*/
    default LifecycleOwner getLifecycleOwner(){return null;}
}
