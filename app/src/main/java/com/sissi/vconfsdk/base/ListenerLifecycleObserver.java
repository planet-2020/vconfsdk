package com.sissi.vconfsdk.base;


import android.arch.lifecycle.DefaultLifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.support.annotation.NonNull;

import com.sissi.vconfsdk.utils.KLog;

class ListenerLifecycleObserver implements DefaultLifecycleObserver {

    private Callback cb;

    ListenerLifecycleObserver(Callback cb){
        this.cb = cb;
    }

    boolean tryObserve(Object listener){
        KLog.p("listener instanceof LifecycleOwner? %s", listener instanceof LifecycleOwner);
        if (listener instanceof LifecycleOwner){
            ((LifecycleOwner)listener).getLifecycle().addObserver(this);
            return true;
        }
        return false;
    }

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {

    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {

    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        if (null != cb){
            cb.onListenerResumed(owner);
        }
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        if (null != cb){
            cb.onListenerPause(owner);
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (null != cb){
            cb.onListenerStop(owner);
        }
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {

    }

    interface Callback{
        void onListenerResumed(Object listener);
        void onListenerPause(Object listener);
        void onListenerStop(Object listener);
    }

}
