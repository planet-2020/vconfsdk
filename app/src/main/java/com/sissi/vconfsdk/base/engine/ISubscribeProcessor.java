package com.sissi.vconfsdk.base.engine;

import android.os.Handler;

public interface ISubscribeProcessor {
    boolean subscribe(Handler subscriber, String ntfId);
    void unsubscribe(Handler subscriber, String ntfId);
}
