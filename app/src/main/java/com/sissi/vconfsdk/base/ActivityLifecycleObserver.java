package com.sissi.vconfsdk.base;


import android.app.Activity;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;

import com.sissi.vconfsdk.utils.KLog;

public class ActivityLifecycleObserver implements LifecycleObserver {

    private Activity activity;

    public ActivityLifecycleObserver(Activity activity){
        this.activity = activity;
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void connectListener() {
        KLog.p("--> resume "+activity);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void disconnectListener() {
        KLog.p("--> pause "+activity);
    }
}
