package com.sissi.vconfsdk.frame;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import org.json.JSONException;

/**
 * Jni管理器（JM）。
 * Created by Sissi on 1/5/2017.
 */

public final class JniManager {
    
    private static final String TAG = "JniManager";
    
    private static JniManager instance;
    private static boolean reqEnabled = true; // 是否允许发送请求
    private static boolean rspEnabled = true; // 是否允许接收响应（包括RSP和NTF）

    private Thread reqThread; // 发送请求线程
    private Handler reqHandler; // 请求handler
    private Thread rspThread; // 接收响应线程
    private Handler rspHandler; // 响应handler
    private static final int UI_REQ = -999;
    private static final int NATIVE_RSP = -998;

    private ConfigManager configManager;  // 配置管理器
    private SessionManager sessionManager;  // 会话管理器
    private NotifyManager notifyManager; // 通知管理器
    private JsonManager jsonManager;    // json管理器，负责序列化反序列化
    private MessageRegister messageRegister; // 请求-响应映射器(保存有请求响应的映射关系)

    private NativeEmulator nativeEmulator; // native模拟器。可模拟native层接收请求及反馈响应，仅用于调试！

    private boolean isWhiteListEnabled = false;
    private boolean isBlackListEnabled = false;

    // 消息类型
    public static final int NTF = 101; // 通知
    public static final int RSP = 102; // 响应
    public static final int RSP_FIN = 103; // 响应结束（收到响应序列中的最后一条响应）
    public static final int RSP_TIMEOUT = 104; // 响应超时

    private JniManager(){
        configManager = ConfigManager.instance();
        sessionManager = SessionManager.instance();
        notifyManager = NotifyManager.instance();

        jsonManager = JsonManager.instance();
        messageRegister = MessageRegister.instance();

        if (NativeEmulatorOnOff.on) {
            // 模拟模式开启
            initEmulator();
            sessionManager.setEmulatedNativeHandler(nativeEmulator.getHandler());
        }
        initReqThread();
        initRspThread();
        sessionManager.setSendreqHandler(reqHandler);
    }

    public synchronized static JniManager instance() {
        if (null == instance) {
            instance = new JniManager();
//            NativeMethods.setCallback(instance);
        }

        return instance;
    }

    /**
     * 设置是否允许发送请求
     * */
    synchronized static void setReqEnable(boolean enable){
        reqEnabled = enable;
    }
    /**
     * 设置是否允许接收响应
     * */
    synchronized static void setRspEnable(boolean enable){
        rspEnabled = enable;
    }


    /**
     * 发送请求。<p>
     * 该接口是非阻塞的，一方面意味调用该接口不会导致调用者阻塞，另一方面意味着接口返回不代表请求已发送出去仅代表请求已加入请求缓存队列。
     * @param requester 请求者。
     * @param reqName 请求消息名称。
     * @param reqPara 请求参数。
     * @param reqSn 请求序列号。
     * @param rsps 请求对应的响应。若不为null则表示此为模拟请求，消息交互走模拟器而非真实native层。
     * @return 返回真，若发送请求成功，返回假若发送失败。
     * */
    public boolean request(final Handler requester, final String reqName, final Object reqPara, final int reqSn, final Object[] rsps){
        if (!reqEnabled){
            Log.e(TAG, "Request disabled!");
            return false;
        }
        if (null != rsps && null==nativeEmulator){
            // 期望使用模拟模式但模拟器没开
            Log.e(TAG, "Emulator not enabled");
            return false;
        }
        if (null==requester || null==reqName){
            Log.e(TAG, "Invalid para");
            return false;
        }
        String jsonReqPara = jsonManager.toJson(reqPara);
        jsonReqPara = "null".equalsIgnoreCase(jsonReqPara) ? null : jsonReqPara;
        Message msg = Message.obtain();
        msg.what = UI_REQ;
        msg.obj = new RequestBundle(requester, reqName, jsonReqPara, reqSn, rsps);
        reqHandler.sendMessage(msg);

        return true;
    }

    public boolean request(final Handler requester, final String reqName, final Object reqPara, final int reqSn){
        return request(requester, reqName, reqPara, reqSn, null);
    }

    /**
     * 取消请求。
     * */
    void cancelRequest(Handler requester, String reqName){

    }


    /**
     * native层响应。<p>
     * 该接口是非阻塞的，不会阻塞native线程。
     * @param jsonRsp json格式的响应。
     * */
    void respond(String jsonRsp){
        if (!rspEnabled){
            Log.e(TAG, "Respond disabled!");
            return;
        }
        Message msg = Message.obtain();
        msg.what = NATIVE_RSP;
        msg.obj = jsonRsp;
        rspHandler.sendMessage(msg);
    }

    /**
     * 订阅通知。
     * @param subscriber 订阅者。
     * @param ntfId 订阅的通知。
     * */
    public void subscribeNtf(Handler subscriber, String ntfId){
        if (!rspEnabled){
            Log.e(TAG, "Respond disabled!");
            return;
        }
        Log.i(TAG, String.format("-*-> %s subscriber=%s", ntfId, subscriber));
        notifyManager.subscribeNtf(subscriber, ntfId);
    }

    /**
     * 取消订阅通知。
     * @param subscriber 订阅者。
     * @param ntfId 订阅的通知。
     * */
    public void unsubscribeNtf(Handler subscriber, String ntfId){
        Log.i(TAG, String.format("-*-< %s subscriber=%s", ntfId, subscriber));
        notifyManager.unsubscribeNtf(subscriber, ntfId);
    }

    /**
     * 发射通知。驱动模拟器发射通知，仅用于模拟模式。
     * */
    public void ejectNtf(final String ntfId, Object ntf){
        if (!rspEnabled){
            Log.e(TAG, "Respond disabled!");
            return;
        }
        if (null != nativeEmulator){
            nativeEmulator.ejectNtf(ntfId, ntf);
        }
    }

    /**
     * 设置配置。
     * 该接口阻塞
     * */
    public void setConfig(String reqId, Object config){
        if (!reqEnabled){
            Log.e(TAG, "Request disabled!");
            return;
        }
        String jsonConfig = null==config ? null : jsonManager.toJson(config);
        Log.i(TAG, String.format("-~->| %s\npara=%s", reqId, jsonConfig));
        configManager.setConfig(reqId, jsonConfig);
    }

    /**
     * 获取配置。
     * 该接口阻塞
     * */
    Object getConfig(String reqId){
//        if (!reqEnabled){
//            Log.e(TAG, "Request disabled!");
//            return null;
//        }
//        Class<?> clz = messageRegister.getConfClazz(reqId);
//        if (null == clz){
//            Log.e(TAG, "No register clazz for "+reqId);
//            return null;
//        }
//
//        Log.i(TAG, String.format("-~->| %s", reqId));
//        String config = configManager.getConfig(reqId);
//        return jsonManager.fromJson(config, clz);
        return null;
    }

    /**
     * native回调。<p>
     * 方法名称是固定的，要修改需和native层协商一致。
     * */
    void callback(String jsonRsp){
        respond(jsonRsp);
    }

    /**
     * 初始化发送请求线程
     * */
    private void initReqThread(){
        final Object lock = new Object();
        reqThread = new Thread(){
            @SuppressLint("HandlerLeak") // 或者, 可以使用Handler.Callback替代在Thread中定义内部类Handler以避免"HandlerLeak"问题，尤其在主线程中建议如此。
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                Looper.prepare();

                reqHandler = new Handler(){
                    @Override
                    public void handleMessage(Message msg) {
                        processReq(msg);
                    }
                };
                synchronized (lock){lock.notify();}

                Looper.loop();
            }
        };

        reqThread.setName("JM.req");

        reqThread.start();

        if (null == reqHandler){
            synchronized (lock) {
                try {
                    lock.wait(); // 保证thread初始化结束后handler立即可用。
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 初始化接收响应线程
     * */
    private void initRspThread(){
        final Object lock = new Object();
        rspThread = new Thread(){
            @SuppressLint("HandlerLeak")
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                Looper.prepare();

                rspHandler = new Handler(){
                    @Override
                    public void handleMessage(Message msg) {
                        processRsp(msg);
                    }
                };
                synchronized (lock){lock.notify();}

                Looper.loop();
            }
        };

        rspThread.setName("JM.rsp");

        rspThread.start();

        if (null == rspHandler){
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void processReq(Message msg){
        if (UI_REQ == msg.what){
            RequestBundle reqBundle = (RequestBundle) msg.obj;
            Log.i(TAG, String.format("-~-> %s\npara=%s\nrequester=%s", reqBundle.reqName, reqBundle.reqPara, reqBundle.requester));
            if (!sessionManager.request(reqBundle.requester, reqBundle.reqName, reqBundle.reqPara, reqBundle.reqSn, reqBundle.rsps)){
                Log.e(TAG, "Session request failed!");
            }
        }
    }

    private void processRsp(Message msg){
        if (NATIVE_RSP == msg.what){
            String rsp = (String) msg.obj;
            String rspName = null;
            String rspBody = null;

            try {
                Object rootObj = jsonManager.getRootObj(rsp);
                rspName = jsonManager.getRspName(rootObj);
                rspBody = jsonManager.getRspBody(rootObj);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (null == rspName || null == rspBody){
                Log.e(TAG, "Invalid rsp: "+ rsp);
                return;
            }

//            Log.i(TAG,"<-~- "+rspName+"\n"+rsp);

            Class<?> clz = messageRegister.getRspClazz(rspName);
            if (null == clz){
                Log.e(TAG, "Failed to find clazz corresponding "+rspName);
                return;
            }

            Object rspObj = jsonManager.fromJson(rspBody, clz);
            if (null == rspObj){
                Log.e(TAG, String.format("Failed to convert msg %s to object, msg json body: %s ", rspName, rspBody));
                return;
            }

            if (sessionManager.respond(rspName, rspObj)){
                Log.i(TAG,String.format("<-~- %s\n%s", rspName, rsp));
            }else if(notifyManager.notify(rspName, rspObj)){
                Log.i(TAG,String.format("<<-~- %s\n%s", rspName, rsp));
            }else {
                Log.e(TAG, "Unexpected msg: "+rspName);
            }
        }
    }

    /**
     * 初始化Native模拟器
     * */
    private void initEmulator(){
        nativeEmulator = NativeEmulator.instance();
        nativeEmulator.setCallback(new NativeEmulator.Callback(){
            @Override
            public void callback(String jsonRsp) {
                respond(jsonRsp);
            }
        });
    }

    class RequestBundle{
        Handler requester; // 请求者
        String reqName; // 请求消息名称
        String reqPara; // 请求参数(JSon格式)
        int reqSn; // 请求序列号
        Object[] rsps;  // 模拟响应. 仅模拟模式下有意义
        RequestBundle(Handler requester, String reqName, String reqPara, int reqSn, Object[] rsps){
            this.requester = requester;
            this.reqName = reqName;
            this.reqPara = reqPara;
            this.reqSn = reqSn;
            this.rsps = rsps;
        }
    }


    public static class ResponseBundle{
        String name;  // 响应消息名称
        Object body; // 响应消息体
        int type;   // 响应类型：NTF、RSP、FIN、TIMEOUT
        String reqName; // 对应的请求消息名称
        int reqSn;  // 对应的请求序列号

        ResponseBundle(String name, Object body, int type, String reqName, int reqSn){
            this.name = name;
            this.body = body;
            this.type = type;
            this.reqName = reqName;
            this.reqSn = reqSn;
        }

        ResponseBundle(String name, Object body, int type){
            this.name = name;
            this.body = body;
            this.type = type;
        }

        public String name(){
            return name;
        }

        public Object body(){
            return body;
        }

        public int type(){
            return type;
        }

        public String reqName(){
            return reqName;
        }

        public int reqSn(){
            return reqSn;
        }
    }

}
