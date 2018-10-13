package com.sissi.vconfsdk.base.amulet;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;


/**
 * Created by Sissi on 1/20/2017.
 */
@SuppressWarnings({"JniMissingFunction", "unused"})
final class NativeInteractor implements INativeCallback{

    private static final String TAG = NativeInteractor.class.getSimpleName();

    private static NativeInteractor instance;

    private Handler nativeMsgHandler;

    private IResponseProcessor responseProcessor;
    private INotificationProcessor notificationProcessor;
    private INativeEmulator nativeEmulator;

    private NativeInteractor(){
        initNativeMsgHandler();
    }

    public synchronized static NativeInteractor instance() {
        if (null == instance) {
            instance = new NativeInteractor();
//            setCallback(instance);
        }

        return instance;
    }

    int request(String methodName, String reqPara){
        if (null != nativeEmulator){
            return nativeEmulator.call(methodName, reqPara);
        }

        return call(methodName, reqPara);
    }

    int set(String methodName, String setPara){
        if (null != nativeEmulator){
            return nativeEmulator.call(methodName, setPara);
        }
        return call(methodName, setPara);
    }

    int get(String methodName, String para, StringBuffer output){
        if (null != nativeEmulator){
            return nativeEmulator.call(methodName, para, output);
        }
        return call(methodName, para, output);
    }

    int get(String methodName, StringBuffer output){
        if (null != nativeEmulator){
            return nativeEmulator.call(methodName, output);
        }
        return call(methodName, output);
    }

    /**
     * 发射通知。驱动模拟器发射通知，仅用于模拟模式。
     * */
    boolean emitNotification(String ntfId){
        if (null == nativeEmulator){
            return false;
        }
        nativeEmulator.ejectNotification(ntfId);
        return true;
    }


    @Override
    public void callback(String msgId, String msgBody){
        if (null == msgId || msgId.isEmpty()){
            Log.e(TAG, "Invalid native msg.");
            return;
        }
        Message msg = Message.obtain();
        msg.obj = new NativeMsgWrapper(msgId, msgBody);
        nativeMsgHandler.sendMessage(msg);
    }


    /**
     * 初始化native消息处理线程
     * */
    private void initNativeMsgHandler(){
        final Object lock = new Object();
        Thread thread = new Thread() {
            @SuppressLint("HandlerLeak")
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                Looper.prepare();

                nativeMsgHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        NativeMsgWrapper nativeMsgWrapper = (NativeMsgWrapper) msg.obj;
                        String msgId = nativeMsgWrapper.msgId;
                        String msgBody = nativeMsgWrapper.msgBody;
                        if (null!=responseProcessor){
                            responseProcessor.processResponse(msgId, msgBody);
                        }
                        if (null!=notificationProcessor){ //XXX 需不需要采用消费模式？但是如果一条消息既可以是通知也可以是响应呢？
                            notificationProcessor.processNotification(msgId, msgBody);
                        }
                    }
                };
                synchronized (lock) {
                    lock.notify();
                }

                Looper.loop();
            }
        };

        thread.setName("NI.nativeMsgHandler");

        thread.start();

        if (null == nativeMsgHandler){
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    NativeInteractor setResponseProcessor(IResponseProcessor responseProcessor){
        this.responseProcessor = responseProcessor;
        return this;
    }

    NativeInteractor setNotificationProcessor(INotificationProcessor notificationProcessor){
        this.notificationProcessor = notificationProcessor;
        return this;
    }

    NativeInteractor setNativeEmulator(INativeEmulator nativeEmulator){
        this.nativeEmulator = nativeEmulator;
        if (null != nativeEmulator) {
            nativeEmulator.setCallback(this);
//            setCallback(null);
        }
        return this;
    }

    private class NativeMsgWrapper{
        String msgId;
        String msgBody;
        NativeMsgWrapper(String msgId, String msgBody){this.msgId=msgId; this.msgBody=msgBody;}
    }


    // native methods

    private native int setCallback(INativeCallback callback);

    native int call(String methodName, String reqPara);  // request/set
    native int call(String methodName, StringBuffer output); // get
    native int call(String methodName, String para, StringBuffer output); // get

}

