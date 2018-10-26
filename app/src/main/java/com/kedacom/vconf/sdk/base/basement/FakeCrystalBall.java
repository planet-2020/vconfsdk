/**
 * Created by gaofan_kd7331, 2018-10-25
 */


package com.kedacom.vconf.sdk.base.basement;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

class FakeCrystalBall implements ICrystalBall {

    private static final String TAG = "FakeCrystalBall";

    private static FakeCrystalBall instance;

    private JsonProcessor jsonProcessor;
    private MagicBook magicBook;

    private Handler handler;
    private IYellback yb;

    private FakeCrystalBall() {
        jsonProcessor = JsonProcessor.instance();
        magicBook = MagicBook.instance();
        initHandler();
    }

    synchronized static FakeCrystalBall instance() {
        if (null == instance) {
            instance = new FakeCrystalBall();
        }
        return instance;
    }

    private void initHandler(){
        HandlerThread handlerThread = new HandlerThread("FEW.yellback", Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }



    @Override
    public void setYellback(IYellback yb) {
        this.yb = yb;
    }

    @Override
    public int yell(String methodName, String para) {
        if (magicBook.isSet(methodName)){
            set(methodName, para);
            return 0;
        }

        if (null == yb){
            return -1;
        }

        Log.d(TAG, String.format("receive REQ %s, para=%s", methodName, para));

        String[] rspIds = magicBook.getRspSeqs(methodName)[0]; // 若有多路响应序列默认返回第一路
        Object rspBody = null;
        int delay = 0;
        for (String rspId : rspIds) {
            delay += magicBook.getRspDelay(rspId);
            try {
                Class<?> clz = magicBook.getRspClazz(rspId);
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
                yb.yellback(rspId, jsonRspBody);
            }, delay);

        }

        return 0;
    }

    @Override
    public int yell(String methodName, StringBuffer output) {
        return yell(methodName, null, output);
    }

    @Override
    public int yell(String methodName, String para, StringBuffer output) {
        Object result = null;
        try {
            Class<?> clz = magicBook.getGetResultClazz(methodName);
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
    public boolean ejectNotification(String ntfId) {
        if (null == yb){
            return false;
        }

        Object ntfBody = null;
        try {
            Class<?> clz = magicBook.getNtfClazz(ntfId);
            Constructor ctor = clz.getDeclaredConstructor((Class[])null); // 使用通知消息体类的默认构造函数构造通知消息对象
            ctor.setAccessible(true);
            ntfBody = ctor.newInstance((Object[]) null);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        final String jsonNtfBody = jsonProcessor.toJson(ntfBody);
        handler.postDelayed(() -> {
            Log.d(TAG, String.format("send NTF %s, content=%s", ntfId, jsonNtfBody));
            yb.yellback(ntfId, jsonNtfBody);
        }, magicBook.getNtfDelay(ntfId));

        return true;
    }

    private void set(String methodName, String para){
        Log.d(TAG, String.format("SET %s, para= %s", methodName, para));
    }

}
