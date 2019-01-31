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

public class NativeInteractor2 implements ICrystalBall2{
    private static final String TAG = NativeInteractor2.class.getSimpleName();
    private static NativeInteractor2 instance;

    private final Map<String, Method> cachedMethods = new HashMap<>();

    private final List<IListener> listeners = new ArrayList<>();

    private NativeInteractor2(){
//        setCallback(this);
    }

    public synchronized static NativeInteractor2 instance() {
        if (null == instance) {
            instance = new NativeInteractor2();
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
    public void onAppear(String msgName, String msgContent) {
        if (null == msgName || msgName.isEmpty()){
            Log.w(TAG, "invalid msgId");
            return;
        }
        Message msg = Message.obtain();
        msg.obj = new MsgWrapper(msgName, msgContent);
        handler.sendMessage(msg);
    }

    /**
     * 添加消息监听器。
     * NOTE: 先添加的监听器优先消费消息。
     * */
    @Override
    public void addListener(IListener listener) {
        listeners.add(listener);
    }

    @Override
    public void delListener(IListener listener) {
        listeners.remove(listener);
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
                /*
                 * 消费该消息.
                 * 先添加的监听器优先消费，若消息被消费则不再传递给后续监听器。
                 * */
                boolean bConsumed = false;
                for (IListener listener : listeners){
                    if (bConsumed=listener.onMsg(msgName, msgBody)){
                        break;
                    }
                }
                if (!bConsumed){
                    Log.w(TAG, String.format("<-/- %s, unconsumed msg \n%s", msgName, msgBody));
                }
            }
        };

    private class MsgWrapper {
        String msgName;
        String msgBody;
        MsgWrapper(String msgName, String msgBody){this.msgName =msgName; this.msgBody=msgBody;}
    }

}
