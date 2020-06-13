package com.kedacom.vconf.sdk.amulet;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import com.kedacom.vconf.sdk.utils.log.KLog;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by gaofan_kd7331, 2018-10-25
 */

public class CrystalBall implements ICrystalBall {
    private static final String TAG = CrystalBall.class.getSimpleName();
    private static CrystalBall instance;

    private final Map<String, Method> cachedMethods = new ConcurrentHashMap<>();

    private final List<PriorityListener> listeners = new ArrayList<>();

    protected CrystalBall(){
    }

    public synchronized static CrystalBall instance() {
        if (null == instance) {
            instance = new CrystalBall();
        }
        return instance;
    }


    @Override
    public int spell(String methodOwner, String methodName, Object[] para, Class[] paraType) {
        Method method = cachedMethods.get(methodName);
        if (null != method){
            try {
                KLog.p(KLog.DEBUG, "try invoke method %s", method);
                method.invoke(null, para);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
            return 0;
        }

        try {
            Class<?> clz = Class.forName(methodOwner);
            method = clz.getDeclaredMethod(methodName, paraType);
            method.setAccessible(true);
            KLog.p(KLog.DEBUG, "try invoke method %s", method);
            method.invoke(null, para);
            cachedMethods.put(methodName, method);
        } catch (NoSuchMethodException | ClassNotFoundException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return 0;
    }

    @Override
    public void onAppear(String msgId, String msgContent) {
        if (null == msgId || msgId.isEmpty()){
            KLog.p(KLog.ERROR, "invalid msgId");
            return;
        }
        Message msg = Message.obtain();
        msg.obj = new MsgWrapper(msgId, msgContent);
        handler.sendMessage(msg);
    }


    @Override
    public void addListener(IListener listener, int priority) {
        if (priority<0){
            KLog.p(KLog.ERROR, "priority(%s) can not be < 0", priority);
            return;
        }
        for (PriorityListener priorityListener : listeners){
            if (listener == priorityListener.listener){
                priorityListener.priority = priority;
                Collections.sort(listeners);
                return;
            }
        }

        listeners.add(new PriorityListener(listener, priority));
        Collections.sort(listeners);
    }

    @Override
    public void delListener(IListener listener) {
        for (PriorityListener priorityListener : listeners){
            if (listener == priorityListener.listener){
                listeners.remove(priorityListener);
                return;
            }
        }
    }

    @Override
    public void clearListeners() {
        listeners.clear();
    }

    @Override
    public int getPriority(IListener listener) {
        for (PriorityListener priorityListener : listeners){
            if (listener == priorityListener.listener){
                return priorityListener.priority;
            }
        }
        return -1;
    }


    private Handler handler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                MsgWrapper msgWrapper = (MsgWrapper) msg.obj;
                String msgName = msgWrapper.msgName;
                String msgBody = msgWrapper.msgBody;
                for (PriorityListener priorityListener : listeners){
                    if (priorityListener.listener.onMsg(msgName, msgBody)){
                        return;
                    }
                }
                KLog.p(KLog.DEBUG, "<-x- %s, dropped, no consumer. \n%s", msgName, msgBody);
            }
        };

    private class MsgWrapper {
        String msgName;
        String msgBody;
        MsgWrapper(String msgName, String msgBody){this.msgName =msgName; this.msgBody=msgBody;}
    }

    private class PriorityListener implements Comparable<PriorityListener>{
        IListener listener;
        int priority;

        PriorityListener(IListener listener, int priority) {
            this.listener = listener;
            this.priority = priority;
        }

        @Override
        public int compareTo(@NonNull PriorityListener o) {
            return priority - o.priority;
        }
    }

}
