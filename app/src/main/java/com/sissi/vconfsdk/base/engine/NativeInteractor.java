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

    private static final String TAG = "NativeInteractor";

    private static NativeInteractor instance;
    private static boolean reqEnabled = true; // 是否允许发送请求
    private static boolean rspEnabled = true; // 是否允许接收响应（包括RSP和NTF）

    private Thread msgProcessThread; // 接收响应线程
    private Handler msgHandler; // 响应handler
    private static final int UI_REQ = -999;
    private static final int NATIVE_RSP = -998;

    private JsonProcessor jsonProcessor;    // json管理器，负责序列化反序列化
    private MessageRegister messageRegister; // 请求-响应映射器(保存有请求响应的映射关系)

    private boolean isWhiteListEnabled = false;
    private boolean isBlackListEnabled = false;


    private IResponseProcessor responseProcessor;
    private INotificationProcessor notificationProcessor;
    private INativeEmulator nativeEmulator; // native模拟器。可模拟native层接收请求及反馈响应，仅用于调试！

    private NativeInteractor(){

        jsonProcessor = JsonProcessor.instance();
        messageRegister = MessageRegister.instance();

        initMessageProcessThread();
    }

    public synchronized static NativeInteractor instance() {
        if (null == instance) {
            instance = new NativeInteractor();
//            setCallback(instance);
        }

        return instance;
    }

    /**
     * 设置是否允许发送请求
     * */
    synchronized void setReqEnable(boolean enable){
        reqEnabled = enable;
    }
    /**
     * 设置是否允许接收响应
     * */
    synchronized void setRspEnable(boolean enable){
        rspEnabled = enable;
    }


    /**
     * native层响应。<p>
     * 该接口是非阻塞的，不会阻塞native线程。
     * @param nativeMsg json格式的响应。
     * */
    void respond(String nativeMsg){
        if (!rspEnabled){
            Log.e(TAG, "Respond disabled!");
            return;
        }
        Message msg = Message.obtain();
        msg.what = NATIVE_RSP;
        msg.obj = nativeMsg;
        msgHandler.sendMessage(msg);
    }


//    /**
//     * 发射通知。驱动模拟器发射通知，仅用于模拟模式。
//     * */
//    public boolean ejectNtf(final String ntfId, Object ntf){
//        if (!messageRegister.isNotification(ntfId)){
//            Log.e(TAG, "Unknown notification "+ntfId);
//            return false;
//        }
//        if (null != nativeEmulator){
//            nativeEmulator.ejectNtf(ntfId, ntf);
//        }
//        return true;
//    }




    /**
     * 初始化native消息处理线程
     * */
    private void initMessageProcessThread(){
        final Object lock = new Object();
        msgProcessThread = new Thread(){
            @SuppressLint("HandlerLeak")
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                Looper.prepare();

                msgHandler = new Handler(){
                    @Override
                    public void handleMessage(Message msg) {
                        processMessage(msg);
                    }
                };
                synchronized (lock){lock.notify();}

                Looper.loop();
            }
        };

        msgProcessThread.setName("JM.rsp");

        msgProcessThread.start();

        if (null == msgHandler){
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void processMessage(Message msg){
        if (NATIVE_RSP == msg.what){
            String rsp = (String) msg.obj;
            String rspName = null;
            String rspBody = null;

            try {
                Object rootObj = jsonProcessor.getRootObj(rsp);
                rspName = jsonProcessor.getRspName(rootObj);
                rspBody = jsonProcessor.getRspBody(rootObj);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (null == rspName || null == rspBody){
                Log.e(TAG, "Invalid rsp: "+ rsp);
                return;
            }

            Class<?> clz = messageRegister.getRspClazz(rspName);
            if (null == clz){
                Log.e(TAG, "Failed to find clazz corresponding "+rspName);
                return;
            }

            Object rspObj = jsonProcessor.fromJson(rspBody, clz);
            if (null == rspObj){
                Log.e(TAG, String.format("Failed to convert msg %s to object, msg json body: %s ", rspName, rspBody));
                return;
            }

            if (messageRegister.isResponse(rspName)){
                if (responseProcessor.process(rspName, rspObj)){
                    Log.i(TAG, String.format("<-~- %s\n%s", rspName, rsp));
                }else{
                    Log.e(TAG, String.format("<-~- %s. EXCEPTION: No session expects this response! \n%s", rspName, rsp));
                }
            }else if (messageRegister.isNotification(rspName)){
                if (notificationProcessor.process(rspName, rspObj)){
                    Log.i(TAG, String.format("<<-~- %s\n%s", rspName, rsp));
                }else{
                    Log.e(TAG, String.format("<<-~- %s. EXCEPTION: No observer subscribes this notification! \n%s", rspName, rsp));
                }
            }else{
                Log.e(TAG, String.format("<-~- %s. EXCEPTION: Unknown msg. \n%s", rspName, rsp));
            }

        }
    }



    void setResponseProcessor(IResponseProcessor responseProcessor){
        this.responseProcessor = responseProcessor;
    }

    void setNotificationProcessor(INotificationProcessor notificationProcessor){
        this.notificationProcessor = notificationProcessor;
    }

    void setNativeEmulator(INativeEmulator nativeEmulator){
        this.nativeEmulator = nativeEmulator;
        nativeEmulator.setCallback(this);
    }


    int call(String methodName, Object reqPara){
        String jsonReqPara = jsonProcessor.toJson(reqPara);
        jsonReqPara = "null".equalsIgnoreCase(jsonReqPara) ? null : jsonReqPara;

        return invoke(methodName, jsonReqPara);
    }


    int call(String methodName, StringBuffer output){
        return invoke(methodName, output);
    }

    int call(String methodName, Object para, StringBuffer output){
        String jsonPara = jsonProcessor.toJson(para);
        jsonPara = "null".equalsIgnoreCase(jsonPara) ? null : jsonPara;

        return invoke(methodName, jsonPara, output);
    }

    int emulateCall(String methodName, Object reqPara){
        String jsonReqPara = jsonProcessor.toJson(reqPara);
        jsonReqPara = "null".equalsIgnoreCase(jsonReqPara) ? null : jsonReqPara;

        return nativeEmulator.invoke(methodName, jsonReqPara);
    }

    /**
     * native回调。<p>
     * 方法名称是固定的，要修改需和native层协商一致。
     * */
    @Override
    public void callback(String nativeMsg){
        respond(nativeMsg);
    }

    // native methods

    private native int setCallback(Object callback);

    native int invoke(String methodName, String reqPara);  // request/set
    native int invoke(String methodName, StringBuffer output); // get
    native int invoke(String methodName, String para, StringBuffer output); // get

}
