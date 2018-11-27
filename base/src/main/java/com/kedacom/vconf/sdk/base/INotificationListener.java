package com.kedacom.vconf.sdk.base;

import androidx.lifecycle.LifecycleOwner;

public interface INotificationListener extends ILifecycleOwner{
    @Override
    default LifecycleOwner getLifecycleOwner(){return null;}

    void onNotification(Object notification);
}
