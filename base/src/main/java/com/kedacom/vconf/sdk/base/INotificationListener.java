package com.kedacom.vconf.sdk.base;


public interface INotificationListener extends ILifecycleOwner{
    void onNotification(Object notification);
}
