package com.kedacom.vconf.sdk.base.basement;

import android.os.Handler;

interface ISubscribeProcessor {
    boolean subscribe(Handler subscriber, String ntfId);
    void unsubscribe(Handler subscriber, String ntfId);
}
