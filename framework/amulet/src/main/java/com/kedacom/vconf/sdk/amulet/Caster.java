package com.kedacom.vconf.sdk.amulet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;

import com.kedacom.vconf.sdk.utils.log.KLog;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;


public abstract class Caster<T extends Enum<T>> implements
        IFairy.ISessionFairy.IListener,
        IFairy.INotificationFairy.IListener{

    private IFairy.ISessionFairy sessionFairy = new SessionFairy();
    private IFairy.INotificationFairy notificationFairy = new NotificationFairy();
    private IFairy.ICommandFairy commandFairy = new CommandFairy();
    private ICrystalBall crystalBall = CrystalBall.instance();

    private final Set<Session> sessions = new LinkedHashSet<>();
    private final Map<Class<? extends ILifecycleOwner>, T> listenerType2CaredNtfMap = new HashMap<>();
    private final Map<T, Set<ILifecycleOwner>> ntfListenersMap = new LinkedHashMap<>();

    private Class<T> enumT;

    private static Set<String> modules = new HashSet<>();
    private String module;
    private String msgPrefix;

    private ListenerLifecycleObserver listenerLifecycleObserver = ListenerLifecycleObserver.getInstance();
    private ListenerLifecycleObserver.Callback ListenerLifecycleObserverCb = new ListenerLifecycleObserver.Callback(){
        @Override
        public void onListenerResumed(Object listener) {
            // 该事件是粘滞的，即便activity已经resume很久了然后才注册生命周期观察者也会收到该事件。
            if (((ILifecycleOwner) listener).destroyWhenLifecycleOwner() == Lifecycle.Event.ON_RESUME){
                delListener((ILifecycleOwner) listener);
            }
        }

        @Override
        public void onListenerPause(Object listener) {
            if (((ILifecycleOwner) listener).destroyWhenLifecycleOwner() == Lifecycle.Event.ON_PAUSE){
                delListener((ILifecycleOwner) listener);
            }
        }

        @Override
        public void onListenerStop(Object listener) {
            if (((ILifecycleOwner) listener).destroyWhenLifecycleOwner() == Lifecycle.Event.ON_STOP){
                delListener((ILifecycleOwner) listener);
            }
        }

        @Override
        public void onListenerDestroy(Object listener) {
            if (((ILifecycleOwner) listener).destroyWhenLifecycleOwner() == Lifecycle.Event.ON_DESTROY){
                delListener((ILifecycleOwner) listener);
            }
        }
    };


    @SuppressWarnings("ConstantConditions")
    protected Caster(){
        IMagicBook magicBook = null;
        enumT = (Class<T>)((ParameterizedType)getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        try {
            Class<?> magicBookClz = Class.forName(enumT.getPackage().getName()+".MagicBook$$Impl");
            Constructor<?> ctor = magicBookClz.getDeclaredConstructor();
            ctor.setAccessible(true);
            magicBook = (IMagicBook) ctor.newInstance();
            module = magicBook.name();
            if(!modules.add(module)){
                throw new RuntimeException(String.format("module %s has existed already!", module));
            }
            msgPrefix = module +"_";
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException | InstantiationException e) {
            e.printStackTrace();
        }
        if (null == magicBook){
            throw new RuntimeException("no magicBook!?");
        }

        sessionFairy.setMagicBook(magicBook);
        sessionFairy.setCrystalBall(crystalBall);
        commandFairy.setMagicBook(magicBook);
        commandFairy.setCrystalBall(crystalBall);
        notificationFairy.setMagicBook(magicBook);

        crystalBall.addRspListener(sessionFairy);
        crystalBall.addNtfListener(notificationFairy);

        Set<String> ntfIds = magicBook.ntfIds(null);
        if (null != ntfIds){
            for (String ntfId : ntfIds){
                if (notificationFairy.subscribe(this, ntfId)){
                    String val = unprefix(ntfId);
                    T ntf = T.valueOf(enumT, val);
                    ntfListenersMap.put(ntf, new HashSet<>());
                }
            }
        }

        Map<Class<? extends ILifecycleOwner>, T> ntfListenerTypes = regNtfListenerType();
        if (ntfListenerTypes != null) {
            listenerType2CaredNtfMap.putAll(ntfListenerTypes);
        }
    }

    /**
     * 注册通知监听器类型
     * */
    protected Map<Class<? extends ILifecycleOwner>, T> regNtfListenerType(){return null;}

    /**
     * 会话请求（异步请求）
     * NOTE: 不同于{@link #set(Enum, Object...)}和{@link #get(Enum, Object...)}，
     * 该接口是异步的不会阻塞调用者；
     * 该接口返回不代表请求已执行，请求结果通过rspProcessor反馈。
     * @param req 请求消息
     * @param sessionProcessor 会话处理器。为null则表示不关注响应或没有响应。
     * @param reqParas 请求参数列表，可以没有。
     * @param resultListener 请求结果监听器。
     *                       NOTE: 会话持有了resultListener的引用，而resultListener可能为生命周期对象如Activity，
     *                       若resultListener生命周期结束于会话之前则会话需要及时释放resultListener引用以避免内存泄漏以及不被用户期望的请求结果回调。
     *                       释放引用有两种方式：手动释放和自动释放。
     *                       手动释放指用户通过调用{@link #delListener(ILifecycleOwner)}释放，自动释放则由Caster自动管理，目前实现是在resultListener生命周期结束时释放。
     *                       手动释放很繁琐且易遗漏出错，所以最好由Caster自动释放。
     *
     *                       在调用本接口时，Caster会尝试监控resultListener的生命周期，进而实现自动释放，但是成功的前提是——
     *                       resultListener需得是生命周期拥有者{@link androidx.lifecycle.LifecycleOwner}或者绑定了某个LifecycleOwner，
     *                       这样，当LifecycleOwner#onDestroy时Caster会自动解除对resultListener的引用，用户无需做额外操作（除非这个行为非用户期望）。
     *                       AppCompatActivity以及Fragment(support包的或androidx的)都是LifecycleOwner，用户也可以自定义监听器并实现LifecycleOwner（详询官网）。
     *                       resultListener也可以绑定到某个LifecycleOwner，这样其生命周期跟绑定对象同步。
     *                       绑定有两种方式：手动绑定和自动绑定。
     *                       手动绑定是通过实现{@link IResultListener#getLifecycleOwner()}，返回值即为绑定的生命周期拥有者。此种绑定方式具有最高优先级，即便resultListener自身是LifecycleOwner。
     *                       自动绑定的条件是监听器的“直接”外部类是LifecycleOwner或者绑定了LifecycleOwner。
     *
     *                       下面举几个例子：
     *                       例一：自动绑定
     *                       {@code
     *                       public class WelcomeActivity extends AppCompatActivity { // WelcomeActivity为LifecycleOwner
     *                           protected void onResume() {
     *                           // startupManager为Caster子类
     *                              startupManager.start(terminalType, new IResultListener() { // 此匿名内部类的直接外部类WelcomeActivity为LifecycleOwner，所以它自动绑定到了WelcomeActivity的生命周期，
     *                                                                                            在WelcomeActivity#onDestroy时，Caster将自动解除和该IResultListener的绑定，WelcomeActivity顺利销毁且不会再收到IResultListener的回调。
     *                                  ...
     *                       }
     *
     *                       例二：手动绑定+自动绑定
     *                       {@code
     *                       public class WelcomeActivity extends AppCompatActivity {  // WelcomeActivity为LifecycleOwner
     *                           protected void onResume() {
     *                              startBtn.setOnClickListener(new View.OnClickListener() { // OnClickListener不是LifecycleOwner
     *                                 public void onClick(View v) {
     *                                     startupManager.start(terminalType, new IResultListener() { // 此监听器（记为IResultListener0）直接外部类是OnClickListener，非LifecycleOwner，
     *                                                                                                  并且IResultListener0自身也不是LifecycleOwner，所以该监听器没有自动绑定的生命周期对象，Caster不能自动管理其生命周期。
     *                                          public LifecycleOwner getLifecycleOwner() {
     *                                              // 用户手动绑定IResultListener0到WelcomeActivity的生命周期，此时Caster可以自动管理该监听器的生命周期了，用户无需手动释放
     *                                              return WelcomeActivity.this;
     *                                          }
     *                                          public void onSuccess(Object result) {
     *                                              startupManager.login(account, password, new IResultListener() { // 该监听器（记为IResultListener1）的直接外部类是IResultListener0，
     *                                                                                                                 虽然IResultListener0不是LifecycleOwner但是它绑定了WelcomeActivity的生命周期，所以
     *                                                                                                                 IResultListener1也自动绑定了WelcomeActivity的生命周期，用户无需手动释放。
     *                                                ......
     *                       }
     *
     *                       例三：未绑定生命周期对象+手动释放：
     *                       {@code
     *                        public class WelcomeActivity extends AppCompatActivity {
     *                            IResultListener startResultListener = new IResultListener() {...}; // 手动释放往往需要定义一个监听器的成员变量
     *                            protected void onResume() {
     *                               startupManager.start(terminalType, startResultListener); // 传入前面定义的监听器。（Ps：同一个监听器可以同时用于多个会话。）
     *                               ...
     *                            }
     *                            protected void onDestroy() {
     *                               startupManager.delListener(startResultListener); // 手动释放监听器
     *                            }
     *                        }
     *
     *                       总结:
     *                       调用本接口时Caster会尝试监控resultListener的生命周期以便在resultListener销毁时自动释放其引用，但成功的条件是resultListener是LifecycleOwner或绑定了LifecycleOwner；
     *                       绑定LifecycleOwner可手动（通过实现{@link IResultListener#getLifecycleOwner()}），或自动（满足resultListener的“直接”外部类是LifecycleOwner或绑定了LifecycleOwner）；
     *                       绑定的优先级按从高到低： getLifecycleOwner > 自身即为LifecycleOwner > 直接外部类是LifecycleOwner或绑定了LifecycleOwner；
     *                       如果用户的监听器本身就是一个长寿对象（如一个全局单例），肯定长过session的生命周期，则无需关注生命周期问题；
     *                       当session结束时（session一定会结束，有超时机制），Caster会自动释放监听器引用，所以多数情况下即使不做任何处理现象上也不会表现出问题，但逻辑上是有问题的，在某些极端场景下会表现异常；
     *                       IResultListener的外部类定义不要使用lambada，否则绑定可能失败（取决于java编译器具体实现），AS会提示转换为lambada，请suppress；
     *                       建议用户尽量参考例一例二；
     *
     * */
    protected void req(@NonNull T req, SessionProcessor<T> sessionProcessor, IResultListener resultListener, Object... reqParas){
        String reqId = prefix(req.name());
        Session s = new Session(req, sessionProcessor, resultListener);
        if (!sessionFairy.req(this, reqId, s.id, reqParas)){
            KLog.p(KLog.ERROR, "%s failed", req);
            return;
        }
        sessions.add(s);
        listenerLifecycleObserver.tryObserve(resultListener, ListenerLifecycleObserverCb);
        KLog.p(KLog.DEBUG,"req=%s, sid=%s, sessionProcessor=%s, resultListener=%s", req, s.id, sessionProcessor, resultListener);
    }

    /**
     * 取消会话。
     * @param req 请求消息
     * @param resultListener 结果监听器，为null则取消所有请求消息为req的会话。
     * */
    protected void cancelReq(@NonNull T req, IResultListener resultListener){
        Iterator<Session> it = sessions.iterator();
        while (it.hasNext()){
            Session s = it.next();
            if (req == s.req
                    && (null==resultListener || resultListener==s.resultListener)){
                KLog.p(KLog.DEBUG,"cancel req=%s, sid=%s, sessionProcessor=%s, listener=%s", req, s.id, s.processor, s.resultListener);
                it.remove();
                sessionFairy.cancelReq(s.id);
            }
        }
        if (!containsListener(resultListener)){
            listenerLifecycleObserver.unobserve(resultListener);
        }
    }

    /**
     * 取消所有会话。
     * */
    protected void cancelAllReqs(){
        for (Session s : sessions) {
            sessionFairy.cancelReq(s.id);
            if (!containsNtfListener(s.resultListener)){
                listenerLifecycleObserver.unobserve(s.resultListener);
            }
        }
        sessions.clear();
    }

    /**
     * 同步请求（一般用于设置配置）
     * 该接口是同步的，若下层native方法实现耗时则调用者被阻塞。接口返回请求完成。
     * @param set 请求消息。
     * @param paras 参数。
     * @see #req(Enum, SessionProcessor, IResultListener, Object...)
     * */
    protected void set(T set, Object... paras){
        commandFairy.set(prefix(set.name()), paras);
    }

    /**
     * 获取配置请求
     * 该接口是同步的，若下层native方法实现耗时则调用者被阻塞。接口返回请求完成。
     * @param get 请求消息。
     * @param paras 参数。可以为空。
     * @return 请求结果。
     * */
    protected Object get(T get, Object... paras){
        return commandFairy.get(prefix(get.name()), paras);
    }


    /**
     * 添加通知监听器
     * @param ntfListener 通知监听器。
     *                    NOTE: Caster会尝试监测该监听器的生命周期，并做相应处理，
     *                    参见{@link #req(Enum, SessionProcessor, IResultListener, Object...)}}对IResultListener的处理
     * */
    protected void addNtfListener(@NonNull T ntfId, @NonNull ILifecycleOwner ntfListener){
        Set<ILifecycleOwner> listeners = ntfListenersMap.get(ntfId);
        if (null == listeners){
            KLog.p(KLog.ERROR, "no such ntf %s", ntfId);
            return;
        }
        KLog.p(KLog.DEBUG,"ntfId=%s, ntfListener=%s", ntfId, ntfListener);
        listeners.add(ntfListener);
        listenerLifecycleObserver.tryObserve(ntfListener, ListenerLifecycleObserverCb);
    }

    /**
     * 批量添加通知监听器
     * */
    protected void addNtfListeners(@NonNull T[] ntfIds, @NonNull ILifecycleOwner ntfListener){
        for (T ntfId : ntfIds){
            addNtfListener(ntfId, ntfListener);
        }
    }


    /**
     * 删除通知监听器
     * @param ntf 监听器监听的通知
     * @param listener 通知监听器。要删除的监听器对象 */
    protected void delNtfListener(@NonNull T ntf, @NonNull ILifecycleOwner listener){
        Set<ILifecycleOwner> listeners = ntfListenersMap.get(ntf);
        if (null == listeners) {
            KLog.p(KLog.ERROR, "no such ntf %s", ntf);
            return;
        }
        KLog.p(KLog.DEBUG,"delete ntfListener, ntf=%s, listener=%s", ntf, listener);
        listeners.remove(listener);
        if(!containsListener(listener)){
            listenerLifecycleObserver.unobserve(listener);
        }
    }


    /**
     * 批量删除通知监听器
     * @param ntfs 监听器监听的通知（一个监听器可能监听多个通知）。若为null则表示任意通知。
     * @param listener 通知监听器。要删除的监听器对象。若为null则表示任意监听器。
     *                 NOTE: 若ntfs和listener同时为null则删除所有通知监听器。
     * */
    protected void delNtfListeners(@Nullable T[] ntfs, @Nullable ILifecycleOwner listener){
        if (null != ntfs) {
            for (T ntf : ntfs) {
                Set<ILifecycleOwner> ntfListeners = ntfListenersMap.get(ntf);
                if (null != ntfListeners) {
                    if (null != listener) {
                        KLog.p(KLog.DEBUG, "delete ntfListener, ntf=%s, listener=%s", ntf, listener);
                        ntfListeners.remove(listener);
                    }else{
                        KLog.p(KLog.DEBUG, "clear ntfListener, ntf=%s", ntf);
                        ntfListeners.clear();
                    }
                }
            }
        }else{
            if (null != listener) {
                for (Set<ILifecycleOwner> ntfListeners : ntfListenersMap.values()) {
                    KLog.p(KLog.DEBUG, "delete ntfListener, listener=%s", listener);
                    ntfListeners.remove(listener);
                }
            }else{
                KLog.p(KLog.DEBUG, "clear all ntfListeners");
                ntfListenersMap.clear();
            }
        }
        if(!containsListener(listener)){
            listenerLifecycleObserver.unobserve(listener);
        }
    }

    /**
     * 添加通知监听器
     * */
    public void addNtfListener(@NonNull ILifecycleOwner listener){
        T ntf = listenerType2CaredNtfMap.get(listener.getClass());
        if (ntf==null){
            throw new IllegalArgumentException(String.format("listener %s not supported by %s!", listener.getClass(), getClass()));
        }
        addNtfListener(ntf, listener);
    }

    /**
     * 删除监听器。
     * NOTE：该接口会删除该listener注册的所有监听器，包括各个请求结果监听器，通知监听器。
     *
     * @param listener*/
    public void delListener(@NonNull ILifecycleOwner listener){
        delNtfListeners(null, listener);
        if (listener instanceof IResultListener){
            delResultListener((IResultListener) listener);
        }
    }

    /**
     * 删除结果监听器
     *
     * @param listener*/
    protected void delResultListener(@NonNull IResultListener listener){
        for (Session s : sessions) {
            if (listener == s.resultListener) {
                KLog.p(KLog.DEBUG, "delete result listener, req=%s, sid=%s, listener=%s", s.req, s.id, s.resultListener);
                s.resultListener = null; // 保留会话，仅删除监听器
            }
        }
        if(!containsListener(listener)){
            listenerLifecycleObserver.unobserve(listener);
        }
    }


    private boolean containsListener(ILifecycleOwner listener){
        return containsNtfListener(listener) || ((listener instanceof IResultListener) && containsRspListener((IResultListener) listener));
    }

    private boolean containsRspListener(IResultListener listener){
        for (Session s : sessions){
            if (s.resultListener == listener){
                return true;
            }
        }
        return false;
    }

    protected boolean containsNtfListener(ILifecycleOwner listener){
        for (Set<ILifecycleOwner> listeners : ntfListenersMap.values()){
            if (listeners.contains(listener)){
                return true;
            }
        }
        return false;
    }

    private String prefix(String msg){
        return msgPrefix +msg;
    }

    private String unprefix(String prefixedMsg){
        return prefixedMsg.substring((msgPrefix).length());
    }

    private Session getSession(int sid){
        for (Session s : sessions){
            if (sid == s.id){
                return s;
            }
        }
        throw new RuntimeException("no such session "+sid);
    }

    @Override
    public void onReqSent(boolean hasRsp, String reqId, int reqSn, Object[] reqParas) {
        T req = T.valueOf(enumT, unprefix(reqId));
        Session s = getSession(reqSn);
        IResultListener resultListener = s.resultListener;
        if (!hasRsp){
            sessions.remove(s);
            listenerLifecycleObserver.unobserve(resultListener);
        }
        KLog.p(KLog.DEBUG,"req=%s, sid=%s, resultListener=%s", req, s.id, resultListener);
        if (null == s.processor){
            KLog.p(KLog.WARN, "null == processor");
            return;
        }
        s.processor.onReqSent(resultListener, req, reqParas);
    }

    @Override
    public boolean onRsp(boolean bLast, String rspId, Object rspContent, String reqId, int reqSn, Object[] reqParas) {
        T req = T.valueOf(enumT, unprefix(reqId));
        T rsp = T.valueOf(enumT, unprefix(rspId));
        Session s = getSession(reqSn);
        IResultListener resultListener = s.resultListener;
        SessionProcessor<T> processor = s.processor;
        KLog.p(KLog.DEBUG,"req=%s, sid=%s, rsp=%s, resultListener=%s, \nrspContent=%s", req, s.id, rsp, resultListener, rspContent);
        if (null != processor){
            boolean[] isConsumed = new boolean[]{true};
            processor.onRsp(rsp, rspContent, resultListener, req, reqParas, isConsumed);
            if (isConsumed[0]){
                if (bLast){
                    sessions.remove(s);
                    listenerLifecycleObserver.unobserve(resultListener);
                }
            }
            return isConsumed[0];
        }

        return false;
    }

    @Override
    public void onTimeout(String reqId, int reqSn, Object[] reqParas) {
        T req = T.valueOf(enumT, unprefix(reqId));
        Session s = getSession(reqSn);
        sessions.remove(s);
        IResultListener resultListener = s.resultListener;
        listenerLifecycleObserver.unobserve(resultListener);
        KLog.p(KLog.DEBUG,"req=%s, sid=%s, resultListener=%s", req, s.id, resultListener);
        SessionProcessor<T> processor = s.processor;
        boolean[] isConsumed = new boolean[]{false};
        if (null != processor){
            processor.onTimeout(resultListener, req, reqParas, isConsumed);
        }
        if (!isConsumed[0]){
            // 超时未被消费则此处通知用户超时
            if (resultListener != null) {
                resultListener.onArrive(false);
                resultListener.onTimeout();
            }
        }
    }


    @Override
    public void onNtf(String ntfId, Object ntfContent) {
        T ntf = T.valueOf(enumT, unprefix(ntfId));
        Set<ILifecycleOwner> listeners = ntfListenersMap.get(ntf);
        StringBuilder sb = new StringBuilder();
        for (Object listener : listeners) {
            sb.append(listener).append("\n");
        }
        KLog.p(KLog.DEBUG,"ntf=%s, ntfContent=%s\nlisteners=%s", ntf, ntfContent, sb.toString());

        onNotification(ntf, ntfContent, listeners);
    }

    /**
     * 上报用户请求进度
     * */
    protected void reportProgress(Object progress, IResultListener listener){
        reportProgress(progress, listener, false);
    }


    /**
     * 上报用户请求成功
     * */
    protected void reportSuccess(Object result, IResultListener listener){
        reportSuccess(result, listener, false);
    }

    /**
     * 上报用户请求失败
     * */
    protected void reportFailed(int errorCode, IResultListener listener){
        reportFailed(errorCode, listener, false);
    }

    /**
     * 上报用户请求超时
     * */
    protected void reportTimeout(IResultListener listener){
        reportTimeout(listener, false);
    }


    /**
     * @param onlyIfListenerExistInSession 仅当listener仍存在于会话中时才上报。
     *                                     listener可能在会话过程中被销毁了详情可参考{@link #req(Enum, SessionProcessor, IResultListener, Object...)}
     * */
    protected void reportProgress(Object progress, IResultListener listener, boolean onlyIfListenerExistInSession){
        if (null == listener || (onlyIfListenerExistInSession && !containsRspListener(listener)) ){
            return;
        }
        listener.onProgress(progress);
    }

    protected void reportSuccess(Object result, IResultListener listener, boolean onlyIfListenerExistInSession){
        if (null == listener || (onlyIfListenerExistInSession && !containsRspListener(listener)) ){
            return;
        }
        listener.onArrive(true);
        listener.onSuccess(result);
    }

    protected void reportFailed(int errorCode, IResultListener listener, boolean onlyIfListenerExistInSession){
        if (null == listener || (onlyIfListenerExistInSession && !containsRspListener(listener)) ){
            return;
        }
        listener.onArrive(false);
        listener.onFailed(errorCode);
    }

    protected void reportTimeout(IResultListener listener, boolean onlyIfListenerExistInSession){
        if (null == listener || (onlyIfListenerExistInSession && !containsRspListener(listener)) ){
            return;
        }
        listener.onArrive(false);
        listener.onTimeout();
    }



    /**
     * 通知抵达
     * 若要处理通知需override此方法。
     * @param ntf 通知
     * @param ntfContent 通知内容，具体类型由通知消息决定
     * @param ntfListeners 通知监听器集合
     * */
    protected void onNotification(T ntf, Object ntfContent, Set<ILifecycleOwner> ntfListeners){}


    /**会话处理器*/
    protected interface SessionProcessor<T>{
        /**
         * 请求已发出。（业务组件接口已返回）
         * @param resultListener 结果监听器,req()传入。
         *                 NOTE: 可能为null。如用户传入即为null，或者会话过程中监听器被销毁，
         * @param req 请求消息，req()传入。
         * @param reqParas 请求参数列表，req()传入，顺序同传入时的
         * */
        default void onReqSent(IResultListener resultListener, T req, Object[] reqParas){}

        /**
         * 收到响应
         * @param rsp 响应消息
         * @param rspContent 响应内容，具体类型由响应消息决定。
         * @param isConsumed 是否已被消费。出参。true已消费，默认是true。 若未消费该消息继续向下流转到其他会话或通知处理器
         * */
        default void onRsp(T rsp, Object rspContent, IResultListener resultListener, T req, Object[] reqParas, boolean[] isConsumed){}

        /**
         * 会话超时
         * @param isConsumed 是否已被消费。出参。默认是已消费。
         * @return 是否已被消费。出参。true已消费，默认是false。若未消费则Caster会接管处理——上报用户已超时。
         * */
        default void onTimeout(IResultListener resultListener, T req, Object[] reqParas, boolean[] isConsumed){}
    }


    private static int sessionCount;
    private class Session {
        private int id;
        private T req;
        private SessionProcessor<T> processor;
        private IResultListener resultListener;

        public Session(T req, SessionProcessor<T> processor, IResultListener resultListener) {
            id = sessionCount++;
            this.req = req;
            this.processor = processor;
            this.resultListener = resultListener;
        }
    }

}
