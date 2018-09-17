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
    private static boolean reqEnabled = true;
    private static boolean callbackEnabled = true;

    private Thread nativeCallbackProcessThread;
    private Handler nativeCallbackProcessHandler;
    private static final int UI_REQ = -999;
    private static final int NATIVE_RSP = -998;

    private JsonProcessor jsonProcessor;
//    private MessageRegister messageRegister;

    private boolean isWhiteListEnabled = false;
    private boolean isBlackListEnabled = false;


    private IResponseProcessor responseProcessor;
    private INotificationProcessor notificationProcessor;
    private INativeEmulator nativeEmulator;

    private NativeInteractor(){

        jsonProcessor = JsonProcessor.instance();
//        messageRegister = MessageRegister.instance();
        // 此处写死 responseProcessor = NotifiManager.instance(); responseProcessor= ; nativeEmulator = ;因为可替换的部分是各个xxManager,而上层Requester以及下层NativeInteractor都是不变的. 此处是为了方便起见直接写死在这里, 更好的方式是支持外部注入.

        responseProcessor = SessionManager.instance();
        notificationProcessor = NotifiManager.instance();
        if (NativeEmulatorOnOff.on) {
            nativeEmulator = NativeEmulator.instance();
        }

        initNativeCallbackProcessThread();
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
        callbackEnabled = enable;
    }


    int emulateInvoke(String methodName, Object reqPara){ // 此reqPara为RequestBundle？或session？RequestBundle可删除？ XXX 上层（SessionManager）不应该感知下层具体是真实的调用还是模拟调用！！！意即不应该区别调用该接口， 但是真实请求和模拟请求数据不一样，如果用其它通用的invoke则模拟数据也能让sessionManager感知，得通过其它途径设置下来，在Requester内做。UI层的调用方式可以保持不变。模拟器、通知管理器、会话管理器、这些模块的组合需要高层的模块去做，而非像现在这样在SM、NM中设置。
        String jsonReqPara = jsonProcessor.toJson(reqPara);
        jsonReqPara = "null".equalsIgnoreCase(jsonReqPara) ? null : jsonReqPara;

        return nativeEmulator.call(methodName, jsonReqPara);
    }

    int request(String methodName, String reqPara){
        return call(methodName, reqPara);
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
    public boolean emulateNotify(String ntfId, Object ntfContent){
        if (null != nativeEmulator){
            nativeEmulator.ejectNotification(ntfId, jsonProcessor.toJson(ntfContent));
        }
        return true;
    }


    /**
     * native回调。<p>
     * 方法名称是固定的，要修改需和native层协商一致。
     * */
    @Override
    public void callback(String nativeMsg){
        if (!callbackEnabled){
            Log.e(TAG, "native callback disabled!");
            return;
        }
        enqueue(nativeMsg);
    }



    /**
     * native层响应。<p>
     * 该接口是非阻塞的，不会阻塞native线程。
     * @param nativeMsg json格式的响应。
     * */
    void enqueue(String nativeMsg){
        Message msg = Message.obtain();
        msg.what = NATIVE_RSP;
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
        if (NATIVE_RSP == msg.what){  // XXX 消息ID没必要
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

//            Class<?> clz = messageRegister.getRspClazz(rspName);
//            if (null == clz){
//                Log.e(TAG, "Failed to find clazz corresponding "+rspName);
//                return;
//            }
//
//            Object rspObj = jsonProcessor.fromJson(rspBody, clz);
//            if (null == rspObj){
//                Log.e(TAG, String.format("Failed to convert msg %s to object, msg json body: %s ", rspName, rspBody));
//                return;
//            }

//            if (messageRegister.isResponse(rspName)){
//                if (responseProcessor.processResponse(rspName, rspObj)){
//                    Log.i(TAG, String.format("<-~- %s\n%s", rspName, rsp));
//                }else{
//                    Log.e(TAG, String.format("<-~- %s. EXCEPTION: No session expects this response! \n%s", rspName, rsp));
//                }
//            }else if (messageRegister.isNotification(rspName)){
//                if (notificationProcessor.processNotification(rspName, rspObj)){
//                    Log.i(TAG, String.format("<<-~- %s\n%s", rspName, rsp));
//                }else{
//                    Log.e(TAG, String.format("<<-~- %s. EXCEPTION: No observer subscribes this notification! \n%s", rspName, rsp));
//                }
//            }else{
//                Log.e(TAG, String.format("<-~- %s. EXCEPTION: Unknown msg. \n%s", rspName, rsp));
//            }

            if (responseProcessor.processResponse(rspName, rspBody)){

            }else if (notificationProcessor.processNotification(rspName, rspBody)){

            }else{
                Log.e(TAG, String.format("<-~- %s. EXCEPTION: unexpected msg. \n%s", rspName, rsp));
            }

        }
    }

    // TODO 还是需要在此类中，此类更名为MessageProcessor, 注释处理器的更名为MessageAnnotationProcessor。　先绘图后编码。彻底把关系理清。　


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



    // native methods

    private native int setCallback(INativeCallback callback);

    native int call(String methodName, String reqPara);  // request/set
    native int call(String methodName, StringBuffer output); // get
    native int call(String methodName, String para, StringBuffer output); // get

}
