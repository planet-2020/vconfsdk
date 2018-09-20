package com.sissi.vconfsdk.base.amulet;

import android.os.Handler;

public interface ISubscribeProcessor {
    boolean subscribe(Handler subscriber, String ntfId);
    void unsubscribe(Handler subscriber, String ntfId);
}
