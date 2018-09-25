package com.sissi.vconfsdk.base;


import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;

import com.sissi.vconfsdk.utils.KLog;

class RequesterLifecycleObserver implements LifecycleObserver {

    private Object requester;
    private Callback cb;

    public RequesterLifecycleObserver(Object requester, Callback cb){
        this.requester = requester;
        this.cb = cb;
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    void onResumed() {
        KLog.p("--> resumed "+ requester);
        if (null != cb){
            cb.onRequesterResumed(requester);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    void onPause() {
        KLog.p("--> pause "+ requester);
        if (null != cb){
            cb.onRequesterPause(requester);
        }
    }

    interface Callback{
        void onRequesterResumed(Object requester);
        void onRequesterPause(Object requester);
    }

}
