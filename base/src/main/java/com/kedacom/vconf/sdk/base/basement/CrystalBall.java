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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CrystalBall implements ICrystalBall {
    private static final String TAG = CrystalBall.class.getSimpleName();
    private static CrystalBall instance;

    private final Map<String, Method> cachedMethods = new HashMap<>();

    private final List<IListener> rspListeners = new ArrayList<>();
    private final List<IListener> ntfListeners = new ArrayList<>();

    private CrystalBall(){
//        setCallback(this);
    }

    public synchronized static CrystalBall instance() {
        if (null == instance) {
            instance = new CrystalBall();
        }
        return instance;
    }


    @Override
    public int spell(String methodOwner, String methodName, Object[] para, Class[] paraType) {
        Log.d(TAG, "####=yell methodOwner="+methodOwner+" methodName="+methodName+" paras="+para);
        Method method = cachedMethods.get(methodName);
        if (null != method){
            try {
                method.invoke(null, para);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "####call cached method: "+method);
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

        Log.d(TAG, "####call method: "+method);

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

    /**
     * 添加消息监听器。
     * NOTE: 先添加的监听器优先消费消息。
     * */
    @Override
    public void addRspListener(IListener listener) {
        rspListeners.add(listener);
    }

    @Override
    public void addNtfListener(IListener listener) {
        ntfListeners.add(listener);
    }

    @Override
    public void delListener(IListener listener) {
        rspListeners.remove(listener);
        ntfListeners.remove(listener);
    }

    @Override
    public void clearListeners() {
        rspListeners.clear();
        ntfListeners.clear();
    }


    private Handler handler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                MsgWrapper msgWrapper = (MsgWrapper) msg.obj;
                String msgName = msgWrapper.msgName;
                String msgBody = msgWrapper.msgBody;
                /*
                 * 消费该消息.
                 * 响应监听器优先消费，若消息被消费则不再传递给后续监听器。
                 * */
                for (IListener listener : rspListeners){
                    if (listener.onMsg(msgName, msgBody)){
                        return;
                    }
                }
                for (IListener listener : ntfListeners){
                    listener.onMsg(msgName, msgBody);
                }
            }
        };

    private class MsgWrapper {
        String msgName;
        String msgBody;
        MsgWrapper(String msgName, String msgBody){this.msgName =msgName; this.msgBody=msgBody;}
    }

}
