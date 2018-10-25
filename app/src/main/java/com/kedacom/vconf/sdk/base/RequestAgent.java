package com.kedacom.vconf.sdk.base;

import android.support.annotation.RestrictTo;

import com.kedacom.vconf.sdk.base.amulet.Caster;
import com.kedacom.vconf.sdk.utils.KLog;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public abstract class RequestAgent implements Caster.IOnFeedbackListener, ListenerLifecycleObserver.Callback{

    private int reqSn; // 请求序列号，唯一标识一次请求。
    private final Map<Integer, RequestBundle> rspListeners;
    private final Map<String, Set<INotificationListener>> ntfListeners;

    private Map<Msg, RspProcessor> rspProcessorMap;
    private Map<Msg, NtfProcessor> ntfProcessorMap;

    private ListenerLifecycleObserver listenerLifecycleObserver;

    private Caster caster;

    protected RequestAgent(){
        caster = new Caster();
        caster.setOnFeedbackListener(this);

        listenerLifecycleObserver = new ListenerLifecycleObserver(this);

        reqSn = 0;
        rspListeners = new HashMap<>();
        ntfListeners = new HashMap<>();

        rspProcessorMap = rspProcessors();
        if (null == rspProcessorMap){
            rspProcessorMap = new HashMap<>();
        }
        ntfProcessorMap = ntfProcessors();
        if (null != ntfProcessorMap){
            String ntfName;
            for (Msg ntf : ntfProcessorMap.keySet()){
                ntfName = ntf.name();
                caster.subscribe(ntfName);
                ntfListeners.put(ntfName, new HashSet<>());
            }
        }else{
            ntfProcessorMap = new HashMap<>();
        }
    }


    protected abstract Map<Msg, RspProcessor> rspProcessors();
    protected abstract Map<Msg, NtfProcessor> ntfProcessors();

    protected interface RspProcessor{
        void process(Msg rspId, Object rspContent, IResultListener listener);
    }

    protected interface NtfProcessor{
        void process(Msg ntfId, Object ntfContent, Set<INotificationListener> listeners);
    }


    /**
     * 发送请求。
     * @param rspListener 响应监听者。
     * */
    protected synchronized void req(Msg reqId, Object reqPara, IResultListener rspListener){
//        Log.i(TAG, String.format("rspListener=%s, reqId=%s, para=%s", rspListener, reqId, para));

        if (!rspProcessorMap.keySet().contains(reqId)){
            KLog.p(KLog.ERROR, "%s is not in 'cared-req-list'", reqId);
            return;
        }

        if (!caster.req(reqId.name(), ++reqSn, reqPara)){
            return;
        }

        if (null != rspListener) {
            rspListeners.put(reqSn, new RequestBundle(reqId, rspListener));
            listenerLifecycleObserver.tryObserve(rspListener);  // 注意：如果同一个listener多次调用该接口注册，则该listener生命周期事件发生时会回调多次。
        }
    }

    /**
     * 取消请求。
     * 若同样的请求id同样的响应监听者请求了多次，则取消的是最早的请求。*/
    protected synchronized void cancelReq(Msg reqId, IResultListener rspListener){
        if (null == reqId || null == rspListener){
            return;
        }
        if (!rspProcessorMap.keySet().contains(reqId)){
            KLog.p(KLog.ERROR, "%s is not in 'cared-req-list'", reqId);
            return;
        }

        int BIG_REQSN = 987654321;
        int earliestReq = BIG_REQSN;
        RequestBundle bundle;
        int reqSn;
        for (Map.Entry<Integer, RequestBundle> entry : rspListeners.entrySet()){
            reqSn = entry.getKey();
            bundle = entry.getValue();
            if (reqId.equals(bundle.reqId)
                    && rspListener.equals(bundle.resultListener)){
                if (reqSn < earliestReq){
                    earliestReq = reqSn;
                }
            }
        }

        if (BIG_REQSN != earliestReq){
            caster.cancelReq(earliestReq);
        }

    }

    /**
     * 订阅通知
     * */
    protected synchronized void subscribe(Msg ntfId, INotificationListener ntfListener){
//        Log.i(TAG, String.format("ntfListener=%s, ntfId=%s", ntfListener, ntfId));
        if (null == ntfListener){
            return;
        }

        if (!ntfProcessorMap.keySet().contains(ntfId)){
            KLog.p(KLog.ERROR, "%s is not in 'cared-ntf-list'", ntfId);
            return;
        }

        String ntfName = ntfId.name();
        Set<INotificationListener> listeners = ntfListeners.get(ntfName);
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

        String ntfName = ntfId.name();
        Set<INotificationListener> listeners = ntfListeners.get(ntfName);
        if (null != listeners){
            listeners.remove(ntfListener);
        }
    }


    /**
     * （驱使下层）发射通知。仅用于模拟模式。
     * */
    protected void eject(Msg ntfId){
//        Log.i(TAG, "eject ntf "+ntfId);
        if (!ntfProcessorMap.keySet().contains(ntfId)){
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
        Iterator<Map.Entry<Integer,RequestBundle>> iter = rspListeners.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Integer,RequestBundle> entry = iter.next();
            IResultListener resultListener = entry.getValue().resultListener;
            if(rspListener.equals(resultListener)){
                iter.remove();
            }
        }
    }

    /**
     * 删除通知监听者
     * */
    protected synchronized void delNtfListener(Object ntfListener){
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
        /*pause 只做标记，destroy才删除？保证onCreate中请求后，跳转到其他界面再跳回来时，请求结果依然能上报界面，而无需在onResume中再次请求。
        * 但是这样就需要缓存消息，并且XXXManager也需要等待，导致原本可以后台处理的逻辑停滞。比如后台保存联系人。或者可以设定一个超时来改善这种
        * 情况？比如设定1分钟超时，超过一分钟则上报该响应给xxxManager，若1分钟内界面切回来onResume了则立即推消息给界面？ 也不好。考虑使用LiveData
        * 等android生命周期相关的组件*/
    }

//    @Override
//    public void onListenerStop(Object listener) {
//        KLog.p(""+ listener);
//        delListener(listener);
//    }

    @Override
    public void onFeedbackRsp(String rspId, Object rspContent, String reqId, int reqSn) {
        RequestBundle requestBundle = rspListeners.get(reqSn);
        IResultListener resultListener = null == requestBundle ? null : requestBundle.resultListener;
        rspProcessorMap.get(Msg.valueOf(reqId))
                .process(Msg.valueOf(rspId), rspContent, resultListener);
    }

    @Override
    public void onFeedbackRspFin(String rspId, Object rspContent, String reqId, int reqSn) {
        RequestBundle requestBundle = rspListeners.remove(reqSn);
        IResultListener resultListener = null == requestBundle ? null : requestBundle.resultListener;
        rspProcessorMap.get(Msg.valueOf(reqId))
                .process(Msg.valueOf(rspId), rspContent, resultListener);
    }

    @Override
    public void onFeedbackTimeout(String reqId, int reqSn) {
        RequestBundle requestBundle = rspListeners.remove(reqSn);
        IResultListener resultListener = null == requestBundle ? null : requestBundle.resultListener;
        rspProcessorMap.get(Msg.valueOf(reqId))
                .process(Msg.Timeout, null, resultListener);
        if (null != resultListener){
            resultListener.onResponse(ResultCode.TIMEOUT, null);
        }
    }

    @Override
    public void onFeedbackNtf(String ntfId, Object ntfContent) {
        ntfProcessorMap.get(Msg.valueOf(ntfId))
                .process(Msg.valueOf(ntfId), ntfContent, ntfListeners.get(ntfId));
    }

    private static class RequestBundle{
        private Msg reqId;
        private IResultListener resultListener;
        RequestBundle(Msg reqId, IResultListener resultListener){
            this.reqId = reqId;
            this.resultListener = resultListener;
        }
    }
}
