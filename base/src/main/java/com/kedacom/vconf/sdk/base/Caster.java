package com.kedacom.vconf.sdk.base;

import com.kedacom.vconf.sdk.annotation.Response;
import com.kedacom.vconf.sdk.base.basement.CrystalBall;
import com.kedacom.vconf.sdk.base.basement.FakeCrystalBall;
import com.kedacom.vconf.sdk.base.basement.ICrystalBall;
import com.kedacom.vconf.sdk.base.basement.IFairy;
import com.kedacom.vconf.sdk.base.basement.NotificationFairy;
import com.kedacom.vconf.sdk.base.basement.SessionFairy;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public abstract class Caster implements IFairy.ISessionFairy.IListener,
        IFairy.INotificationFairy.IListener{

    private IFairy.ISessionFairy sessionFairy;
    private IFairy.INotificationFairy notificationFairy;

    private int reqSn = 0; // 请求序列号，递增。
    private final Map<Integer, ReqBundle> rspListeners = new LinkedHashMap<>();
    private final Map<String, LinkedHashSet<Object>> ntfListeners = new LinkedHashMap<>();

    private Map<Msg, RspProcessor> rspProcessorMap = new LinkedHashMap<>();
    private Map<Msg, NtfProcessor> ntfProcessorMap = new LinkedHashMap<>();

    private ListenerLifecycleObserver listenerLifecycleObserver;
    private ListenerLifecycleObserver.Callback listenerLifecycleCallback = new ListenerLifecycleObserver.Callback(){
        @Override
        public void onListenerResumed(Object listener) { // 该事件是粘滞的，即便activity已经resume很久了然后才注册生命周期观察者也会收到该事件。
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
    protected Caster(){
        IFairy.ISessionFairy sessionFairy = SessionFairy.instance();
        IFairy.INotificationFairy notificationFairy = NotificationFairy.instance();
        ICrystalBall crystalBall = CrystalBall.instance();
        crystalBall.addRspListener(sessionFairy);
        crystalBall.addNtfListener(notificationFairy);
        sessionFairy.setCrystalBall(crystalBall);
        notificationFairy.setCrystalBall(crystalBall);
        this.sessionFairy = sessionFairy;
        this.notificationFairy = notificationFairy;

        listenerLifecycleObserver = new ListenerLifecycleObserver(listenerLifecycleCallback);

        rspProcessorMap.putAll(rspProcessors());

        Map<Msg, NtfProcessor> ntfProcessorMap = ntfProcessors();
        if (null != ntfProcessorMap){
            for (Msg ntf : ntfProcessorMap.keySet()){
                String ntfName = ntf.name();
                if(notificationFairy.subscribe(this, ntfName)){
                    ntfListeners.put(ntfName, new LinkedHashSet<>());
                    this.ntfProcessorMap.put(ntf, ntfProcessorMap.get(ntf));
                }
            }
        }

        Map<Msg[], NtfProcessor> ntfsProcessorMap = ntfsProcessors();
        if (null != ntfsProcessorMap){
            for (Msg[] ntfs : ntfsProcessorMap.keySet()){
                NtfProcessor ntfProcessor = ntfsProcessorMap.get(ntfs);
                for (Msg ntf : ntfs){
                    String ntfName = ntf.name();
                    if (notificationFairy.subscribe(this, ntfName)) {
                        ntfListeners.put(ntfName, new LinkedHashSet<>());
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
         * @param rsp 响应ID
         * @param rspContent 响应内容，具体类型由响应ID决定，参考{@link Response#clz()}。
         * @param listener 响应监听器，由{@link #req(Msg, IResultListener, Object...)}传入。
         *                 NOTE：可能在会话过程中监听器被销毁，如调用了{@link #delListener(Object)}或者监听器绑定的生命周期对象已销毁，
         *                 则此参数为null，（当然也可能调用{@link #req(Msg, IResultListener, Object...)}时传入的就是null）
         *                 所以使用者需对该参数做非null判断。
         * @param req 请求ID，由{@link #req(Msg, IResultListener, Object...)}传入。
         * @param reqParas 请求参数列表，由{@link #req(Msg, IResultListener, Object...)}传入，顺序同传入时的
         * @return true，若该响应已被处理；否则false。
         * */
        boolean process(Msg rsp, Object rspContent, IResultListener listener, Msg req, Object[] reqParas);
    }

    /**通知处理器*/
    protected interface NtfProcessor{
        /**
         * @param ntf 通知ID
         * @param ntfContent 通知内容，具体类型由通知ID决定，参考{@link Response#clz()}。
         * @param listeners 通知监听器，由{@link #subscribe(Msg, Object)}和{@link #subscribe(Msg[], Object)}传入。  */
        void process(Msg ntf, Object ntfContent, Set<Object> listeners);
    }


    /**
     * 启用/停用仿真器。
     * 若启用则仿真器将替代底层组件并反馈模拟的响应/通知；
     * 若停用则恢复正常模式，即请求通过底层组件发给平台平台反馈消息组件再上抛消息。
     * NOTE: 仅用于DEBUG版本。
     * @param bEnable true：启用，false：停用。
     * */
    public void enableSimulator(boolean bEnable){
        if (!SimulatorOnOff.on){
            KLog.p(KLog.ERROR, "forbidden operation");
            return;
        }
        ICrystalBall crystalBall;
        if (bEnable){
            KLog.p(KLog.WARN, "switch to FakeCrystalBall");
            crystalBall = FakeCrystalBall.instance();
        }else{
            crystalBall = CrystalBall.instance();
        }
        crystalBall.addRspListener(sessionFairy);
        crystalBall.addNtfListener(notificationFairy);
        sessionFairy.getCrystalBall().delListener(sessionFairy);
        notificationFairy.getCrystalBall().delListener(notificationFairy);
        sessionFairy.setCrystalBall(crystalBall);
        notificationFairy.setCrystalBall(crystalBall);
    }




    /**
     * 发送请求。
     * @param req 请求ID 参考{@link Msg} and {@link com.kedacom.vconf.sdk.annotation.Request}
     * @param rspListener 响应监听者。可以为null表示请求者不关注请求结果
     * @param reqPara 请求参数列表，可以没有。  */
    protected synchronized void req(Msg req, IResultListener rspListener, Object... reqPara){

        if (!rspProcessorMap.keySet().contains(req)){
            KLog.p(KLog.ERROR, "%s is not in 'cared-req-list'", req);
            return;
        }

        if (!sessionFairy.req(this, req.name(), ++reqSn, reqPara)){
            KLog.p(KLog.ERROR, "req failed");
            return;
        }

        KLog.p("req=%s, reqSn=%s, listener=%s", req, reqSn, rspListener);

        listenerLifecycleObserver.tryObserve(rspListener);

        rspListeners.put(reqSn, new ReqBundle(req, rspListener));

    }

    /**
     * 取消请求。
     * 若同样的请求id同样的响应监听者请求了多次，则取消的是最早的请求。*/
    protected synchronized void cancelReq(Msg req, IResultListener rspListener){
        if (null == req || null == rspListener){ // TODO 支持rspListener==null
            return;
        }
        if (!rspProcessorMap.keySet().contains(req)){
            KLog.p(KLog.ERROR, "%s is not in 'cared-req-list'", req);
            return;
        }

        for (Map.Entry<Integer, ReqBundle> entry : rspListeners.entrySet()){
            int reqSn = entry.getKey();
            ReqBundle value = entry.getValue();
            if (req.equals(value.req)
                    && rspListener.equals(value.listener)){
                KLog.p("cancel reqSn=%s, req=%s, listener=%s", reqSn, value.req, value.listener);
                sessionFairy.cancelReq(reqSn);
                rspListeners.remove(reqSn);
                listenerLifecycleObserver.unobserve(rspListener);
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
//            KLog.p("unsubscribe ntf=%s, listener=%s", ntfId, ntfListener);
            Object listener = listeners.remove(ntfListener);
            listenerLifecycleObserver.unobserve(listener);
        }
    }

    protected Set<Object> getNtfListeners(Msg ntfId){
        return ntfListeners.get(ntfId.name());
    }

    protected boolean containsNtfListener(Object ntfListener){ // TODO 改为containsNtfListener(Msg ntf, Object ntfListener)
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

    protected boolean containsRspListener(Object rspListener){ // TODO 改为containsNtfListener(Msg rsp, Object rspListener)
        if (null == rspListener){
            return false;
        }
        for (ReqBundle val: rspListeners.values()){
            if (rspListener.equals(val.listener)){
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

        for (Map.Entry<Integer, ReqBundle> entry: rspListeners.entrySet()){
            int key = entry.getKey();
            ReqBundle val = entry.getValue();
            if (rspListener.equals(val.listener)){
//                KLog.p("delRspListener reqSn=%s, req=%s, listener=%s", key, val.req, val.listener);
                val.listener = null;
            }
        }

        listenerLifecycleObserver.unobserve(rspListener);
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
    public boolean onRsp(boolean bLast, String rspName, Object rspContent, String reqName, int reqSn, Object[] reqParas) {
        Msg req = Msg.valueOf(reqName);
        Msg rsp = Msg.valueOf(rspName);
        IResultListener resultListener = rspListeners.get(reqSn).listener;
        StringBuffer sb = new StringBuffer();
        for (Object para : reqParas){
            sb.append(para).append("; ");
        }
        KLog.p("rsp=%s, rspContent=%s, resultListener=%s, req=%s, reqSn=%s, \nreqParas=%s", rsp, rspContent, resultListener, reqName, reqSn, sb);
        boolean bConsumed = rspProcessorMap.get(req).process(rsp, rspContent, resultListener, req, reqParas);
        if (bConsumed){
            if (bLast){
                rspListeners.remove(reqSn);
                listenerLifecycleObserver.unobserve(resultListener);
            }
        }else{
            KLog.p(KLog.WARN, "rsp %s not consumed, req=%s, reqSn=%s", rsp, reqName, reqSn);
        }
        return bConsumed;
    }

    @Override
    public void onTimeout(String reqName, int reqSn, Object[] reqParas) {
        Msg req = Msg.valueOf(reqName);
        IResultListener resultListener = rspListeners.remove(reqSn).listener;
        listenerLifecycleObserver.unobserve(resultListener);
        StringBuffer sb = new StringBuffer();
        for (Object para : reqParas){
            sb.append(para).append("; ");
        }
        KLog.p("rsp=%s, resultListener=%s, \nreq=%s, reqSn=%s, reqParas=%s", Msg.Timeout, resultListener, reqName, reqSn, sb);
        boolean bConsumed = rspProcessorMap.get(req).process(Msg.Timeout, "", resultListener, req, reqParas);
        if (!bConsumed && null != resultListener){
            // 超时未被消费则此处通知用户超时
            resultListener.onTimeout();
        }
    }

    @Override
    public void onFinDueToNoRsp(String reqName, int reqSn, Object[] reqParas) {
        ReqBundle reqBundle = rspListeners.remove(reqSn);
        listenerLifecycleObserver.unobserve(reqBundle.listener);
    }

    @Override
    public void onNtf(String ntfName, Object ntfContent) {
        Set<Object> listeners = ntfListeners.get(ntfName);
        StringBuffer sb = new StringBuffer();
        for (Object listener : listeners){
            sb.append(listener).append("; ");
        }
        Msg ntf = Msg.valueOf(ntfName);
        KLog.p("ntfId=%s, ntfContent=%s, listeners=%s", ntf, ntfContent, sb);
        ntfProcessorMap.get(ntf).process(ntf, ntfContent, listeners);
    }


    private static class ReqBundle{
        private Msg req;
        private IResultListener listener;

        public ReqBundle(Msg req, IResultListener listener) {
            this.req = req;
            this.listener = listener;
        }
    }

}
