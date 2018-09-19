package com.sissi.vconfsdk.base.engine;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import org.json.JSONException;

/**
 * Created by Sissi on 1/20/2017.
 */
@SuppressWarnings({"JniMissingFunction", "unused"})
final class NativeInteractor implements INativeCallback{

    private static final String TAG = NativeInteractor.class.getSimpleName();

    private static NativeInteractor instance;

    private Thread nativeCallbackProcessThread;
    private Handler nativeCallbackProcessHandler;

    private JsonProcessor jsonProcessor;  // XXX 可以把这个去掉, native 层配合传上来就是 msgId, body

    private boolean isWhiteListEnabled = false;
    private boolean isBlackListEnabled = false;


    private IResponseProcessor responseProcessor;
    private INotificationProcessor notificationProcessor;
    private INativeEmulator nativeEmulator;

    private NativeInteractor(){

        jsonProcessor = JsonProcessor.instance();

        initNativeCallbackProcessThread();
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

        return 0;
//        return call(methodName, reqPara);
    }

    int set(String methodName, String setPara){
        return call(methodName, setPara);
    }

    int get(String methodName, String para, StringBuffer output){
        return call(methodName, para, output);
    }

    int get(String methodName, StringBuffer output){
        return call(methodName, output);
    }

    /**
     * 发射通知。驱动模拟器发射通知，仅用于模拟模式。
     * */
    public boolean emitNotification(String ntfId){
        if (null == nativeEmulator){
            return false;
        }
        nativeEmulator.ejectNotification(ntfId);
        return true;
    }


    /**
     * native回调。<p>
     * 方法名称是固定的，要修改需和native层协商一致。
     * */
    @Override
    public void callback(String nativeMsg){
        enqueue(nativeMsg);
    }



    /**
     * native层响应。<p>
     * 该接口是非阻塞的，不会阻塞native线程。
     * @param nativeMsg json格式的响应。
     * */
    void enqueue(String nativeMsg){
        Message msg = Message.obtain();
        msg.obj = nativeMsg;
        nativeCallbackProcessHandler.sendMessage(msg);
    }



    /**
     * 初始化native消息处理线程
     * */
    private void initNativeCallbackProcessThread(){
        final Object lock = new Object();
        nativeCallbackProcessThread = new Thread(){
            @SuppressLint("HandlerLeak")
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                Looper.prepare();

                nativeCallbackProcessHandler = new Handler(){
                    @Override
                    public void handleMessage(Message msg) {
                        processNativeCallback(msg);
                    }
                };
                synchronized (lock){lock.notify();}

                Looper.loop();
            }
        };

        nativeCallbackProcessThread.setName("native message processor");

        nativeCallbackProcessThread.start();

        if (null == nativeCallbackProcessHandler){
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void processNativeCallback(Message msg){
        String rsp = (String) msg.obj;
        String msgName = null;
        String msgBody = null;

        try {
            Object rootObj = jsonProcessor.getRootObj(rsp);
            msgName = jsonProcessor.getRspName(rootObj);
            msgBody = jsonProcessor.getRspBody(rootObj);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (null == msgName || null == msgBody){
            Log.e(TAG, "Invalid rsp: "+ rsp);
            return;
        }

        if (null!=responseProcessor
                && responseProcessor.processResponse(msgName, msgBody)){

        }else if (null!=notificationProcessor
                && notificationProcessor.processNotification(msgName, msgBody)){

        }else{
            Log.e(TAG, String.format("<-~- %s. EXCEPTION: unprocessed msg. \n%s", msgName, rsp));
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



    // native methods

    private native int setCallback(INativeCallback callback);

    native int call(String methodName, String reqPara);  // request/set
    native int call(String methodName, StringBuffer output); // get
    native int call(String methodName, String para, StringBuffer output); // get

}

