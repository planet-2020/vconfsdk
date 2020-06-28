package com.kedacom.vconf.sdk.amulet;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.kedacom.vconf.sdk.utils.log.KLog;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by gaofan_kd7331, 2018-10-25
 */

public class CrystalBall implements ICrystalBall {
    private static final String TAG = CrystalBall.class.getSimpleName();
    private static CrystalBall instance;

    private final Map<String, Method> cachedMethods = new ConcurrentHashMap<>();

    private final Set<IListener> rspListeners = new LinkedHashSet<>();
    private final Set<IListener> ntfListeners = new LinkedHashSet<>();

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
                for (IListener listener : rspListeners){
                    if (listener.onMsg(msgName, msgBody)){
                        return;
                    }
                }
                boolean consumed = false;
                for (IListener listener : ntfListeners) {
                    if (listener.onMsg(msgName, msgBody)) {
                        consumed = true;
                    }
                }
                if (!consumed) {
                    KLog.p(KLog.DEBUG, "<-x- %s, dropped, no consumer. \n%s", msgName, msgBody);
                }
            }
        };

    private static class MsgWrapper {
        String msgName;
        String msgBody;
        MsgWrapper(String msgName, String msgBody){this.msgName =msgName; this.msgBody=msgBody;}
    }

}
