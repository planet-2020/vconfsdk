/**
 * Created by gaofan_kd7331, 2018-10-25
 */


package com.kedacom.vconf.sdk.base.basement;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class FakeCrystalBall implements ICrystalBall {

    private static final String TAG = "FakeCrystalBall";

    private static FakeCrystalBall instance;

    private final List<IListener> rspListeners = new ArrayList<>();
    private final List<IListener> ntfListeners = new ArrayList<>();

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

//    @Override
//    public boolean eject(String ntfName) {
//        if (null == yb){
//            return false;
//        }
//
//        Object ntfBody = createInstanceFromClass(magicBook.getNtfClazz(ntfName));
//
//        final String jsonNtfBody = jsonProcessor.toJson(ntfBody);
//        nativeHandler.postDelayed(() -> {
//            Log.d(TAG, String.format("send NTF %s, content=%s", ntfName, jsonNtfBody));
//            yb.yellback(ntfName, jsonNtfBody);
//        }, magicBook.getNtfDelay(ntfName));
//
//        return true;
//    }


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
            String[] rspSeq = rspSeqs[0]; // 若有多路响应序列默认返回第一路
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

        }else if (magicBook.isGet(msgName)){

        }

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
             * 先添加的监听器优先消费，若消息被消费则不再传递给后续监听器。
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
