package com.sissi.vconfsdk.base;


import android.arch.lifecycle.DefaultLifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.support.annotation.NonNull;

import com.sissi.vconfsdk.utils.KLog;

class RequesterLifecycleObserver implements DefaultLifecycleObserver {

    private Callback cb;

    RequesterLifecycleObserver(Callback cb){
        this.cb = cb;
    }

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {

    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {

    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        KLog.p(""+ owner);
        if (null != cb){
            cb.onRequesterResumed(owner);
        }
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        KLog.p(""+ owner);
        if (null != cb){
            cb.onRequesterPause(owner);
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {

    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {

    }

    interface Callback{
        void onRequesterResumed(Object requester);
        void onRequesterPause(Object requester);
    }

}
