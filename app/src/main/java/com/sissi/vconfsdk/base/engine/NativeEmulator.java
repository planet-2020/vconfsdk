package com.sissi.vconfsdk.base.engine;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by Sissi on 1/20/2017.
 * */
/**
 * 模拟器。<p>
 *
 * 模拟模式下模拟器替代了真实的远端(服务器)，请求会被定向到模拟器而非发给真实服务器.
 * 模拟器收到请求后会反馈响应。<p>
 * 模拟模式主要有两个用途：<p>
 * 1、便于在Native层没有完成开发的情况下UI层开发仍可以照常进行不受制约。<p>
 * 2、便于定位问题。比如当联调出现问题时可启用模拟模式跑下程序，若模拟模式下程序正常则问题出在native层，否则问题出在UI层。
 *
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
        final Object lock = new Object();
        Thread thread = new Thread() {
            @SuppressLint("HandlerLeak")
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                Looper.prepare();
                handler = new Handler();
                synchronized (lock) {
                    lock.notify();
                }
                Looper.loop();
            }
        };

        thread.setName("NE.callback");

        thread.start();

        if (null == handler){
            synchronized (lock) {
                try {
                    lock.wait(); // 保证初始化结束后立即可用。
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void setCallback(INativeCallback cb) {
        this.cb = cb;
    }


    @Override
    public int call(String methodName, String para) {
        if (null == cb){
            return -1;
        }

        String reqId = methodName;
        String reqPara = para;

        Log.i(TAG, String.format("receive REQ %s, para= %s", reqId, reqPara));

        String[] rspIds = messageRegister.getRspSeqs(reqId)[0];
        Object rspBody = null;
        for (int i=0; i<rspIds.length; ++i) {
            // 构造响应json字符串
            final String rspId = rspIds[i];
            try {
                Class<?> clz = messageRegister.getRspClazz(rspId);
                Constructor ctor = clz.getDeclaredConstructor((Class[])null); // 使用响应消息体类的默认构造函数构造响应消息对象
                ctor.setAccessible(true);
                rspBody = ctor.newInstance((Object[]) null);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
            }
            final String jsonRspBody = jsonProcessor.toJson(rspBody);
            // 上报响应
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, String.format("send RSP %s, rspContent=%s", rspId, jsonRspBody));
                    cb.callback(rspId, jsonRspBody);
                }
            }, 100);

        }

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
        final String finalNtfId = ntfId;
        final String jsonNtfBody = jsonProcessor.toJson(ntfBody);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, String.format("send NTF %s, content=%s", finalNtfId, jsonNtfBody));
                cb.callback(finalNtfId, jsonNtfBody);
            }
        }, 100);
    }

}
