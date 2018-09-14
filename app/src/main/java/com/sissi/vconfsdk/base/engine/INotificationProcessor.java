package com.sissi.vconfsdk.base.engine;

/**
 * Created by Sissi on 2018/9/14.
 */

public interface INotificationProcessor {
    boolean process(String ntfName, Object ntfContent);
}
