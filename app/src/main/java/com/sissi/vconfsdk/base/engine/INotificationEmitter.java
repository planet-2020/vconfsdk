package com.sissi.vconfsdk.base.engine;

public interface INotificationEmitter {
    boolean emitNotification(String ntfName, Object ntfContent);
}
