package com.sissi.vconfsdk.frame;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.sissi.vconfsdk.DmMsg;
import com.sissi.vconfsdk.utils.KLog;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * Jni请求者。用来向JM发送请求、订阅通知、接收响应。
 * Created by Sissi on 1/9/2017.
 */
public abstract class Requester{
    private MessageDispatcher messageDispatcher;
    private static HashMap<Class<?>, Requester> instances = new HashMap<>();
    private static HashMap<Class<?>, Integer> refercnt = new HashMap<>();
    private int reqSn; // 请求序列号，唯一标识一次请求。
    private final HashMap<Integer, Object> rspListenerList; // 响应监听者列表
    private HashMap<DmMsg, Set<Object>> ntfListenerList; // 通知监听者列表

    /* 辅助线程。
    对于高频次反馈的响应建议抛给辅助线程处理以减轻主线程压力但同时需要小心注意多线程可能带来的问题。
    请将响应序列作为整体抛给辅助线程处理，如某响应序列为xxx_rsp, xxx_fin_rsp，
     则应将两个响应均交给辅助线程处理或者均不交给，不要单独拿出一个抛给辅助线程处理，
     若违反了则需自行处理时序问题，否则可能导致上层收到响应时序紊乱*/
    private Thread assistThread;
    private Handler assistHandler;
//    private EmRsp[] assistThreadRsps; // 需要在辅助线程处理的响应

    private Handler handler;

    protected Requester(){
        handler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                processMsg(msg);
            }
        };

        messageDispatcher = MessageDispatcher.instance();
        reqSn = 0;
        rspListenerList = new HashMap<>();
        ntfListenerList = new HashMap<>();
    }

    /**获取Jni请求者。
     * @param clz 请求者类型*/
    public synchronized static Requester instance(Class<?> clz){
        if (!Requester.class.isAssignableFrom(clz)){
            KLog.p(KLog.ERROR, "Invalid para!");
            return null;
        }
        Requester requester = instances.get(clz);
        if (null == requester){
            try {
                Constructor ctor = clz.getDeclaredConstructor((Class[])null);
                ctor.setAccessible(true);
                requester = (Requester) ctor.newInstance();
                KLog.p("~~> new presenter: %s", requester);
                instances.put(clz, requester);
                refercnt.put(clz, 1);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        } else {
            int cnt = refercnt.get(clz);
            refercnt.put(clz, ++cnt);
        }

        return requester;
    }

    /**释放Jni请求者。
     * @param clz 请求者类型*/
    public synchronized static void free(Class<?> clz){
        int cnt = refercnt.get(clz);
        refercnt.put(clz, --cnt);
        if (cnt > 0){
            return;
        }

        KLog.p("free presenter: %s", clz);
        instances.remove(clz);
    }

//    public synchronized static void setReqEnable(boolean enable){
//        MessageDispatcher.setReqEnable(enable);
//    }
//    public synchronized static void setRspEnable(boolean enable){
//        MessageDispatcher.setRspEnable(enable);
//    }

    /**
     * 发送请求（不关注响应）
     * */
    protected synchronized void sendReq(DmMsg reqId, Object reqPara){
        sendReq(reqId, reqPara, null);
    }

    /**
     * 发送请求。
     * @param rspListener 响应监听者。
     * */
    protected synchronized void sendReq(DmMsg reqId, Object reqPara, Object rspListener){
//        KLog.p("rspListener=%s, reqId=%s, reqPara=%s", rspListener, reqId, reqPara);
        if (messageDispatcher.request(handler, reqId.name(), reqPara, ++reqSn)){
//            if (null != rspListener) {
                rspListenerList.put(reqSn, rspListener);
//            }
        }
    }

    /**
     * 发送请求。模拟模式，需自行指定期望的响应。
     * @param rspListener 响应监听者。
     * */
    protected synchronized void sendReq(DmMsg reqId, Object reqPara, Object[] rsps, Object rspListener){
//        KLog.p("rspListener=%s, reqId=%s, reqPara=%s, rsps=%s", rspListener, reqId, reqPara, rsps);
        if (messageDispatcher.request(handler, reqId.name(), reqPara, ++reqSn, rsps)){
//            if (null != rspListener) {
                rspListenerList.put(reqSn, rspListener);
//            }
        }
    }

    /**撤销请求*/
    protected synchronized void revertReq(DmMsg reqId, Object rspListener){
        // TODO
    }

    /**
     * 订阅通知
     * */
    protected synchronized void subscribeNtf(Object ntfListener, DmMsg ntfId){
//        KLog.p("ntfListener=%s, ntfId=%s", ntfListener, ntfId);
        if (null == ntfListener){
            return;
        }
        Set<Object> listeners = ntfListenerList.get(ntfId);
        if (null == listeners){
            messageDispatcher.subscribeNtf(handler, ntfId.name());
            listeners = new HashSet<Object>();
            ntfListenerList.put(ntfId, listeners);
        }
        listeners.add(ntfListener);
    }

    /**
     * 批量订阅通知
     * */
    protected synchronized void subscribeNtf(Object ntfListener, DmMsg[] ntfIds){
        if (null == ntfListener || null == ntfIds){
            return;
        }
        for (int i=0; i<ntfIds.length; ++i){
            subscribeNtf(ntfListener, ntfIds[i]);
        }
    }

    /**
     * 取消订阅通知
     * */
    protected synchronized void unsubscribeNtf(Object ntfListener, DmMsg ntfId){
        if (null == ntfListener){
            return;
        }
        Set<Object> listeners = ntfListenerList.get(ntfId);
        if (null != listeners){
            listeners.remove(ntfListener);
//            KLog.p("del ntfListener=%s, ntfId=%s", ntfListener, ntfId);
            if (listeners.isEmpty()) {
                ntfListenerList.remove(ntfId);
                messageDispatcher.unsubscribeNtf(handler, ntfId.name());
//                KLog.p("unsubscribeNtf %s", ntfId);
            }
        }
    }

    /**
     * 批量取消订阅通知
     * */
    protected synchronized void unsubscribeNtf(Object ntfListener, DmMsg[] ntfIds){
        if (null == ntfListener || null == ntfIds){
            return;
        }
        for (DmMsg ntfId:ntfIds) {
            unsubscribeNtf(ntfListener, ntfId);
        }
    }

    /**
     * （驱使下层）发射通知。仅用于模拟模式。
     * */
    protected synchronized void ejectNtf(DmMsg ntfId, Object ntf){
//        KLog.p("ntfId=%s, ntf=%s", ntfId, ntf);
        messageDispatcher.ejectNtf(ntfId.name(), ntf);
    }

    /**
     * 设置配置
     * */
    protected synchronized void setConfig(DmMsg reqId, Object config){
        messageDispatcher.setConfig(reqId.name(), config);
    }

    /**
     * 获取配置
     * */
    protected synchronized Object getConfig(DmMsg reqId){ // TODO 获取配置也有可能需要除reqId外的传入参数
//        return messageDispatcher.getConfig(reqId);
        return null;
    }

    /**
     * 删除监听者。
     * */
    public synchronized void delListener(Object listener){
        if (null == listener){ // TOIMPROVE 添加Listener的时候允许为null
            return;
        }
        delRspListener(listener);
        delNtfListener(listener);
    }

    /**
     * 删除响应监听者
     * */
    protected synchronized void delRspListener(Object rspListener){
        Iterator<Map.Entry<Integer,Object>> iter = rspListenerList.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Integer,Object> entry = iter.next();
            if(rspListener.equals(entry.getValue())){
//                KLog.p("del rspListener: %s", rspListener);
                iter.remove();
            }
        }
    }

    /**
     * 删除通知监听者
     * */
    protected synchronized void delNtfListener(Object ntfListener){
        for (DmMsg ntfId : ntfListenerList.keySet()) {
            unsubscribeNtf(ntfListener, ntfId);
        }
    }


//    /**处理Jni层反馈的响应、通知*/
//    @Override
//    public void handleMessage(Message msg) {
//        KLog.p("handle msg=%s", msg);
//        MessageDispatcher.ResponseBundle responseBundle = (MessageDispatcher.ResponseBundle) msg.obj;
//        EmRsp[] rspIds = EmRsp.values();
//        if (rspOrdinal<0 || rspIds.length<=rspOrdinal){
//            KLog.p(KLog.ERROR, "Invalid rsp ordinal %d", rspOrdinal);
//            return;
//        }
//        EmRsp rspId = rspIds[rspOrdinal];
//        if (null != assistThreadRsps){
//            for (EmRsp r : assistThreadRsps){
//                if (r == rspId){ // 该响应需要在辅助线程处理
//                    if (null == assistThread){
//                        initAssistThread();
//                    }
//                    assistHandler.sendMessage(Message.obtain(msg));
//                    return;
//                }
//            }
//        }

//        processMsg(msg);
//    }

    private void processMsg(Message msg){
        ResponseBundle responseBundle = (ResponseBundle) msg.obj;
        Object rspContent = responseBundle.body();
        int type = responseBundle.type();
        int reqSn = responseBundle.reqSn();
        if (ResponseBundle.NTF == type){
            // 通知
            DmMsg ntfId = DmMsg.valueOf(responseBundle.name());
            Set<Object> ntfListeners = ntfListenerList.get(ntfId);
            if (null != ntfListeners){
                onNtf(ntfListeners, ntfId, rspContent);
            }
        }else if (ResponseBundle.RSP_TIMEOUT == type){
            // 请求超时
            Object rspListener = rspListenerList.get(reqSn);
            synchronized (rspListenerList) {
                rspListenerList.remove(reqSn); // 请求已结束，移除该次请求记录
            }
            onTimeout(rspListener, DmMsg.valueOf(responseBundle.reqName()));
        }else{
            // 响应
            Object rspListener = rspListenerList.get(reqSn);
            if (ResponseBundle.RSP_FIN == type){
                synchronized (rspListenerList) {
                    rspListenerList.remove(reqSn); // 请求已结束，移除该次请求记录
                }
            }
//            if (null != rspListener){
            DmMsg rspId = DmMsg.valueOf(responseBundle.name());
            onRsp(rspListener, rspId, rspContent);
//            }
        }
    }

    /**
     * 处理响应
     * @param listener 响应监听者
     * @param rspId 响应ID
     * @param rspContent 响应内容*/
    protected void onRsp(Object listener, DmMsg rspId, Object rspContent){ }

    /**
     * 处理通知
     * @param listeners 通知监听者列表
     * @param rspId 通知ID
     * @param rspContent 通知内容*/
    protected void onNtf(Set<Object> listeners, DmMsg rspId, Object rspContent){ }

    /**
     * 处理请求超时
     * @param listener 响应监听者
     * @param reqId 请求ID
     * */
    protected void onTimeout(Object listener, DmMsg reqId){ }


//    /**设置响应在非主线程处理。
//     * @param rsps 在非主线程处理的<b>响应序列<b/>。
//     * 注意：如果请求对应多条响应，请将整个响应序列作为一个整体设置下来，而非单独设置其中某一个或某几个。
//     * */
//    protected synchronized void setAssistThreadRsps(EmRsp[] rsps){
//        assistThreadRsps = rsps;
//    }
//
//    /**初始化辅助线程*/
//    private void initAssistThread(){
//        final Object lock = new Object();
//        assistThread = new Thread(){
//            @Override
//            public void run() {
//                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
//                Looper.prepare();
//                assistHandler = new Handler(){
//                    @Override
//                    public void handleMessage(Message msg) {
//                        processMsg(msg);
//                    }
//                };
//                synchronized (lock){lock.notify();}
//                Looper.loop();
//                KLog.p("assist thread quit");
//            }
//        };
//
//        assistThread.setName("Requester.assist");
//        assistThread.start();
//
//        // 保证往下继续执行时handler已初始化完成
//        if (null == assistHandler){
//            synchronized (lock) {
//                try {
//                    lock.wait();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//
//        // 1分钟后尝试退出辅助线程
//        assistHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                // 判断消息队列是否为空
//                boolean isMsgQueEmpty = true;
//                for (int i=0; i<assistThreadRsps.length; ++i) {
//                    if (assistHandler.hasMessages(assistThreadRsps[i].ordinal())) {
//                        isMsgQueEmpty = false;
//                        break;
//                    }
//                }
//
//                if (isMsgQueEmpty) { // 没有待处理的消息
//                    Looper.myLooper().quit();
//                    assistThread = null;
//                    assistHandler = null;
//                }else{ // 尚存在待处理的消息
//                    assistHandler.postDelayed(this, 2*1000); // 2秒后再次尝试退出
//                }
//            }
//        }, 1*60*1000);
//    }


}
