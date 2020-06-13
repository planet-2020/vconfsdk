package com.kedacom.vconf.sdk.amulet;

import androidx.annotation.NonNull;

import com.kedacom.vconf.sdk.utils.lifecycle.ListenerLifecycleObserver;
import com.kedacom.vconf.sdk.utils.log.KLog;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.HashSet;
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

    private ListenerLifecycleObserver listenerLifecycleObserver;

    private Class<T> enumT;

    private String msgPrefix;

    @SuppressWarnings("ConstantConditions")
    protected Caster(){
        enumT = (Class<T>)((ParameterizedType)getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        try {
            Class<?> msgGenClz = Class.forName(enumT.getPackage().getName()+".Message$$Generated");
            MagicBook.instance().addChapter(msgGenClz);
            Field field = msgGenClz.getDeclaredField("module");
            field.setAccessible(true);
            msgPrefix = field.get(null)+"_";
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
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
                for (ReqBundle reqBundle : rspListeners.values()){
                    if (listener == reqBundle.listener){
                        reqBundle.listener = null;  // 保留会话，仅删除监听器
                    }
                }
            }
        });


        Set<T> ntfs = subscribeNtfs();
        if (null != ntfs){
            for (T ntf : ntfs){
                notificationFairy.subscribe(this, prefixMsg(ntf.name()));
            }
        }

    }

    /**订阅通知*/
    protected abstract Set<T> subscribeNtfs();


    /**
     * 会话请求。
     * 该接口是异步的，请求结果会通过rspListener反馈。
     * @param req 请求消息
     * @param rspListener 响应监听者。可以为null表示请求者不关注请求结果
     * @param reqPara 请求参数列表，可以没有。  */
    protected synchronized void req(@NonNull T req, IResultListener rspListener, Object... reqPara){
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
    protected synchronized void cancelReq(@NonNull T req, IResultListener rspListener){
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
     * 取消会话请求。
     * */
    protected synchronized void cancelReq(@NonNull Set<T> reqs){
        Iterator<Map.Entry<Integer, ReqBundle>> it = rspListeners.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, ReqBundle> entry = it.next();
            int reqSn = entry.getKey();
            ReqBundle reqBundle = entry.getValue();
            if (reqs.contains(reqBundle.req)){
                KLog.p(KLog.DEBUG,"cancel req=%s, reqSn=%s, listener=%s", prefixMsg(reqBundle.req.name()), reqSn, reqBundle.listener);
                it.remove();
                sessionFairy.cancelReq(reqSn);
                listenerLifecycleObserver.unobserve(reqBundle.listener);
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


    /**
     * 监听器是否仍在。
     * 请求发出后，响应回来前，可能由于各种原因，如用户取消请求、或者监听器本身被销毁，导致该监听器已不存在于会话中。
     * @param req
     * @param rspListener
     * */
    protected boolean contains(T req, @NonNull IResultListener rspListener){
        for (ReqBundle reqBundle : rspListeners.values()){
            if (req == reqBundle.req
                    && rspListener==reqBundle.listener){
                return true;
            }
        }
        return false;
    }


    /**
     * 删除监听器。
     * */
    public synchronized void delListener(@NonNull IResultListener listener){
        for (Map.Entry<Integer, ReqBundle> entry: rspListeners.entrySet()){
            int key = entry.getKey();
            ReqBundle reqBundle = entry.getValue();
            if (listener == reqBundle.listener){
//                KLog.p("delRspListener reqSn=%s, req=%s, listener=%s", key, val.req, val.listener);
                reqBundle.listener = null;  // 保留会话，仅删除监听器
                listenerLifecycleObserver.unobserve(listener);
            }
        }
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
        boolean bConsumed = onRsp(rsp, rspContent, resultListener, req, reqParas);
        if (bConsumed){
            if (bLast){
                rspListeners.remove(reqSn);
                listenerLifecycleObserver.unobserve(resultListener);
            }
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
        T ntf = T.valueOf(enumT, unPrefixedNtfName);
        onNtf(ntf, ntfContent);
    }


    /**
     * @param rsp 响应ID
     * @param rspContent 响应内容，具体类型由响应ID决定。
     * @param listener 响应监听器,req()传入。
     *                 NOTE：可能为null。一则可能用户传入即为null，二则可能在会话过程中监听器被销毁，如调用了{@link #delListener(IResultListener)} 或者监听器绑定的生命周期对象已销毁，
     * @param req 请求ID，req()传入。
     * @param reqParas 请求参数列表，req()传入，顺序同传入时的
     * @return true，若该响应已被处理；否则false。
     * */
    protected abstract boolean onRsp(T rsp, Object rspContent, IResultListener listener, T req, Object[] reqParas);

    /**
     * @param req 请求ID，req()传入。
     * @param listener 响应监听器,req()传入。
     *                 NOTE：可能为null。一则可能用户传入即为null，二则可能在会话过程中监听器被销毁，如调用了{@link #delListener(IResultListener)} 或者监听器绑定的生命周期对象已销毁，
     * @param reqParas 请求参数列表，req()传入，顺序同传入时的
     * */
    protected boolean onTimeout(T req, IResultListener listener, Object[] reqParas){
        return false;
    }

    /**
     * @param ntf 通知ID
     * @param ntfContent 通知内容，具体类型由通知ID决定*/
    protected abstract void onNtf(T ntf, Object ntfContent);


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
