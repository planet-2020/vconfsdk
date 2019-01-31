package com.kedacom.vconf.sdk.base;

import com.kedacom.vconf.sdk.annotation.Notification;
import com.kedacom.vconf.sdk.annotation.Response;
import com.kedacom.vconf.sdk.base.basement.IFairy2;
import com.kedacom.vconf.sdk.base.basement.Witch;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;

public abstract class Caster implements IFairy2.ISessionFairy.IListener,
        IFairy2.INotificationFairy.IListener{

    private IFairy2.ISessionFairy sessionFairy;
    private IFairy2.INotificationFairy notificationFairy;

    private int reqSn = 0; // 请求序列号，递增。
    private final Map<ReqKey, RequestBundle> rspListeners = new LinkedHashMap<>();
    private final Map<String, LinkedHashSet<Object>> ntfListeners = new LinkedHashMap<>();

    private Map<Msg, RspProcessor> rspProcessorMap = new LinkedHashMap<>();
    private Map<Msg, NtfProcessor> ntfProcessorMap = new LinkedHashMap<>();

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

    @SuppressWarnings("ConstantConditions")
    protected Caster(@NonNull IFairy2.ISessionFairy sessionFairy, @NonNull IFairy2.INotificationFairy notificationFairy){

        this.sessionFairy = sessionFairy;
        this.notificationFairy = notificationFairy;

        listenerLifecycleObserver = new ListenerLifecycleObserver(listenerLifecycleCallback);

        rspProcessorMap.putAll(rspProcessors());

        Map<Msg, NtfProcessor> ntfProcessorMap = ntfProcessors();
        if (null != ntfProcessorMap){
            for (Msg ntf : ntfProcessorMap.keySet()){
                String ntfId = ntf.name();
                if(notificationFairy.subscribe(this, ntfId)){
                    ntfListeners.put(ntfId, new LinkedHashSet<>());
                    this.ntfProcessorMap.put(ntf, ntfProcessorMap.get(ntf));
                }
            }
        }

        Map<Msg[], NtfProcessor> ntfsProcessorMap = ntfsProcessors();
        if (null != ntfsProcessorMap){
            for (Msg[] ntfs : ntfsProcessorMap.keySet()){
                NtfProcessor ntfProcessor = ntfsProcessorMap.get(ntfs);
                for (Msg ntf : ntfs){
                    String ntfId = ntf.name();
                    if (notificationFairy.subscribe(this, ntfId)) {
                        ntfListeners.put(ntfId, new LinkedHashSet<>());
                        this.ntfProcessorMap.put(ntf, ntfProcessor);
                    }
                }
            }
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
        /**
         * @param rspId 响应ID
         * @param rspContent 响应内容，具体类型由响应ID决定，参考{@link Response#clz()}。
         * @param listener 响应监听器，由{@link #req(Msg, IResultListener, Object...)}传入。
         *                 NOTE：可能在会话过程中监听器被销毁，如调用了{@link #delListener(Object)}或者监听器绑定的生命周期对象已销毁，
         *                 则此参数为null，（当然也可能调用{@link #req(Msg, IResultListener, Object...)}时传入的就是null）
         *                 所以使用者需对该参数做非null判断。
         * @param reqId 请求ID，由{@link #req(Msg, IResultListener, Object...)}传入。
         * @param reqParas 请求参数列表，由{@link #req(Msg, IResultListener, Object...)}传入，顺序同传入时的
         * @return true，若该响应已被处理；否则false。
         * */
        boolean process(Msg rspId, Object rspContent, IResultListener listener, Msg reqId, Object[] reqParas);
    }

    /**通知处理器*/
    protected interface NtfProcessor{
        /**
         * @param ntfId 通知ID
         * @param ntfContent 通知内容，具体类型由通知ID决定，参考{@link Notification#clz()}。
         * @param listeners 通知监听器，由{@link #subscribe(Msg, Object)}和{@link #subscribe(Msg[], Object)}传入。
         * */
        void process(Msg ntfId, Object ntfContent, Set<Object> listeners);
    }


    /**
     * 发送请求。
     * @param reqId 请求ID 参考{@link Msg} and {@link com.kedacom.vconf.sdk.annotation.Request}
     * @param rspListener 响应监听者。可以为null表示请求者不关注请求结果
     * @param reqPara 请求参数列表，可以没有。
     * */
    protected synchronized void req(Msg reqId, IResultListener rspListener, Object... reqPara){
//        Log.i(TAG, String.format("rspListener=%s, reqId=%s, para=%s", rspListener, reqId, para));

        if (!rspProcessorMap.keySet().contains(reqId)){
            KLog.p(KLog.ERROR, "%s is not in 'cared-req-list'", reqId);
            return;
        }

        listenerLifecycleObserver.tryObserve(rspListener);

        if (!sessionFairy.req(this, reqId.name(), ++reqSn, reqPara)){
            return;
        }

        rspListeners.put(new ReqKey(reqId, reqSn), new RequestBundle(rspListener)); // TODO RequestBundle能否去掉

    }

    /**
     * 取消请求。
     * 若同样的请求id同样的响应监听者请求了多次，则取消的是最早的请求。*/
    protected synchronized void cancelReq(Msg reqId, IResultListener rspListener){
        if (null == reqId || null == rspListener){ // TODO 支持rspListener==null
            return;
        }
        if (!rspProcessorMap.keySet().contains(reqId)){
            KLog.p(KLog.ERROR, "%s is not in 'cared-req-list'", reqId);
            return;
        }

        for (Map.Entry<ReqKey, RequestBundle> entry : rspListeners.entrySet()){
            ReqKey reqKey = entry.getKey();
            RequestBundle bundle = entry.getValue();
            if (reqId.equals(reqKey.reqId)
                    && rspListener.equals(bundle.resultListener)){
                sessionFairy.cancelReq(reqKey.reqId.name(), reqKey.reqSn);
                break;
            }
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

        listenerLifecycleObserver.tryObserve(ntfListener);

        ntfListeners.get(ntfId.name()).add(ntfListener);
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

    protected boolean containsNtfListener(Object ntfListener){
        if (null == ntfListener){
            return false;
        }
        for (Set<Object> listeners : ntfListeners.values()){
            if (listeners.contains(ntfListener)){
                return true;
            }
        }

        return false;
    }

    protected boolean containsRspListener(Object rspListener){
        if (null == rspListener){
            return false;
        }
        for (RequestBundle bundle: rspListeners.values()){
            if (rspListener.equals(bundle.resultListener)){
                return true;
            }
        }
        return false;
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
        notificationFairy.emit(ntfId.name());
    }

    /**
     * （驱使下层）发射通知。仅用于模拟模式。
     * */
    protected synchronized void eject(Msg[] ntfIds){
        for (Msg ntf : ntfIds) {
            eject(ntf);
        }
    }

//    /**
//     * 设置配置
//     * */
//    protected void set(Msg setId, Object para){
//        witch.set(setId.name(), para);
//    }
//
//    /**
//     * 获取配置
//     * */
//    protected Object get(Msg getId){
//        return witch.get(getId.name());
//    }
//
//    protected Object get(Msg getId, Object para){
//        return witch.get(getId.name(), para);
//    }

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

        for (RequestBundle bundle : rspListeners.values()){
            if (rspListener.equals(bundle.resultListener)){
                bundle.resultListener = null;
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
    public boolean onRsp(boolean bLast, String rspId, Object rspContent, String reqId, int reqSn, Object[] reqParas) {
        Msg req = Msg.valueOf(reqId);
        Msg rsp = Msg.valueOf(rspId);
        RequestBundle requestBundle;
        if (bLast) {
            requestBundle = rspListeners.remove(new ReqKey(req, reqSn));
        }else{
            requestBundle = rspListeners.get(new ReqKey(req, reqSn));
        }
        IResultListener resultListener = requestBundle.resultListener;
        if (!requestBundle.bResultArrived){
            requestBundle.bResultArrived = true;
            if(null != resultListener) resultListener.onArrive();
        }
        StringBuffer sb = new StringBuffer();
        for (Object para : reqParas){
            sb.append(para).append("; ");
        }
        KLog.p("rspId=%s, rspContent=%s, resultListener=%s, \nreqId=%s, reqSn=%s, reqParas=%s", rsp, rspContent, resultListener, reqId, reqSn, sb);
        return rspProcessorMap.get(req).process(rsp, rspContent, resultListener, req, reqParas);
    }

    @Override
    public void onTimeout(String reqId, int reqSn, Object[] reqParas) {
        Msg req = Msg.valueOf(reqId);
        RequestBundle requestBundle = rspListeners.remove((new ReqKey(req, reqSn)));
        IResultListener resultListener = requestBundle.resultListener;
        if (!requestBundle.bResultArrived){
            requestBundle.bResultArrived = true;
            if(null != resultListener) resultListener.onArrive();
        }
        StringBuffer sb = new StringBuffer();
        for (Object para : reqParas){
            sb.append(para).append("; ");
        }
        KLog.p("rspId=%s, resultListener=%s, \nreqId=%s, reqSn=%s, reqParas=%s", Msg.Timeout, resultListener, reqId, reqSn, sb);
        boolean bConsumed = rspProcessorMap.get(req).process(Msg.Timeout, "", resultListener, req, reqParas);
        if (!bConsumed && null != resultListener){
            // 超时未被消费则此处通知用户超时
            resultListener.onTimeout();
        }
    }

    @Override
    public void onNtf(String ntfId, Object ntfContent) {
        Set<Object> listeners = ntfListeners.get(ntfId);
        StringBuffer sb = new StringBuffer();
        for (Object listener : listeners){
            sb.append(listener).append("; ");
        }
        Msg ntf = Msg.valueOf(ntfId);
        KLog.p("ntfId=%s, ntfContent=%s, listeners=%s", ntf, ntfContent, sb);
        ntfProcessorMap.get(ntf).process(ntf, ntfContent, listeners);
    }


    private static class ReqKey{
        private final Msg reqId;
        private final int reqSn;

        ReqKey(Msg reqId, int reqSn) {
            this.reqId = reqId;
            this.reqSn = reqSn;
        }
    }

    private static class RequestBundle{
        private IResultListener resultListener;
        private boolean bResultArrived = false;
        RequestBundle(IResultListener resultListener){
            this.resultListener = resultListener;
        }
    }
}
