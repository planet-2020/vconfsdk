package com.kedacom.vconf.sdk.amulet;

import com.kedacom.vconf.sdk.utils.lifecycle.ListenerLifecycleObserver;
import com.kedacom.vconf.sdk.utils.log.KLog;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"unchecked", "WeakerAccess", "unused"})
public abstract class Caster<T extends Enum<T>> implements
        IFairy.ISessionFairy.IListener,
        IFairy.INotificationFairy.IListener{

    static {
        KLog.p("\n=================================================================" +
                        "\n======== Caster version=%s, timestamp=%s" +
                        "\n===============================================================",
                BuildConfig.VERSION, BuildConfig.TIMESTAMP);
    }

    private IFairy.ISessionFairy sessionFairy = new SessionFairy();
    private IFairy.INotificationFairy notificationFairy = new NotificationFairy();
    private IFairy.ICommandFairy commandFairy = new CommandFairy();
    private ICrystalBall crystalBall = CrystalBall.instance();

    /**
     * 会话和通知处理的优先级定义，越小优先级越高。
     * 保证会话先于通知处理。
     * */
    private static int count = 0;
    private static final int SESSION_FAIRY_BASE_PRIORITY = 0;
    private static final int NOTIFICATION_FAIRY_BASE_PRIORITY = SESSION_FAIRY_BASE_PRIORITY+10000;

    private int reqSn = 0; // 请求序列号，递增。
    private final Map<Integer, ReqBundle> rspListeners = new LinkedHashMap<>();
    private final Map<String, LinkedHashSet<Object>> ntfListeners = new LinkedHashMap<>();

    private Map<T, RspProcessor<T>> rspProcessorMap = new LinkedHashMap<>();
    private Map<T, NtfProcessor<T>> ntfProcessorMap = new LinkedHashMap<>();

    private ListenerLifecycleObserver listenerLifecycleObserver;

    private Class<T> enumT;

    private String msgPrefix;

    @SuppressWarnings("ConstantConditions")
    protected Caster(){
        enumT = (Class<T>)((ParameterizedType)getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        try {
//            Class<?> buildConfigClz = Class.forName(getClass().getPackage().getName()+".BuildConfig"); //XXX BuildConfig在AndroidManifest所定义的package下，而非当前对象所在package。
//            Field versionField = buildConfigClz.getDeclaredField("VERSION");
//            Field timestampField = buildConfigClz.getDeclaredField("TIMESTAMP");
//            String version = (String) versionField.get(null);
//            String timestamp = (String) timestampField.get(null);
//            KLog.p("\n=================================================================" +
//                            "\n======== %s version=%s, timestamp=%s" +
//                            "\n================================================================",
//                    getClass().getSimpleName(), version, timestamp);

            Class<?> msgGenClz = Class.forName(enumT.getPackage().getName()+".Message$$Generated");
            MagicBook.instance().addChapter(msgGenClz);
            Field field = msgGenClz.getDeclaredField("module");
            field.setAccessible(true);
            msgPrefix = field.get(null)+"_";
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        if (null == msgPrefix){
            throw new RuntimeException("null == msgPrefix");
        }

        crystalBall.addListener(sessionFairy, SESSION_FAIRY_BASE_PRIORITY+count);
        crystalBall.addListener(notificationFairy, NOTIFICATION_FAIRY_BASE_PRIORITY+count);
        sessionFairy.setCrystalBall(crystalBall);
        commandFairy.setCrystalBall(crystalBall);
        ++count;

        listenerLifecycleObserver = new ListenerLifecycleObserver(new ListenerLifecycleObserver.Callback(){
            @Override
            public void onListenerResumed(Object listener) {
                // 该事件是粘滞的，即便activity已经resume很久了然后才注册生命周期观察者也会收到该事件。
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
        });

        Map<T[], RspProcessor<T>> rspsProcessorMap = rspsProcessors();
        if (null != rspsProcessorMap){
            for (T[] reqs : rspsProcessorMap.keySet()){
                RspProcessor rspProcessor = rspsProcessorMap.get(reqs);
                for (T req : reqs) {
                    this.rspProcessorMap.put(req, rspProcessor);
                }
            }
        }

        Map<T[], NtfProcessor<T>> ntfsProcessorMap = ntfsProcessors();
        if (null != ntfsProcessorMap){
            for (T[] ntfs : ntfsProcessorMap.keySet()){
                NtfProcessor ntfProcessor = ntfsProcessorMap.get(ntfs);
                for (T ntf : ntfs){
                    String ntfName = ntf.name();
                    if (notificationFairy.subscribe(this, prefixMsg(ntfName))) {
                        ntfListeners.put(ntfName, new LinkedHashSet<>());
                        this.ntfProcessorMap.put(ntf, ntfProcessor);
                    }
                }
            }
        }

    }


    /**注册请求对应的响应处理器*/
    protected abstract Map<T[], RspProcessor<T>> rspsProcessors();

    /**注册通知处理器*/
    protected abstract Map<T[], NtfProcessor<T>> ntfsProcessors();

    /**响应处理器*/
    protected interface RspProcessor<T>{
        /**
         * @param rsp 响应ID
         * @param rspContent 响应内容，具体类型由响应ID决定。
         * @param listener 响应监听器,req()传入。
         *                 NOTE：可能在会话过程中监听器被销毁，如调用了{@link #delListener(Object)}或者监听器绑定的生命周期对象已销毁，
         *                 则此参数为null，（当然也可能调用req()时传入的就是null）
         *                 所以使用者需对该参数做非null判断。
         * @param req 请求ID，req()传入。
         * @param reqParas 请求参数列表，req()传入，顺序同传入时的
         * @return true，若该响应已被处理；否则false。
         * */
        boolean process(T rsp, Object rspContent, IResultListener listener, T req, Object[] reqParas);
    }

    /**通知处理器*/
    protected interface NtfProcessor<T>{
        /**
         * @param ntf 通知ID
         * @param ntfContent 通知内容，具体类型由通知ID决定
         * @param listeners 通知监听器，由{@link #subscribe(Enum, Object)}和{@link #subscribe(Enum[], Object)}传入。*/
        void process(T ntf, Object ntfContent, Set<Object> listeners);
    }




    /**
     * 会话请求。
     * 该接口是异步的，请求结果会通过rspListener反馈。
     * @param req 请求消息
     * @param rspListener 响应监听者。可以为null表示请求者不关注请求结果
     * @param reqPara 请求参数列表，可以没有。  */
    protected synchronized void req(T req, IResultListener rspListener, Object... reqPara){

        if (!rspProcessorMap.keySet().contains(req)){
            KLog.p(KLog.ERROR, "%s is not in 'cared-req-list'", req);
            return;
        }

        String prefixedReq = prefixMsg(req.name());
        if (!sessionFairy.req(this, prefixedReq, ++reqSn, reqPara)){
            KLog.p(KLog.ERROR, "%s failed", req);
            return;
        }

        KLog.p(KLog.DEBUG,"req=%s, reqSn=%s, listener=%s", prefixedReq, reqSn, rspListener);

        listenerLifecycleObserver.tryObserve(rspListener);

        rspListeners.put(reqSn, new ReqBundle(req, rspListener));

    }

    /**
     * 取消会话请求。
     * @param req 请求消息
     * @param rspListener 监听者，可能为null（对应的req时传入的是null）
     * 若同样的请求id同样的响应监听者请求了多次，则取消的是最早的请求。*/
    protected synchronized void cancelReq(T req, IResultListener rspListener){
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
                KLog.p(KLog.DEBUG,"cancel req=%s, reqSn=%s, listener=%s", prefixMsg(req.name()), reqSn, value.listener);
                sessionFairy.cancelReq(reqSn);
                rspListeners.remove(reqSn);
                listenerLifecycleObserver.unobserve(rspListener);
                break;
            }
        }

    }

    /**
     * 取消指定类型的请求。
     * */
    protected synchronized void cancelReq(Set<T> reqs){
        if (null == reqs){
            return;
        }

        Iterator<Map.Entry<Integer, ReqBundle>> it = rspListeners.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, ReqBundle> entry = it.next();
            int reqSn = entry.getKey();
            ReqBundle value = entry.getValue();
            if (reqs.contains(value.req)){
                KLog.p(KLog.DEBUG,"cancel req=%s, reqSn=%s, listener=%s", prefixMsg(value.req.name()), reqSn, value.listener);
                it.remove();
                sessionFairy.cancelReq(reqSn);
                listenerLifecycleObserver.unobserve(value.listener);
            }
        }

    }

    /**
     * 取消所有会话请求。
     * */
    protected synchronized void cancelAllReqs(){
        for (Map.Entry<Integer, ReqBundle> entry : rspListeners.entrySet()){
            int reqSn = entry.getKey();
            ReqBundle value = entry.getValue();
            KLog.p(KLog.DEBUG,"cancel req=%s, reqSn=%s, listener=%s", prefixMsg(value.req.name()), reqSn, value.listener);
            sessionFairy.cancelReq(reqSn);
            listenerLifecycleObserver.unobserve(value.listener);
        }
        rspListeners.clear();
    }

    /**
     * 订阅通知
     * */
    protected synchronized void subscribe(T ntfId, Object ntfListener){
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
    protected synchronized void subscribe(T[] ntfIds, Object ntfListener){
        if (null == ntfIds || null == ntfListener){
            return;
        }
        boolean bSuccess = false;
        for (T ntfId : ntfIds){
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
    protected synchronized void unsubscribe(T ntfId, Object ntfListener){
        if (null == ntfListener){
            return;
        }
        T[] ntfIds = (T[]) Array.newInstance(enumT, 1);
        ntfIds[0] = ntfId;
        if (delNtfListener(ntfIds, ntfListener)){
            listenerLifecycleObserver.unobserve(ntfListener);
        }
    }

    /**
     * 批量取消订阅通知
     * */
    protected synchronized void unsubscribe(T[] ntfIds, Object ntfListener){
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
     * @param set 请求消息。
     * @param paras 参数。
     * */
    protected void set(T set, Object... paras){
        commandFairy.set(prefixMsg(set.name()), paras);
    }

    /**
     * 获取（配置）请求
     * 该接口是同步的，若下层natiev方法实现耗时则调用者被阻塞，请求结果通过返回值反馈给调用者。
     * @param get 请求消息。
     * @param paras 参数。可以为空。
     * @return 请求结果。
     * */
    protected Object get(T get, Object... paras){
        return commandFairy.get(prefixMsg(get.name()), paras);
    }




    protected Set<Object> getNtfListeners(T ntfId){
        return ntfListeners.get(ntfId.name());
    }

    protected boolean containsNtfListener(Object ntfListener){ // TODO 改为containsNtfListener(T ntf, Object ntfListener)
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

    /**
     * 该响应监听器是否仍在。
     * 请求发出后，响应回来前，可能由于各种原因，如用户取消请求、或者监听器本身被销毁，导致该监听器已不存在于会话中。
     * */
    protected boolean containsRspListener(Object rspListener){ // TODO 改为containsRspListener(T rsp, Object rspListener)
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

    private synchronized boolean delNtfListener(T[] ntfIds, Object ntfListener){
        boolean bSuccess = false;
        if (null != ntfIds) {
            for (T ntfId : ntfIds) {
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

    private String prefixMsg(String msg){
        return msgPrefix+msg;
    }

    private String unprefixMsg(String prefixedMsg){
        return prefixedMsg.substring(msgPrefix.length());
    }

    @Override
    public boolean onRsp(boolean bLast, String rspName, Object rspContent, String reqName, int reqSn, Object[] reqParas) {
        T req = T.valueOf(enumT, unprefixMsg(reqName));
        T rsp = T.valueOf(enumT, unprefixMsg(rspName));
        IResultListener resultListener = rspListeners.get(reqSn).listener;
        StringBuffer sb = new StringBuffer();
        for (Object para : reqParas){
            sb.append(para).append("; ");
        }
        KLog.p(KLog.DEBUG,"rsp=%s, rspContent=%s, resultListener=%s, req=%s, reqSn=%s, \nreqParas=%s", rspName, rspContent, resultListener, reqName, reqSn, sb);
        boolean bConsumed = rspProcessorMap.get(req).process(rsp, rspContent, resultListener, req, reqParas);
        if (bConsumed){
            if (bLast){
                rspListeners.remove(reqSn);
                listenerLifecycleObserver.unobserve(resultListener);
            }
        }else{
            KLog.p(KLog.WARN, "rsp %s not consumed, req=%s, reqSn=%s", rspName, reqName, reqSn);
        }
        return bConsumed;
    }

    @Override
    public void onTimeout(String reqName, int reqSn, Object[] reqParas) {
        T req = T.valueOf(enumT, unprefixMsg(reqName));
        IResultListener resultListener = rspListeners.remove(reqSn).listener;
        listenerLifecycleObserver.unobserve(resultListener);
        StringBuffer sb = new StringBuffer();
        for (Object para : reqParas){
            sb.append(para).append("; ");
        }
        KLog.p(KLog.DEBUG,"TIMEOUT, req=%s, resultListener=%s, reqSn=%s, reqParas=%s", reqName, resultListener, reqSn, sb);
        boolean bConsumed = onTimeout(req, resultListener, reqParas);
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
        String unPrefixedNtfName = unprefixMsg(ntfName);
        Set<Object> listeners = ntfListeners.get(unPrefixedNtfName);
        StringBuffer sb = new StringBuffer();
        for (Object listener : listeners){
            sb.append(listener).append("; ");
        }
        T ntf = T.valueOf(enumT, unPrefixedNtfName);
        KLog.p(KLog.DEBUG,"ntfId=%s, ntfContent=%s, listeners=%s", ntfName, ntfContent, sb);
        ntfProcessorMap.get(ntf).process(ntf, ntfContent, listeners);
    }

    /**
     * 请求超时
     * */
    protected boolean onTimeout(T req, IResultListener rspListener, Object[] reqPara){return false;}

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

    private class ReqBundle{
        private T req;
        private IResultListener listener;

        public ReqBundle(T req, IResultListener listener) {
            this.req = req;
            this.listener = listener;
        }
    }



    //========= 以下为模拟模式相关接口，仅用于本地调试，正式产品中请勿使用=========

    /**
     * 启用/停用模拟器。
     * 若启用则本模块的请求都交由模拟器处理，模拟器会反馈用户模拟的响应/通知；
     * 若停用则恢复正常模式，正常模式下请求通过底层组件发给平台平台反馈消息。
     * NOTE: 仅用于本地调试，正式产品中请勿启用。
     * @param bEnable true：启用，false：停用。
     * */
    public void enableSimulator(boolean bEnable){
        int sessionFairyPriority = crystalBall.getPriority(sessionFairy);
        int notificationFairyPriority = crystalBall.getPriority(notificationFairy);

        crystalBall.delListener(sessionFairy);
        crystalBall.delListener(notificationFairy);
        sessionFairy.setCrystalBall(null);
        commandFairy.setCrystalBall(null);

        if (bEnable){
            crystalBall = FakeCrystalBall.instance();
        }else{
            crystalBall = CrystalBall.instance();
        }

        crystalBall.addListener(sessionFairy, sessionFairyPriority);
        crystalBall.addListener(notificationFairy, notificationFairyPriority);
        sessionFairy.setCrystalBall(crystalBall);
        commandFairy.setCrystalBall(crystalBall);
    }


    /**
     * 填充模拟数据。仅用于模拟模式
     * @param key 数据对应的键，约定为方法名。
     * @param data 用户期望的数据。
     * */
    public void feedSimulatedData(String key, Object data){
        if (!(crystalBall instanceof FakeCrystalBall)){
            KLog.p(KLog.ERROR, "simulator not enable yet!");
            return;
        }
        Object[] datas = genSimulatedData(new String[]{key}, data);
        if (null == datas) {
            KLog.p(KLog.ERROR, "genSimulatedData for (%s, %s) failed", key, data);
            return;
        }
        for (Object o : datas) {
            if (null != o) SimulatedDataRepository.put(o);
        }
    }

    /**
     * （驱使下层）发射通知。仅用于模拟模式。
     * */
    public void eject(String key, Object data){
        if (!(crystalBall instanceof FakeCrystalBall)){
            KLog.p(KLog.ERROR, "simulator not enable yet!");
            return;
        }
        String[] keys = new String[]{key};
        Object[] simulatedData = genSimulatedData(keys, data);
        notificationFairy.emit(keys[0], simulatedData[0]);
    }

    public void eject(String[] keys, Object[] datas){
        for (int i=0; i<keys.length; ++i){
            eject(keys[i], datas[i]);
        }
    }


    /**
     * 生成模拟数据
     * @param key 数据对应的键（约定为方法名），定义为数组因为可能为出参。
     * @param data 用户期望的数据。
     * @return 底层消息对应的数据。
     * */
    protected Object[] genSimulatedData(String[] key, Object data){return null;}


}
