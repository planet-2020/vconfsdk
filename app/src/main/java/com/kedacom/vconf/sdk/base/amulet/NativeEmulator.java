package com.kedacom.vconf.sdk.base.amulet;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


/**
 * 模拟器。<p>
 *
 * 模拟模式下模拟器替代了真实的远端(服务器)，请求会被定向到模拟器而非发给真实服务器.
 * 模拟器收到请求后会反馈响应。<p>
 * 模拟模式主要有两个用途：<p>
 * 1、便于在Native层没有完成开发的情况下UI层开发仍可以照常进行不受制约。<p>
 * 2、便于定位问题。比如当联调出现问题时可启用模拟模式跑下程序，若模拟模式下程序正常则问题出在native层，否则问题出在UI层。
 *
 * Created by Sissi on 1/20/2017.
 */
final class NativeEmulator implements INativeEmulator{

    private static final String TAG = "NativeEmulator";

    private static NativeEmulator instance;

    private JsonProcessor jsonProcessor;
    private MessageRegister messageRegister;

    private Handler handler;
    private INativeCallback cb;

    private NativeEmulator() {
        jsonProcessor = JsonProcessor.instance();
        messageRegister = MessageRegister.instance();
        initHandler();
    }

    synchronized static NativeEmulator instance() {
        if (null == instance) {
            instance = new NativeEmulator();
        }

        return instance;
    }

    private void initHandler(){
        HandlerThread handlerThread = new HandlerThread("NE.callback", Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }


    @Override
    public void setCallback(INativeCallback cb) {
        this.cb = cb;
    }


    @Override
    public int call(String methodName, String para) {
        if (messageRegister.isSet(methodName)){
            set(methodName, para);
            return 0;
        }


        if (null == cb){
            return -1;
        }

        Log.d(TAG, String.format("receive REQ %s, para= %s", methodName, para));

        String[] rspIds = messageRegister.getRspSeqs(methodName)[0]; // 若有多路响应序列默认返回第一路
        Object rspBody = null;
        int delay = 0;
        for (String rspId : rspIds) {
            delay += messageRegister.getRspDelay(rspId);
            try {
                Class<?> clz = messageRegister.getRspClazz(rspId);
                Constructor ctor = clz.getDeclaredConstructor((Class[]) null); // 使用响应消息体类的默认构造函数构造响应消息对象
                ctor.setAccessible(true);
                rspBody = ctor.newInstance((Object[]) null);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
            }
            String jsonRspBody = jsonProcessor.toJson(rspBody);
            // 上报响应
            handler.postDelayed(() -> {
                Log.d(TAG, String.format("send RSP %s, rspContent=%s", rspId, jsonRspBody));
                cb.callback(rspId, jsonRspBody);
            }, delay);

        }

        return 0;
    }

    @Override
    public int call(String methodName, StringBuffer output) {
        return call(methodName, null, output);
    }

    @Override
    public int call(String methodName, String para, StringBuffer output) {
        Object result = null;
        try {
            Class<?> clz = messageRegister.getGetResultClazz(methodName);
            Constructor ctor = clz.getDeclaredConstructor((Class[])null);
            ctor.setAccessible(true);
            result = ctor.newInstance((Object[]) null);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }

        Log.d(TAG, String.format("GET %s, para= %s, result=%s", methodName, para, jsonProcessor.toJson(result)));

        output.append(jsonProcessor.toJson(result));

        return 0;
    }


    @Override
    public void ejectNotification(String ntfId) {
        if (null == cb){
            return;
        }
        Object ntfBody = null;
        try {
            Class<?> clz = messageRegister.getNtfClazz(ntfId);
            Constructor ctor = clz.getDeclaredConstructor((Class[])null); // 使用通知消息体类的默认构造函数构造通知消息对象
            ctor.setAccessible(true);
            ntfBody = ctor.newInstance((Object[]) null);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        final String jsonNtfBody = jsonProcessor.toJson(ntfBody);
        handler.postDelayed(() -> {
            Log.d(TAG, String.format("send NTF %s, content=%s", ntfId, jsonNtfBody));
            cb.callback(ntfId, jsonNtfBody);
        }, messageRegister.getNtfDelay(ntfId));
    }


    private void set(String methodName, String para){
        Log.d(TAG, String.format("SET %s, para= %s", methodName, para));
    }

}
