package com.sissi.vconfsdk.base;

import com.sissi.vconfsdk.base.amulet.Caster;
import com.sissi.vconfsdk.utils.KLog;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public abstract class Requester implements Caster.IOnFeedbackListener {

    private static HashMap<Class<?>, Requester> instances = new HashMap<>();
    private static HashMap<Class<?>, Integer> refercnt = new HashMap<>();

    private int reqSn; // 请求序列号，唯一标识一次请求。
    private final HashMap<Integer, Object> rspListeners; // 响应监听者
    private final HashMap<Msg, Set<Object>> ntfListeners; // 通知监听者

    private Caster caster;

    protected Requester(){
        caster = new Caster();
        caster.setOnFeedbackListener(this);

        reqSn = 0;
        rspListeners = new HashMap<>();
        ntfListeners = new HashMap<>();

    }

    /**获取Jni请求者。
     * @param clz 请求者类型*/
    public synchronized static Requester instance(Class<?> clz){
        if (!Requester.class.isAssignableFrom(clz)){
            KLog.p("Invalid para!");
            return null;
        }
        Requester requester = instances.get(clz);
        if (null == requester){
            try {
                Constructor ctor = clz.getDeclaredConstructor((Class[])null);
                ctor.setAccessible(true);
                requester = (Requester) ctor.newInstance();
                instances.put(clz, requester);
                refercnt.put(clz, 1);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
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

        KLog.p("free presenter: "+clz);
        instances.remove(clz);
    }



    /**
     * 发送请求（不关注响应）
     * */
    protected synchronized void req(Msg reqId, Object reqPara){
        req(reqId, reqPara, null);
    }

    /**
     * 发送请求。
     * @param rspListener 响应监听者。
     * */
    protected synchronized void req(Msg reqId, Object reqPara, Object rspListener){
//        Log.i(TAG, String.format("rspListener=%s, reqId=%s, reqPara=%s", rspListener, reqId, reqPara));
        if (caster.req(reqId.name(), ++reqSn, reqPara)){
//            if (null != rspListener) {
            rspListeners.put(reqSn, rspListener);
//            }
        }
    }

    /**撤销请求*/
    protected synchronized void revertReq(Msg reqId, Object rspListener){
        // TODO
    }

    /**
     * 订阅通知
     * */
    protected synchronized void subscribe(Msg ntfId, Object ntfListener){
//        Log.i(TAG, String.format("ntfListener=%s, ntfId=%s", ntfListener, ntfId));
        if (null == ntfListener){
            return;
        }

        if (!caster.subscribe(ntfId.name())){
            return;
        }

        Set<Object> listeners = ntfListeners.get(ntfId);
        if (null == listeners){
            listeners = new HashSet<>();
            ntfListeners.put(ntfId, listeners);
        }
        listeners.add(ntfListener);
    }

    /**
     * 取消订阅通知
     * */
    protected synchronized void unsubscribe(Msg ntfId, Object ntfListener){
        if (null == ntfListener){
            return;
        }
        Set<Object> listeners = ntfListeners.get(ntfId);
        if (null != listeners){
            listeners.remove(ntfListener);
//            KLog.p("del ntfListener=%s, ntfId=%s", ntfListener, ntfId);
            if (listeners.isEmpty()) {
                ntfListeners.remove(ntfId);
                caster.unsubscribe(ntfId.name());
//                KLog.p("unsubscribeNtf %s", ntfId);
            }
        }
    }


    /**
     * 批量订阅通知
     * */
    protected synchronized void subscribe(Msg[] ntfIds, Object ntfListener){
        if (null == ntfListener || null == ntfIds){
            return;
        }
        for (Msg ntfId : ntfIds){
            subscribe(ntfId, ntfListener);
        }
    }

    /**
     * 批量取消订阅通知
     * */
    protected synchronized void unsubscribe(Msg[] ntfIds, Object ntfListener){
        if (null == ntfListener || null == ntfIds){
            return;
        }
        for (Msg ntfId : ntfIds) {
            unsubscribe(ntfId, ntfListener);
        }
    }

    /**
     * （驱使下层）发射通知。仅用于模拟模式。
     * */
    protected void eject(Msg ntfId){
//        Log.i(TAG, "eject ntf "+ntfId);
        caster.eject(ntfId.name());
    }

    /**
     * 设置配置
     * */
    protected void set(Msg setId, Object para){
        caster.set(setId.name(), para);
    }

    /**
     * 获取配置
     * */
    protected Object get(Msg getId){
        return caster.get(getId.name());
    }

    protected Object get(Msg getId, Object para){
        return caster.get(getId.name(), para);
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
    protected synchronized void delRspListener(Object rspListener){  // TODO 一个监听者可能监听多种响应而他只想删除其中一种响应的监听，所以再加一个响应id参数？
        Iterator<Map.Entry<Integer,Object>> iter = rspListeners.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Integer,Object> entry = iter.next();
            if(rspListener.equals(entry.getValue())){
                iter.remove();
            }
        }
    }

    /**
     * 删除通知监听者
     * */
    protected synchronized void delNtfListener(Object ntfListener){ // TODO 一个监听者可能监听多种响应而他只想删除其中一种响应的监听，所以再加一个响应id参数？
        for (Msg ntfId : ntfListeners.keySet()) {
            unsubscribe(ntfId, ntfListener);
        }
    }



    @Override
    public void onFeedbackRsp(String rspId, Object rspContent, String reqId, int reqSn) {
        Object rspListener = rspListeners.get(reqSn);
        onRsp(Msg.valueOf(rspId), rspContent, rspListener);
    }

    @Override
    public void onFeedbackRspFin(String rspId, Object rspContent, String reqId, int reqSn) {
        Object rspListener = rspListeners.get(reqSn);
        synchronized (this) {
            rspListeners.remove(reqSn); // 请求已结束，移除该次请求记录
        }
        onRsp(Msg.valueOf(rspId), rspContent, rspListener);
    }

    @Override
    public void onFeedbackTimeout(String reqId, int reqSn) {
        Object rspListener = rspListeners.get(reqSn);
        synchronized (this) {
            rspListeners.remove(reqSn); // 请求已结束，移除该次请求记录
        }
        onTimeout(Msg.valueOf(reqId), rspListener);
    }

    @Override
    public void onFeedbackNtf(String ntfId, Object ntfContent) {
        Msg ntfMsg = Msg.valueOf(ntfId);
        Set<Object> ntfListenerSet = ntfListeners.get(ntfMsg);
        if (null != ntfListenerSet){ //TODO ntfListeners为null时是否有必要让上层manager感知？
            for (Object ntfListener : ntfListenerSet) {
                onNtf(ntfMsg, ntfContent, ntfListener);
            }
        }
    }


    protected void onRsp(Msg rspId, Object rspContent, Object listener) {}

    protected void onNtf(Msg ntfId, Object ntfContent, Object listener) {}

    protected void onTimeout(Msg reqId, Object listener) {}

}
