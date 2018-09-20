package com.sissi.vconfsdk.base.amulet;

/**
 * Created by Sissi on 2018/9/14.
 */

interface INotificationProcessor {
    boolean processNotification(String ntfName, String ntfBody);
}
