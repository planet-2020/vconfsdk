package com.kedacom.vconf.sdk.base;

import com.kedacom.vconf.sdk.annotation.Request;
import com.kedacom.vconf.sdk.annotation.Response;
import com.kedacom.vconf.sdk.base.basement.CommandFairy;
import com.kedacom.vconf.sdk.base.basement.CrystalBall;
import com.kedacom.vconf.sdk.base.basement.FakeCrystalBall;
import com.kedacom.vconf.sdk.base.basement.ICrystalBall;
import com.kedacom.vconf.sdk.base.basement.IFairy;
import com.kedacom.vconf.sdk.base.basement.MagicBook;
import com.kedacom.vconf.sdk.base.basement.NotificationFairy;
import com.kedacom.vconf.sdk.base.basement.SessionFairy;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public abstract class Caster implements IFairy.ISessionFairy.IListener,
        IFairy.INotificationFairy.IListener{

    static {
        KLog.p("\n========================================" +
                        "\n======== Caster version=%s, timestamp=%s" +
                        "\n========================================",
                BuildConfig.ARTIFACT_VERSION, BuildConfig.TIMESTAMP);
    }

    private IFairy.ISessionFairy sessionFairy;
    private IFairy.INotificationFairy notificationFairy;
    private IFairy.ICommandFairy commandFairy;
    private ICrystalBall crystalBall;

    /**
     * 会话和通知处理的优先级定义，越小优先级越高。
     * 保证会话先于通知处理。
     * */
    private static int count = 0;
    private static final int SESSION_FAIRY_BASE_PRIORITY = -10000;
    private static final int NOTIFICATION_FAIRY_BASE_PRIORITY = SESSION_FAIRY_BASE_PRIORITY+10000;

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
            delRspListener(listener);
            delNtfListener(null, listener);
        }
    };


    @SuppressWarnings("ConstantConditions")
    protected Caster(){
        ++count;
        sessionFairy = SessionFairy.instance();
        notificationFairy = NotificationFairy.instance();
        commandFairy = CommandFairy.instance();
        crystalBall = CrystalBall.instance();

        crystalBall.addListener(sessionFairy, SESSION_FAIRY_BASE_PRIORITY+count);
        crystalBall.addListener(notificationFairy, NOTIFICATION_FAIRY_BASE_PRIORITY+count);

        sessionFairy.setCrystalBall(crystalBall);
        notificationFairy.setCrystalBall(crystalBall);
        commandFairy.setCrystalBall(crystalBall);

        listenerLifecycleObserver = new ListenerLifecycleObserver(listenerLifecycleCallback);

        Map<Msg, RspProcessor> rspProcessorMap = rspProcessors();
        if (null != rspProcessorMap) {
            this.rspProcessorMap.putAll(rspProcessorMap);
        }

        Map<Msg[], RspProcessor> rspsProcessorMap = rspsProcessors();
        if (null != rspsProcessorMap){
            for (Msg[] reqs : rspsProcessorMap.keySet()){
                RspProcessor rspProcessor = rspsProcessorMap.get(reqs);
                for (Msg req : reqs) {
                    this.rspProcessorMap.put(req, rspProcessor);
                }
            }
        }

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


    /**注册请求对应的响应处理器
     * NOTE @return 返回的Map中的Msg为Request*/
    protected Map<Msg, RspProcessor> rspProcessors(){return null;}

    /**批量注册请求对应的响应处理器*/
    protected Map<Msg[], RspProcessor> rspsProcessors(){return null;}

    /**注册通知处理器*/
    protected Map<Msg, NtfProcessor> ntfProcessors(){return null;}

    /**批量注册通知处理器*/
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
     * 启用/停用模拟器。
     * 若启用则模拟器将替代底层组件并反馈模拟的响应/通知；
     * 若停用则恢复正常模式，即请求通过底层组件发给平台平台反馈消息组件再上抛消息。
     * NOTE: 仅用于本地调试。
     * TODO, XXX: Fairy不要使用单例模式，这样可同时存在不同的Fairy*CrystalBall组合方式，
     * 进而可针对单独的模块启用模拟模式而其他模块使用正常模式。
     * @param bEnable true：启用，false：停用。
     * */
    public void enableSimulator(boolean bEnable){
        if (!SimulatorOnOff.on){
            KLog.p(KLog.ERROR, "forbidden operation");
            return;
        }
        crystalBall.delListener(sessionFairy);
        crystalBall.delListener(notificationFairy);
        if (bEnable){
            KLog.p(KLog.WARN, "switch to FakeCrystalBall");
            crystalBall = FakeCrystalBall.instance();
        }else{
            crystalBall = CrystalBall.instance();
        }

        crystalBall.addListener(notificationFairy, NOTIFICATION_FAIRY_BASE_PRIORITY+count);
        crystalBall.addListener(sessionFairy, SESSION_FAIRY_BASE_PRIORITY+count);

        sessionFairy.setCrystalBall(crystalBall);
        notificationFairy.setCrystalBall(crystalBall);
        commandFairy.setCrystalBall(crystalBall);
    }




    /**
     * 会话请求。
     * 该接口是异步的，请求结果会在适当的时机通过{@link #req#rspListener}反馈。
     * @param req 请求消息 参考{@link Msg} and {@link com.kedacom.vconf.sdk.annotation.Request}
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
     * 取消会话请求。
     * @param req 请求消息
     * @param rspListener 监听者，可能为null（对应的req时传入的是null）
     * 若同样的请求id同样的响应监听者请求了多次，则取消的是最早的请求。*/
    protected synchronized void cancelReq(Msg req, IResultListener rspListener){
        if (null == req){
            return;
        }
        if (!rspProcessorMap.keySet().contains(req)){
            KLog.p(KLog.ERROR, "%s is not in 'cared-req-list'", req);
            return;
        }

        for (Map.Entry<Integer, ReqBundle> entry : rspListeners.entrySet()){
            int reqSn = entry.getKey();
            ReqBundle value = entry.getValue();
            if (req == value.req
                    && rspListener==value.listener){
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
        if (null==ntfId || null == ntfListener){
            return;
        }

        if (!ntfProcessorMap.keySet().contains(ntfId)){
            KLog.p(KLog.ERROR, "%s is not in 'cared-ntf-list'", ntfId);
            return;
        }

        ntfListeners.get(ntfId.name()).add(ntfListener);

        listenerLifecycleObserver.tryObserve(ntfListener);
    }

    /**
     * 批量订阅通知
     * */
    protected synchronized void subscribe(Msg[] ntfIds, Object ntfListener){
        if (null == ntfIds || null == ntfListener){
            return;
        }
        boolean bSuccess = false;
        for (Msg ntfId : ntfIds){
            if (!ntfProcessorMap.keySet().contains(ntfId)){
                KLog.p(KLog.ERROR, "%s is not in 'cared-ntf-list'", ntfId);
                continue;
            }
            ntfListeners.get(ntfId.name()).add(ntfListener);
            bSuccess = true;
        }

        if (bSuccess) {
            listenerLifecycleObserver.tryObserve(ntfListener);
        }
    }

    /**
     * 取消订阅通知
     * */
    protected synchronized void unsubscribe(Msg ntfId, Object ntfListener){
        if (null == ntfListener){
            return;
        }

        if (delNtfListener(new Msg[]{ntfId}, ntfListener)){
            listenerLifecycleObserver.unobserve(ntfListener);
        }
    }

    /**
     * 批量取消订阅通知
     * */
    protected synchronized void unsubscribe(Msg[] ntfIds, Object ntfListener){
        if (null==ntfIds || null == ntfListener){
            return;
        }

        if (delNtfListener(ntfIds, ntfListener)){
            listenerLifecycleObserver.unobserve(ntfListener);
        }
    }

    /**
     * 取消所有订阅的通知
     * */
    protected synchronized void unsubscribe(Object ntfListener){
        if (null == ntfListener){
            return;
        }
        if (delNtfListener(null, ntfListener)){
            listenerLifecycleObserver.unobserve(ntfListener);
        }
    }

    /**
     * 设置请求。
     * 用于设置配置，或者其它需要同步执行native方法的场景。
     * 该接口是同步的，若下层natiev方法实现耗时则调用者被阻塞，设置结果在方法返回时即生效。
     * @param set 请求消息。参考{@link Msg} and {@link Request#SET}
     * @param paras 参数。
     * */
    protected void set(Msg set, Object... paras){
        commandFairy.set(set.name(), paras);
    }

    /**
     * 获取（配置）请求
     * 该接口是同步的，若下层natiev方法实现耗时则调用者被阻塞，请求结果通过返回值反馈给调用者。
     * @param get 请求消息。参考{@link Msg} and {@link Request#GET}
     * @param paras 参数。可以为空。
     * @return 请求结果。
     * */
    protected Object get(Msg get, Object... paras){
        return commandFairy.get(get.name(), paras);
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
     * （驱使下层）发射响应/通知。仅用于模拟模式。
     * */
    public synchronized void eject(Msg msg){
        String msgId = MagicBook.instance().getMsgId(msg.name());
        crystalBall.emit(msgId);
    }

    /**
     * （驱使下层）发射响应/通知。仅用于模拟模式。
     * */
    public synchronized void eject(Msg[] msgs){
        String[] msgIds = new String[msgs.length];
        for (int i=0; i<msgIds.length; ++i) {
            msgIds[i] = MagicBook.instance().getMsgId(msgs[i].name());
        }
        crystalBall.emit(msgIds);
    }


    /**
     * 删除监听者。
     * */
    public synchronized void delListener(Object listener){
        if (null == listener){
            return;
        }
        boolean bDelRspSuccess = delRspListener(listener);
        boolean bDelNtfSuccess = delNtfListener(null, listener);
        if (bDelRspSuccess || bDelNtfSuccess){
            listenerLifecycleObserver.unobserve(listener);
        }
    }


    protected synchronized boolean delRspListener(Object rspListener){
        boolean bSuccess = false;
        for (Map.Entry<Integer, ReqBundle> entry: rspListeners.entrySet()){
            int key = entry.getKey();
            ReqBundle val = entry.getValue();
            if (rspListener.equals(val.listener)){
//                KLog.p("delRspListener reqSn=%s, req=%s, listener=%s", key, val.req, val.listener);
                val.listener = null;  // 保留会话，仅删除监听器
                bSuccess = true;
            }
        }
        return bSuccess;
    }

    private synchronized boolean delNtfListener(Msg[] ntfIds, Object ntfListener){
        boolean bSuccess = false;
        if (null != ntfIds) {
            for (Msg ntfId : ntfIds) {
                Set<Object> listeners = ntfListeners.get(ntfId.name());
                if (null != listeners) bSuccess = listeners.remove(ntfListener);
            }
        }else{
            for (String ntfName : ntfListeners.keySet()) {
                Set<Object> listeners = ntfListeners.get(ntfName);
                if (null != listeners) bSuccess = listeners.remove(ntfListener);
            }
        }
        return bSuccess;
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
        if (!bConsumed){
            // 超时未被消费则此处通知用户超时
            reportTimeout(resultListener);
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

    /**
     * 上报用户请求成功结果
     * */
    protected void reportSuccess(Object result, IResultListener listener){
        if (null != listener){
            listener.onArrive(true);
            listener.onSuccess(result);
        }
    }

    /**
     * 上报用户请求失败
     * */
    protected void reportFailed(int errorCode, IResultListener listener){
        if (null != listener){
            listener.onArrive(false);
            listener.onFailed(errorCode);
        }
    }

    /**
     * 上报用户请求超时
     * */
    protected void reportTimeout(IResultListener listener){
        if (null != listener){
            listener.onArrive(false);
            listener.onTimeout();
        }
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
