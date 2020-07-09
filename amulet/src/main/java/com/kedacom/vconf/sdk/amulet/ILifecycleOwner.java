package com.kedacom.vconf.sdk.amulet;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

public interface ILifecycleOwner {
    /**
     * 指定要绑定的生命周期对象
     * @return 要绑定的生命周期对象。
     * */
    default LifecycleOwner getLifecycleOwner(){
        return null;
    }

    /**
     * 指定销毁的时机。
     * 默认是LifecycleOwner#onDestroy时。
     * */
    default Lifecycle.Event destroyWhenLifecycleOwner(){
        return Lifecycle.Event.ON_DESTROY;
    }
}
