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

public abstract class RequestAgent implements Caster.IOnFeedbackListener, ListenerLifecycleObserver.Callback{

    private static HashMap<Class<?>, RequestAgent> instances = new HashMap<>();
    private static HashMap<Class<?>, Integer> refercnt = new HashMap<>();

    private int reqSn; // 请求序列号，唯一标识一次请求。
    private final HashMap<Integer, Object> rspListeners; // 响应监听者
    private final HashMap<String, Set<Object>> ntfListeners; // 通知监听者
    private Set<Msg> caredNtfs;
    private Caster caster;

    private ListenerLifecycleObserver listenerLifecycleObserver;

    protected RequestAgent(){
        caster = new Caster();
        caster.setOnFeedbackListener(this);

        listenerLifecycleObserver = new ListenerLifecycleObserver(this);

        reqSn = 0;
        rspListeners = new HashMap<>();
        ntfListeners = new HashMap<>();

        caredNtfs = new HashSet<>();
        Msg[] ntfs = caredNtfs();
        if (null != ntfs) {
            String ntfName;
            for (Msg ntf : ntfs) {
                this.caredNtfs.add(ntf);
                ntfName = ntf.name();
                caster.subscribe(ntfName);
                ntfListeners.put(ntfName, new HashSet<>());
            }
        }
    }

    /**获取Jni请求者。
     * @param clz 请求者类型*/
    public synchronized static RequestAgent instance(Class<?> clz){
        if (!RequestAgent.class.isAssignableFrom(clz)){
            KLog.p("Invalid para!");
            return null;
        }
        RequestAgent requester = instances.get(clz);
        if (null == requester){
            try {
                Constructor ctor = clz.getDeclaredConstructor((Class[])null);
                ctor.setAccessible(true);
                requester = (RequestAgent) ctor.newInstance();
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

    protected Msg[] caredNtfs(){return null;}

    /**
     * 发送请求。
     * @param rspListener 响应监听者。
     * */
    protected synchronized void req(Msg reqId, Object reqPara, Object rspListener){
//        Log.i(TAG, String.format("rspListener=%s, reqId=%s, reqPara=%s", rspListener, reqId, reqPara));

        if (!caster.req(reqId.name(), ++reqSn, reqPara)){
            return;
        }

        if (null != rspListener) {
            rspListeners.put(reqSn, rspListener);
            listenerLifecycleObserver.tryObserve(rspListener);  // XXX 如果同一个listener多次调用该接口注册，则生命周期事件发生时会回调多次。
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

        if (!caredNtfs.contains(ntfId)){
            KLog.p(KLog.ERROR, "%s is not in 'cared-ntf-list'", ntfId);
            return;
        }

        String ntfName = ntfId.name();
        Set<Object> listeners = ntfListeners.get(ntfName);
        if (null == listeners) {
            listeners = new HashSet<>();
            ntfListeners.put(ntfName, listeners);
        }
        listeners.add(ntfListener);
        listenerLifecycleObserver.tryObserve(ntfListener);
    }

    /**
     * 取消订阅通知
     * */
    protected synchronized void unsubscribe(Msg ntfId, Object ntfListener){
        if (null == ntfListener){
            return;
        }

        if (!caredNtfs.contains(ntfId)){
            KLog.p(KLog.ERROR, "%s is not in 'cared-ntf-list'", ntfId);
            return;
        }

        String ntfName = ntfId.name();
        Set<Object> listeners = ntfListeners.get(ntfName);
        if (null != listeners){
            listeners.remove(ntfListener);
        }
    }


    /**
     * （驱使下层）发射通知。仅用于模拟模式。
     * */
    protected void eject(Msg ntfId){
//        Log.i(TAG, "eject ntf "+ntfId);
        if (!caredNtfs.contains(ntfId)){
            KLog.p(KLog.ERROR, "%s is not in 'cared-ntf-list'", ntfId);
            return;
        }
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
        if (null == listener){
            return;
        }
        delRspListener(listener);
        delNtfListener(listener);
    }

    /**
     * 删除响应监听者
     * */
    protected synchronized void delRspListener(Object rspListener){  // TODO 一个监听者可能监听多种响应而他只想删除其中一种响应的监听，所以再加一个响应id参数？
        if (null == rspListener){
            return;
        }
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
        if (null == ntfListener){
            return;
        }
        for (String ntfId : ntfListeners.keySet()) {
            unsubscribe(Msg.valueOf(ntfId), ntfListener);
        }
    }

    @Override
    public void onListenerResumed(Object listener) { // 该事件是粘滞的，即便activity已经resume很久了，然后才注册生命周期观察者也会收到该事件。
        KLog.p(""+ listener);
    }

    @Override
    public void onListenerPause(Object listener) {
        KLog.p(""+ listener);
        delListener(listener);
        // pause 只做标记，destroy才删除？保证onCreate中请求后，跳转到其他界面再跳回来时，请求结果依然能上报界面，而无需在onResume中再次请求。
    }

    @Override
    public void onListenerStop(Object listener) {
        KLog.p(""+ listener);
        delListener(listener);
    }

    @Override
    public void onFeedbackRsp(String rspId, Object rspContent, String reqId, int reqSn) {
        onRsp(Msg.valueOf(rspId), rspContent, rspListeners.get(reqSn));
    }

    @Override
    public void onFeedbackRspFin(String rspId, Object rspContent, String reqId, int reqSn) {
        onRsp(Msg.valueOf(rspId), rspContent, rspListeners.remove(reqSn));
    }

    @Override
    public void onFeedbackTimeout(String reqId, int reqSn) {
        onTimeout(Msg.valueOf(reqId), rspListeners.remove(reqSn));
    }

    @Override
    public void onFeedbackNtf(String ntfId, Object ntfContent) {
        onNtf(Msg.valueOf(ntfId), ntfContent, ntfListeners.get(ntfId));
    }


    protected void onRsp(Msg rspId, Object rspContent, Object listener) {}

    protected void onNtf(Msg ntfId, Object ntfContent, Set<Object> listeners) {}

    protected void onTimeout(Msg reqId, Object listener) {}

}
