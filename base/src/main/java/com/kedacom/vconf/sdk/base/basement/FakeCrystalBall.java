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
        HandlerThread handlerThread = new HandlerThread("FCB.yellback", Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }



    @Override
    public void setYellback(IYellback yb) {
        this.yb = yb;
    }

    @Override
    public int yell(String methodOwner, String msgName, Object... para) {
        if (null == yb){
            return -1;
        }

        Log.d(TAG, String.format("receive REQ %s, para=%s", msgName, para));
        String[][] rspSeqs = magicBook.getRspSeqs(msgName);
        if (null == rspSeqs || 0==rspSeqs.length){
            return 0;
        }
        String[] rspIds = magicBook.getRspSeqs(msgName)[0]; // 若有多路响应序列默认返回第一路
        Object rspBody;
        int delay = 0;
        for (String rspId : rspIds) {
            delay += magicBook.getRspDelay(rspId);

            rspBody = createInstanceFromClass(magicBook.getRspClazz(rspId));

            String jsonRspBody = jsonProcessor.toJson(rspBody);
            // 上报响应
            handler.postDelayed(() -> {
                Log.d(TAG, String.format("send RSP %s, rspContent=%s", rspId, jsonRspBody));
                yb.yellback(rspId, jsonRspBody);
            }, delay);

        }

        return 0;
    }
//
//    @Override
//    public int get(String msgName, StringBuffer output) {
//        return get(msgName, null, output);
//    }
//
//    @Override
//    public int get(String msgName, String para, StringBuffer output) {
//        Object result = createInstanceFromClass(magicBook.getGetResultClazz(msgName));
//
//        Log.d(TAG, String.format("GET %s, para= %s, result=%s", msgName, para, jsonProcessor.toJson(result)));
//
//        output.append(jsonProcessor.toJson(result));
//
//        return 0;
//    }
//
//    @Override
//    public int set(String msgName, String para){
//        Log.d(TAG, String.format("SET %s, para= %s", msgName, para));
//        return 0;
//    }

    @Override
    public boolean eject(String ntfName) {
        if (null == yb){
            return false;
        }

        Object ntfBody = createInstanceFromClass(magicBook.getNtfClazz(ntfName));

        final String jsonNtfBody = jsonProcessor.toJson(ntfBody);
        handler.postDelayed(() -> {
            Log.d(TAG, String.format("send NTF %s, content=%s", ntfName, jsonNtfBody));
            yb.yellback(ntfName, jsonNtfBody);
        }, magicBook.getNtfDelay(ntfName));

        return true;
    }

    @Override
    public boolean eject(String[] ntfNames) {
        if (null == yb){
            return false;
        }

        Object ntfBody;
        int delay = 0;
        for (String ntfName : ntfNames) {
            delay += magicBook.getNtfDelay(ntfName);
            ntfBody = createInstanceFromClass(magicBook.getNtfClazz(ntfName));
            final String jsonNtfBody = jsonProcessor.toJson(ntfBody);
            handler.postDelayed(() -> {
                Log.d(TAG, String.format("send NTF %s, content=%s", ntfName, jsonNtfBody));
                yb.yellback(ntfName, jsonNtfBody);
            }, delay);
        }

        return true;
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



}
