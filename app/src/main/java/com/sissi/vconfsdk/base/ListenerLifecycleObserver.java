package com.sissi.vconfsdk.base;


import android.arch.lifecycle.DefaultLifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.support.annotation.NonNull;

import com.sissi.vconfsdk.utils.KLog;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class ListenerLifecycleObserver implements DefaultLifecycleObserver {

    private Callback cb;
    private Map<LifecycleOwner, Set<Object>> ownerEnclosedListeners;

    ListenerLifecycleObserver(Callback cb){
        this.cb = cb;
        ownerEnclosedListeners = new HashMap<>();
    }

    boolean tryObserve(Object listener){
        KLog.p("%s instanceof LifecycleOwner? %s", listener, listener instanceof LifecycleOwner);
        if (listener instanceof LifecycleOwner){
            // listener本身即为生命周期拥有者，则直接监控其生命周期
            ((LifecycleOwner)listener).getLifecycle().addObserver(this);
            return true;
        }

        /* listener本身不拥有生命周期则尝试监控其外部类的生命周期（若其外部类为生命周期拥有者）。
         注意：不支持多级嵌套。即仅检查listener的直接外部类，不再往上递归。*/
        Object encloser = getEncloser(listener);
        KLog.p("%s instanceof LifecycleOwner? %s", encloser, encloser instanceof LifecycleOwner);
        if (encloser instanceof LifecycleOwner){
            LifecycleOwner owner = (LifecycleOwner) encloser;
            owner.getLifecycle().addObserver(this);
            Set<Object> objSet = ownerEnclosedListeners.get(owner);
            if (null == objSet){
                objSet = new HashSet<>();
                ownerEnclosedListeners.put(owner, objSet);
            }
            objSet.add(listener);

            return true;
        }

        return false;
    }

    private Object getEncloser(Object obj){
        Class<?> clz = obj.getClass();
        Class<?> enclosingClz = clz.getEnclosingClass();
        Object encloser = null;
        Field enclosingClzRef;
        try {
            //for debug
//            Field[] fields = clz.getDeclaredFields();
//            for (Field field : fields){
//                KLog.p("======= field: %s", field);
//            }
            if (null != enclosingClz) { // 内部类
                enclosingClzRef = clz.getDeclaredField("this$0"); // 外部类在内部类中的引用名称为"this$0"
            }else { // lambda
                /*lambda可能持有外部类引用也可能未持有。
                若lambda内部引用了外部类或外部类的成员则持有否则未持有。
                持有的情况下外部类在lambda类中的引用名称为"arg$1"*/
                enclosingClzRef = clz.getDeclaredField("arg$1");
            }
        } catch (NoSuchFieldException e) {
            return null;
        }

        enclosingClzRef.setAccessible(true);

        try {
            encloser = enclosingClzRef.get(obj);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return encloser;
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
            notify(owner, cb::onListenerResumed);
        }
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        if (null != cb){
            notify(owner, cb::onListenerPause);
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (null != cb){
            notify(owner, cb::onListenerStop);
        }
        ownerEnclosedListeners.remove(owner);
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
    }

    private void notify(LifecycleOwner owner, Action action){
        action.act(owner);

        if (ownerEnclosedListeners.keySet().contains(owner)){
            Set<Object> objects = ownerEnclosedListeners.get(owner);
            for (Object obj : objects){
                action.act(obj);
            }
        }
    }

    private interface Action {
        void act(Object listener);
    }

    interface Callback{
        void onListenerResumed(Object listener);
        void onListenerPause(Object listener);
        void onListenerStop(Object listener);
    }

}
