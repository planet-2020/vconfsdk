/**
 * Created by gaofan_kd7331, 2018-10-25
 */


package com.kedacom.vconf.sdk.base.basement;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.util.Log;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class FakeCrystalBall extends CrystalBall {

    private static final String TAG = FakeCrystalBall.class.getSimpleName();

    private static FakeCrystalBall instance;

    private final Map<Class<?>, Object> cfgCache = new HashMap<>();

    private JsonProcessor jsonProcessor;
    private MagicBook magicBook;

    private Handler nativeHandler;

    private FakeCrystalBall() {
        jsonProcessor = JsonProcessor.instance();
        magicBook = MagicBook.instance();
        HandlerThread handlerThread = new HandlerThread("FCB.native", Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
        nativeHandler = new Handler(handlerThread.getLooper());
    }

    public synchronized static FakeCrystalBall instance() {
        if (null == instance) {
            instance = new FakeCrystalBall();
        }
        return instance;
    }

    private Object createInstanceFromClass(Class<?> clz){
        Log.d(TAG, "clz="+clz);
        Object instance;
        try {
            if (clz.isArray()){
                Class elementType = clz.getComponentType();
                instance = Array.newInstance(elementType, 2);
            }else {
                Constructor ctor = clz.getDeclaredConstructor((Class[]) null); // 使用通知消息体类的默认构造函数构造通知消息对象
                ctor.setAccessible(true);
                instance = ctor.newInstance((Object[]) null);
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        } catch (InstantiationException e) {
            e.printStackTrace();
            return null;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }

        return instance;
    }


    @Override
    public int spell(String methodOwner, String methodName, Object[] para, Class[] paraType) {
        Log.d(TAG, String.format("receive REQ %s", methodName));
        String msgName = magicBook.getMsgName(methodName);
        if (magicBook.isSession(msgName)){
            String[][] rspSeqs = magicBook.getRspSeqs(msgName);
            if (null == rspSeqs || 0==rspSeqs.length){
                return 0;
            }
            String[] rspSeq = rspSeqs[0]; //NOTE: 仅返回第一路响应序列
            Object rspBody;
            int delay = 0;
            for (String rspName : rspSeq) {
                delay += magicBook.getRspDelay(rspName);

                rspBody = createInstanceFromClass(magicBook.getRspClazz(rspName));

                String jsonRspBody = jsonProcessor.toJson(rspBody);
                // 上报响应
                nativeHandler.postDelayed(() -> {
                    Log.d(TAG, String.format("send RSP %s, rspContent=%s", rspName, jsonRspBody));
                    onAppear(magicBook.getMsgId(rspName), jsonRspBody);
                }, delay);

            }
        }else if (magicBook.isSet(msgName)){
            Class<?>[] classes = magicBook.getUserParaClasses(msgName);
            Class<?> type = classes[classes.length-1]; // 最后一个为设置参数
            cfgCache.put(type, para[para.length-1]);
        }else if (magicBook.isGet(msgName)){
            Class<?>[] classes = magicBook.getUserParaClasses(msgName);
            Class<?> type = classes[classes.length-1];
            Object cfg = cfgCache.get(type);
            para[para.length-1] = cfg; // 最后一个参数为传出参数
        }

        return 0;
    }

    @Override
    public void emit(String msgId) {
        String msgName = magicBook.getMsgName(msgId);
        if (!magicBook.isResponse(msgName)
                && !magicBook.isNotification(msgName)){
            Log.e(TAG, String.format("emit msg failed, %s is not a rsp or ntf", msgName));
            return;
        }

        Object rspBody = createInstanceFromClass(magicBook.getRspClazz(msgName));
        String jsonRspBody = jsonProcessor.toJson(rspBody);
        int delay = magicBook.getRspDelay(msgName);

        // 上报响应
        nativeHandler.postDelayed(() -> {
            Log.d(TAG, String.format("send RSP %s, rspContent=%s", msgName, jsonRspBody));
            onAppear(msgId, jsonRspBody);
        }, delay);
    }

    @Override
    public void emit(String[] msgIds) {
        int delay = 0;
        for (String msgId : msgIds){
            String msgName = magicBook.getMsgName(msgId);
            if (!magicBook.isResponse(msgName)
                    && !magicBook.isNotification(msgName)){
                Log.e(TAG, String.format("emit msg failed, %s is not a rsp or ntf", msgName));
                continue;
            }

            Object rspBody = createInstanceFromClass(magicBook.getRspClazz(msgName));
            String jsonRspBody = jsonProcessor.toJson(rspBody);
            delay += magicBook.getRspDelay(msgName);

            // 上报响应
            nativeHandler.postDelayed(() -> {
                Log.d(TAG, String.format("send RSP %s, rspContent=%s", msgName, jsonRspBody));
                onAppear(msgId, jsonRspBody);
            }, delay);
        }
    }

}
