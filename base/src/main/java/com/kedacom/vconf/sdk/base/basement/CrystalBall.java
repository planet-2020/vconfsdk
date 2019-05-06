/**
 * Created by gaofan_kd7331, 2018-10-25
 */


package com.kedacom.vconf.sdk.base.basement;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
//        Log.d(TAG, "####=yell methodOwner="+methodOwner+" methodName="+methodName+" paras="+para);
        Method method = cachedMethods.get(methodName);
        if (null != method){
            try {
                method.invoke(null, para);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
//            Log.d(TAG, "####call cached method: "+method);
            return 0;
        }

        try {
            Class<?> clz = Class.forName(methodOwner);
            method = clz.getDeclaredMethod(methodName, paraType);
            method.invoke(null, para);
            cachedMethods.put(methodName, method);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

//        Log.d(TAG, "####call method: "+method);

        return 0;
    }

    @Override
    public void onAppear(String msgId, String msgContent) {
        if (null == msgId || msgId.isEmpty()){
            Log.w(TAG, "invalid msgId");
            return;
        }
        Message msg = Message.obtain();
        msg.obj = new MsgWrapper(msgId, msgContent);
        handler.sendMessage(msg);
    }


    @Override
    public void addListener(IListener listener, int priority) {
        for (PriorityListener priorityListener : listeners){
            if (listener == priorityListener){
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
            if (listener == priorityListener){
                listeners.remove(priorityListener);
                return;
            }
        }
    }

    @Override
    public void clearListeners() {
        listeners.clear();
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
        public int compareTo(PriorityListener o) {
            return priority - o.priority;
        }
    }

}
