package com.kedacom.vconf.sdk.base;

import com.kedacom.vconf.sdk.base.basement.Witch;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public abstract class RequestAgent implements Witch.IOnFeedbackListener{

    private int reqSn; // 请求序列号，唯一标识一次请求。
    private final Map<Integer, RequestBundle> rspListeners;
    private final Map<String, Set<Object>> ntfListeners;

    private Map<Msg, RspProcessor> rspProcessorMap;
    private Map<Msg, NtfProcessor> ntfProcessorMap;

    private Witch witch;

    private ListenerLifecycleObserver listenerLifecycleObserver;
    private ListenerLifecycleObserver.Callback listenerLifecycleCallback = new ListenerLifecycleObserver.Callback(){
        @Override
        public void onListenerResumed(Object listener) { // 该事件是粘滞的，即便activity已经resume很久了，然后才注册生命周期观察者也会收到该事件。
//            KLog.p(""+ listener);
        }

        @Override
        public void onListenerPause(Object listener) {
//            KLog.p(""+ listener);
//            delListener(listener);
        }

        @Override
        public void onListenerStop(Object listener) {

        }

        @Override
        public void onListenerDestroy(Object listener) {
            KLog.p(""+ listener);
            delListener(listener);
        }
    };

    protected RequestAgent(){
        witch = new Witch();
        witch.setOnFeedbackListener(this);

        listenerLifecycleObserver = new ListenerLifecycleObserver(listenerLifecycleCallback);

        reqSn = 0;
        rspListeners = new HashMap<>();
        ntfListeners = new HashMap<>();

        rspProcessorMap = rspProcessors();
        if (null == rspProcessorMap){
            rspProcessorMap = new HashMap<>();
        }

        Map<Msg, NtfProcessor> ntfProcessorMap = ntfProcessors();
        String ntfName;
        if (null != ntfProcessorMap){
            for (Msg ntf : ntfProcessorMap.keySet()){
                ntfName = ntf.name();
                witch.subscribe(ntfName);
                ntfListeners.put(ntfName, new HashSet<>());
            }
            this.ntfProcessorMap = ntfProcessorMap;
        }

        Map<Msg[], NtfProcessor> ntfsProcessorMap = ntfsProcessors();
        if (null != ntfsProcessorMap){
            if (null == this.ntfProcessorMap){
                this.ntfProcessorMap = new HashMap<>();
            }
            NtfProcessor ntfProcessor;
            for (Msg[] ntfs : ntfsProcessorMap.keySet()){
                ntfProcessor = ntfsProcessorMap.get(ntfs);
                for (Msg ntf : ntfs){
                    this.ntfProcessorMap.put(ntf, ntfProcessor);
                    ntfName = ntf.name();
                    witch.subscribe(ntfName);
                    ntfListeners.put(ntfName, new HashSet<>());
                }
            }
        }

        if (null == this.ntfProcessorMap){
            this.ntfProcessorMap = new HashMap<>();
        }
    }


    /**注册：请求——请求对应的响应处理器。
     * 子类如有请求，则需重写该方法。*/
    protected Map<Msg, RspProcessor> rspProcessors(){return null;}

    /**注册：通知——通知处理器
     * 子类如需要订阅通知，则需重写该方法和{@link this#ntfsProcessors()}中的一个或者全部。*/
    protected Map<Msg, NtfProcessor> ntfProcessors(){return null;}

    /**注册：通知列表——通知处理器
     * 子类如需要订阅通知，则需重写该方法和{@link this#ntfProcessors()}中的一个或者全部。*/
    protected Map<Msg[], NtfProcessor> ntfsProcessors(){return null;}

    /**响应处理器*/
    protected interface RspProcessor{
        void process(Msg rspId, Object rspContent, IResultListener listener);
    }

    /**通知处理器*/
    protected interface NtfProcessor{
        void process(Msg ntfId, Object ntfContent, Set<Object> listeners);
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

        if (!witch.req(reqId.name(), ++reqSn, reqPara)){
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
            witch.cancelReq(earliestReq);
        }

    }

    /**
     * 订阅通知
     * */
    protected synchronized void subscribe(Msg ntfId, Object ntfListener){
//        Log.i(TAG, String.format("ntfListener=%s, ntfId=%s", ntfListener, ntfId));
        if (null == ntfListener){
            return;
        }

        if (!ntfProcessorMap.keySet().contains(ntfId)){
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
     * 订阅通知
     * */
    protected synchronized void subscribe(Msg[] ntfIds, Object ntfListener){
        if (null == ntfIds){
            return;
        }
        for (Msg ntfId : ntfIds){
            subscribe(ntfId, ntfListener);
        }
    }

    /**
     * 取消订阅通知
     * */
    protected synchronized void unsubscribe(Msg ntfId, Object ntfListener){
        if (null == ntfListener){
            return;
        }

        String ntfName = ntfId.name();
        Set<Object> listeners = ntfListeners.get(ntfName);
        if (null != listeners){
            listeners.remove(ntfListener);
        }
    }

    protected Set<Object> getNtfListeners(Msg ntfId){
        return ntfListeners.get(ntfId.name());
    }

    /**
     * （驱使下层）发射通知。仅用于模拟模式。
     * */
    protected synchronized void eject(Msg ntfId){
//        Log.i(TAG, "eject ntf "+ntfId);
        if (!ntfProcessorMap.keySet().contains(ntfId)){
            KLog.p(KLog.ERROR, "%s is not in 'cared-ntf-list'", ntfId);
            return;
        }
        witch.eject(ntfId.name());
    }

    /**
     * （驱使下层）发射通知。仅用于模拟模式。
     * */
    protected synchronized void eject(Msg[] ntfIds){
        String[] ntfs = new String[ntfIds.length];
        for (int i=0; i<ntfs.length; ++i) {
            if (!ntfProcessorMap.keySet().contains(ntfIds[i])) {
                KLog.p(KLog.ERROR, "%s is not in 'cared-ntf-list'", ntfIds[i]);
                return;
            }
            ntfs[i]=ntfIds[i].name();
        }
        witch.eject(ntfs);
    }

    /**
     * 设置配置
     * */
    protected void set(Msg setId, Object para){
        witch.set(setId.name(), para);
    }

    /**
     * 获取配置
     * */
    protected Object get(Msg getId){
        return witch.get(getId.name());
    }

    protected Object get(Msg getId, Object para){
        return witch.get(getId.name(), para);
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
            resultListener.onTimeout(); // TODO 如需子类控制该上报行为则可让process返回boolean表示是否处理完，没处理完则此处继续处理
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
