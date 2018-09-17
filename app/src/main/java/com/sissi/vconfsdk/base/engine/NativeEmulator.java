package com.sissi.vconfsdk.base.engine;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

/**
 * Created by Sissi on 1/20/2017.
 * */
/**
 * 模拟器。<p>
 *
 * 若启用了模拟器则进入了“模拟模式”，模拟模式下模拟器替代了真实的远端(服务器)，请求会被定向到模拟器而非发给真实服务器.
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

    private Thread thread;
    private Handler handler;
    private INativeCallback cb;

    private NativeEmulator() {
        jsonProcessor = JsonProcessor.instance();
        initThread();
    }

    synchronized static NativeEmulator instance() {
        if (null == instance) {
            instance = new NativeEmulator();
        }

        return instance;
    }


    private void initThread(){
        final Object lock = new Object();
        thread = new Thread() {
            @SuppressLint("HandlerLeak")
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                Looper.prepare();
                handler = new Handler(){
                    @Override
                    public void handleMessage(Message msg) {
                        SessionManager.Session s = (SessionManager.Session) msg.obj;
                        String reqId = s.reqId();
                        Log.i(TAG, String.format("NATIVE RECV REQ %s: reqPara=%s", reqId, s.reqPara()));
                        String[] rspIds = s.rspIds();
                        Object[] rsps = s.rsps();
                        Contract.Head head;
                        Contract.Mtapi mtapi;
                        for (int i=0; i<rsps.length; ++i){
                            // 构造响应json字符串
                            head= new Contract.Head(-1, rspIds[i], 1);
                            mtapi= new Contract.Mtapi(head, rsps[i]);
                            String jsonRsp = jsonProcessor.toJson(new Contract.RspWrapper(mtapi));
                            if (null != cb){
                                // 上报响应
                                Log.i(TAG, String.format("NATIVE REPORT RSP %s(for REQ %s): rspContent=%s", rspIds[i], reqId, jsonRsp));
                                cb.callback(jsonRsp);
                            }
                            try {
                                sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                };
                synchronized (lock) { lock.notify(); }
                Looper.loop();
            }
        };

        thread.setName("NativeEmulator");

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
    public void ejectNotification(final String ntfId, final Object ntfContent) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Contract.Head head= new Contract.Head(-1, ntfId, 1);
                Contract.Mtapi mtapi= new Contract.Mtapi(head, ntfContent);
                String jsonNtf = jsonProcessor.toJson(new Contract.RspWrapper(mtapi));
                if (null != cb){
                    Log.i(TAG, String.format("NATIVE REPORT NTF %s: content=%s", ntfId, jsonNtf));
                    cb.callback(jsonNtf);
                }
            }
        });
    }

    @Override
    public int call(String methodName, String reqPara) {
        Message req = Message.obtain();
        req.obj = reqPara;
        handler.sendMessage(req);
        return 0;
    }

}
