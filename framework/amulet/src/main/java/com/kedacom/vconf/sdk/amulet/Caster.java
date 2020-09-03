package com.kedacom.vconf.sdk.amulet;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;

import com.kedacom.vconf.sdk.utils.lang.ClassHelper;
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
import java.util.Objects;
import java.util.Set;


@SuppressWarnings({"unused", "SameParameterValue"})
public abstract class Caster<T extends Enum<T>> implements
        IFairy.ISessionFairy.IListener,
        IFairy.INotificationFairy.IListener{

    private IFairy.ISessionFairy sessionFairy = new SessionFairy();

    private final Set<Session> sessions = new LinkedHashSet<>();
    private final Map<Class<? extends ILifecycleOwner>, T[]> listenerType2CaredNtfMap = new HashMap<>();
    private final Map<T, Set<ILifecycleOwner>> ntfListeners = new LinkedHashMap<>();

    private Class<T> enumT;

    private static Set<String> modules = new HashSet<>();
    private String msgPrefix;

    private static Set<Caster>casters = new LinkedHashSet<>();

    private static boolean reqGloballyDisabled;

    private boolean reqDisabled;

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
        //noinspection unchecked
        enumT = (Class<T>) ((ParameterizedType)getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        try {
            Class<?> magicBookClz = Class.forName(enumT.getPackage().getName()+".MagicBook$$Impl");
            Constructor<?> ctor = magicBookClz.getDeclaredConstructor();
            ctor.setAccessible(true);
            magicBook = (IMagicBook) ctor.newInstance();
            String module = magicBook.name();
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
        ICrystalBall crystalBall = CrystalBall.instance();
        sessionFairy.setCrystalBall(crystalBall);
        IFairy.INotificationFairy notificationFairy = new NotificationFairy();
        notificationFairy.setMagicBook(magicBook);

        crystalBall.addRspListener(sessionFairy);
        crystalBall.addNtfListener(notificationFairy);

        Set<String> ntfIds = magicBook.ntfIds(null);
        if (null != ntfIds){
            for (String ntfId : ntfIds){
                if (notificationFairy.subscribe(this, ntfId)){
                    String val = unprefix(ntfId);
                    T ntf = Enum.valueOf(enumT, val);
                    ntfListeners.put(ntf, new HashSet<>());
                }
            }
        }

        Map<Class<? extends ILifecycleOwner>, T[]> ntfListenerTypes = regNtfListener();
        if (ntfListenerTypes != null) {
            listenerType2CaredNtfMap.putAll(ntfListenerTypes);
        }

        casters.add(this);
    }


    /**
     * 发起请求
     * */
    protected void req(@NonNull T req, SessionProcessor<T> sessionProcessor, IResultListener resultListener, Object... reqParas){
        req(false, false, req, sessionProcessor, resultListener, reqParas);
    }

    /**
     * 发起请求
     * 该接口是异步的不会阻塞调用者，该接口返回不代表请求已执行。
     * @param ignoreGlobalBan 是否忽略全局性的禁令{@link #disableReqGlobally(boolean)}
     * @param ignoreBan 是否忽略该Caster的禁令{@link #disableReq(boolean)}
     * @param req 请求消息
     * @param sessionProcessor 会话处理器。为null则表示不关注请求结果或没有结果反馈，比如设置配置往往不需要反馈结果。
     * @param reqParas 请求参数列表，可以没有。
     * @param resultListener 请求结果监听器。
     *                       NOTE: 会话持有了resultListener的引用，而resultListener可能为生命周期对象如Activity，
     *                       若resultListener生命周期结束于会话之前则会话需要及时释放resultListener引用以避免内存泄漏以及不被用户期望的请求结果回调。
     *                       释放引用有两种方式：手动释放和自动释放。
     *                       手动释放指用户通过调用{@link #delListener(ILifecycleOwner)}释放，自动释放则由Caster自动管理，默认是在resultListener生命周期结束时释放。
     *                       手动释放很繁琐易出错，所以最好由Caster自动释放。
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
     *                       此例中，如果您使用类似ButterKnife框架，OnClickListener的中间类可以略去，这样IResultListener0的直接外部类就变成了WelcomeActivity。
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
     * */
    protected void req(boolean ignoreGlobalBan, boolean ignoreBan,
                       @NonNull T req, SessionProcessor<T> sessionProcessor, IResultListener resultListener, Object... reqParas){
        Session s = new Session(ignoreBan, req, sessionProcessor, resultListener, reqParas);
        sessions.add(s);
        if (reqGloballyDisabled && !ignoreGlobalBan){
            KLog.p(KLog.WARN, "reqGloballyDisabled, session %s cached, req=%s", s.id, req);
            s.state = Session.Paused;
            return;
        }
        if (reqDisabled && !ignoreBan){
            KLog.p(KLog.WARN, "reqDisabled, session %s cached, req=%s", s.id, req);
            s.state = Session.Paused;
            return;
        }

        String reqId = prefix(req.name());
        if (!sessionFairy.req(this, reqId, s.id, reqParas)){
            KLog.p(KLog.ERROR, "%s failed", req);
            sessions.remove(s);
            return;
        }
        s.state = Session.Working;
        listenerLifecycleObserver.tryObserve(resultListener, ListenerLifecycleObserverCb);
        KLog.p(KLog.DEBUG,"req=%s, sid=%s, sessionProcessor=%s, resultListener=%s", req, s.id, sessionProcessor, resultListener);
    }


    /**
     * 取消请求
     * @param req 请求消息，为null则取消所有结果监听器为resultListener的请求。
     * @param resultListener 结果监听器，为null则取消所有请求消息为req的请求。
     *                       若req和resultListener均为null则取消所有请求。
     * */
    protected void cancelReq(T req, IResultListener resultListener){
        KLog.p(KLog.DEBUG,"req=%s, resultListener=%s", req, resultListener);
        Iterator<Session> it = sessions.iterator();
        while (it.hasNext()){
            Session s = it.next();
            if ((null==req || req == s.req) && (null==resultListener || resultListener==s.resultListener)){
                KLog.p(KLog.DEBUG,"cancel req=%s, sid=%s, sessionProcessor=%s, listener=%s", s.req, s.id, s.processor, s.resultListener);
                it.remove();
                if (s.state == Session.Working) {
                    sessionFairy.cancelReq(s.id);
                }
                s.state = Session.End;
                if (!containsListener(s.resultListener)){
                    listenerLifecycleObserver.unobserve(s.resultListener);
                }
            }
        }
    }


    /**
     * 添加通知监听器
     * @param listeners 通知监听器列表。
     *                  NOTE:
     *                  1、Caster必须先注册通知监听器类型{@link #regNtfListener()}，只有注册过的类型才能被添加；
     *                  2、Caster会尝试监测该监听器的生命周期，并做相应处理，
     *                  参见{@link #req(Enum, SessionProcessor, IResultListener, Object...)}}对IResultListener的处理。
     *                  NOTE：若ntfListener是lambda，则cater可能无法获取其直接外部类，不同于内部类必定持有外部类的引用，
     *                  lambda是否持有外部类的引用取决于lambda中是否引用了外部类或者外部类的成员，
     *                  若不能获取ntfListener的外部类引用，则须得ntfListener自身是生命周期对象或者通过{@link ILifecycleOwner#getLifecycleOwner()}}指定了要绑定的生命周期对象，
     *                  caster才能自动管理ntfListener的生命周期对象。否则listeners的生命周期需要用户自己管理。
     * */
    public void addNtfListener(@NonNull ILifecycleOwner... listeners){
        for (ILifecycleOwner listener : listeners){
            if (listener == null) {
                continue;
            }
            Class<?> listenerClass = listener.getClass();
            T[] ntfs = null;
            for (Class<?> clz : listenerType2CaredNtfMap.keySet()){
                if (clz.isAssignableFrom(listenerClass)){
                    ntfs = listenerType2CaredNtfMap.get(clz);
                    break;
                }
            }
            if (ntfs == null) {
                throw new IllegalArgumentException(String.format("listener %s not supported by %s", ClassHelper.getReadableName(listener.getClass()), getClass()));
            }

            for (T ntf : ntfs){
                Set<ILifecycleOwner> listenerSet = ntfListeners.get(ntf);
                if (null == listenerSet){
                    KLog.p(KLog.ERROR, "no such ntf %s", ntf);
                    return;
                }
                KLog.p(KLog.DEBUG,"ntf=%s, ntfListener=%s", ntf, listener);
                listenerSet.add(listener);
                listenerLifecycleObserver.tryObserve(listener, ListenerLifecycleObserverCb);
            }
        }
    }


    /**
     * 删除通知监听器
     * @param listener 要删除的通知监听器。若为null则表示删除所有通知监听器。
     * */
    public void delNtfListener(ILifecycleOwner listener){
        for (Map.Entry<T, Set<ILifecycleOwner>> entry : ntfListeners.entrySet()){
            T ntf = entry.getKey();
            Set<ILifecycleOwner> listenerSet = entry.getValue();
            if (null != listener) {
                KLog.p(KLog.DEBUG, "delete ntfListener: ntf=%s, listener=%s", ntf, listener);
                listenerSet.remove(listener);
            }else{
                KLog.p(KLog.DEBUG, "clear ntfListener: ntf=%s", ntf);
                listenerSet.clear();
            }
        }

        if( !(listener instanceof IResultListener && containsRspListener((IResultListener) listener)) ){
            listenerLifecycleObserver.unobserve(listener);
        }
    }


    /**
     * 删除请求结果监听器
     * @param listener 要删除的请求结果监听器。若为null则表示删除所有请求结果监听器。
     * */
    public void delResultListener(IResultListener listener){
        for (Session s : sessions) {
            if (listener==null || listener == s.resultListener) {
                KLog.p(KLog.DEBUG, "delete result listener: req=%s, sid=%s, listener=%s", s.req, s.id, s.resultListener);
                s.resultListener = null; // 保留会话，仅删除监听器
            }
        }
        if(!containsNtfListener(listener)){
            listenerLifecycleObserver.unobserve(listener);
        }
    }


    /**
     * 删除监听器。
     * */
    public void delListener(@NonNull ILifecycleOwner listener){
        delNtfListener(listener);
        if (listener instanceof IResultListener){
            delResultListener((IResultListener) listener);
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
        for (Set<ILifecycleOwner> listeners : ntfListeners.values()){
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
    public void onReqSent(boolean hasRsp, String reqId, int reqSn, Object[] reqParas, Object output) {
        T req = Enum.valueOf(enumT, unprefix(reqId));
        Session s = getSession(reqSn);
        IResultListener resultListener = s.resultListener;
        KLog.p(KLog.DEBUG,"req=%s, sid=%s, resultListener=%s", req, s.id, resultListener);
        if (null != s.processor){
            s.processor.onReqSent(resultListener, req, reqParas, output);
        }
        if (!hasRsp){
            sessions.remove(s);
            if (!containsListener(resultListener)) {
                listenerLifecycleObserver.unobserve(resultListener);
            }
        }
    }

    @Override
    public boolean onRsp(boolean bLast, String rspId, Object rspContent, String reqId, int reqSn, Object[] reqParas) {
        T req = Enum.valueOf(enumT, unprefix(reqId));
        T rsp = Enum.valueOf(enumT, unprefix(rspId));
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
                    if (!containsListener(resultListener)) {
                        listenerLifecycleObserver.unobserve(resultListener);
                    }
                }
            }
            return isConsumed[0];
        }

        return false;
    }

    @Override
    public void onTimeout(String reqId, int reqSn, Object[] reqParas) {
        T req = Enum.valueOf(enumT, unprefix(reqId));
        Session s = getSession(reqSn);
        IResultListener resultListener = s.resultListener;
        KLog.p(KLog.DEBUG,"req=%s, sid=%s, resultListener=%s", req, s.id, resultListener);
        SessionProcessor<T> processor = s.processor;
        boolean[] isConsumed = new boolean[]{false};
        if (null != processor){
            processor.onTimeout(resultListener, req, reqParas, isConsumed);
        }
        if (!isConsumed[0]){
            // 超时未被消费则此处通知用户超时
            reportTimeout(resultListener);
        }
        sessions.remove(s);
        if (!containsListener(resultListener)) {
            listenerLifecycleObserver.unobserve(resultListener);
        }
    }


    @Override
    public void onNtf(String ntfId, Object ntfContent) {
        T ntf = Enum.valueOf(enumT, unprefix(ntfId));
        Set<ILifecycleOwner> listeners = ntfListeners.get(ntf);
        StringBuilder sb = new StringBuilder();
        for (ILifecycleOwner listener : Objects.requireNonNull(listeners)) {
            sb.append(listener).append("\n");
        }
        KLog.p(KLog.DEBUG,"ntf=%s, ntfContent=%s\nlisteners=%s", ntf, ntfContent, sb.toString());

        onNtf(ntf, ntfContent, listeners);
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
        reportFailed(errorCode, null, listener,false);
    }

    /**
     * 上报用户请求失败
     * */
    protected void reportFailed(int errorCode, Object errorInfo, IResultListener listener){
        reportFailed(errorCode, errorInfo, listener, false);
    }

    /**
     * 上报用户请求超时
     * */
    protected void reportTimeout(IResultListener listener){
        reportTimeout(listener, false);
    }


    /**
     * @param onlyIfListenerAliveInSession 仅当listener仍存活于会话中时才上报。
     *                                     listener可能在会话过程中被解绑了，详情可参考{@link #req(Enum, SessionProcessor, IResultListener, Object...)}
     * */
    protected void reportProgress(Object progress, IResultListener listener, boolean onlyIfListenerAliveInSession){
        if (null == listener){
            KLog.p(KLog.WARN, "ResultListener is null!");
            return;
        }
        if (onlyIfListenerAliveInSession && !containsRspListener(listener)){
            KLog.p(KLog.WARN, "not alive in session! listener %s", listener);
            return;
        }
        listener.onProgress(progress);
    }


    /**
     * 上报用户请求成功
     * */
    protected void reportSuccess(Object result, IResultListener listener, boolean onlyIfListenerAliveInSession){
        if (null == listener){
            KLog.p(KLog.WARN, "ResultListener is null!");
            return;
        }
        if (onlyIfListenerAliveInSession && !containsRspListener(listener)){
            KLog.p(KLog.WARN, "not alive in session! listener %s", listener);
            return;
        }
        listener.onArrive(true);
        listener.onSuccess(result);
    }


    /**
     * 上报用户请求失败
     * */
    protected void reportFailed(int errorCode, Object errorInfo, IResultListener listener, boolean onlyIfListenerAliveInSession){
        if (null == listener){
            KLog.p(KLog.WARN, "ResultListener is null!");
            return;
        }
        if (onlyIfListenerAliveInSession && !containsRspListener(listener)){
            KLog.p(KLog.WARN, "not alive in session! listener %s", listener);
            return;
        }
        listener.onArrive(false);
        listener.onFailed(errorCode, errorInfo);
    }

    /**
     * 上报用户请求超时
     * */
    protected void reportTimeout(IResultListener listener, boolean onlyIfListenerAliveInSession){
        if (null == listener){
            KLog.p(KLog.WARN, "ResultListener is null!");
            return;
        }
        if (onlyIfListenerAliveInSession && !containsRspListener(listener)){
            KLog.p(KLog.WARN, "not alive in session! listener %s", listener);
            return;
        }
        listener.onArrive(false);
        listener.onTimeout();
    }


    /**
     * 注册通知监听器
     * 若要处理通知需override此方法。
     * @return 通知监听器类型到负责处理的通知类型的映射。
     * */
    protected Map<Class<? extends ILifecycleOwner>, T[]> regNtfListener(){return null;}


    /**
     * 通知抵达
     * 若要处理通知需override此方法以及{@link #regNtfListener()}
     * @param ntf 通知消息
     * @param ntfContent 通知内容，具体类型由通知消息决定
     * @param ntfListeners 通知监听器集合
     * */
    protected void onNtf(T ntf, Object ntfContent, Set<ILifecycleOwner> ntfListeners){}


    /**
     * 设置是否禁止下发请求（全局性，针对所有caster）。
     * 若禁止则所有caster的请求不会下发而是会被缓存，直到禁令解除。
     * 默认不禁止。
     * NOTE：一般情况下仅当全局禁令和模块禁令都解除时请求才会下发；
     *      但是，用户可以在请求时指明忽略禁令{@link #req(boolean, boolean, Enum, SessionProcessor, IResultListener, Object...)}
     * */
    protected static void disableReqGlobally(boolean disable){
        if (reqGloballyDisabled == disable){
            return;
        }
        KLog.p("disable=%s", disable);
        reqGloballyDisabled = disable;
        if (!reqGloballyDisabled) {
            // 禁令解除，尝试下发缓存的请求
            for (Caster c : casters) {
                c.driveReqs();
            }
        }
    }

    /**
     * 设置是否禁止下发请求（仅针对本caster）。
     * 若禁止则该caster的请求不会下发而是会被缓存，直到禁令解除。
     * 默认不禁止。
     * NOTE：仅当全局禁令和模块禁令都解除时请求才会下发；
     *      用户可以在请求时指明忽略禁令{@link #req(boolean, boolean, Enum, SessionProcessor, IResultListener, Object...)}
     * */
    protected void disableReq(boolean disable){
        if (reqDisabled == disable){
            return;
        }
        KLog.p("disable=%s", disable);
        reqDisabled = disable;
        if (!reqDisabled && !reqGloballyDisabled){
            // 禁令解除，尝试下发缓存的请求
            driveReqs();
        }
    }

    /**
     * 推发缓存的请求
     * */
    private void driveReqs(){
        Set<Session> pausedSessions = new LinkedHashSet<>();
        for (Session s : sessions){
            if (s.state == Session.Paused && (!reqDisabled || s.ignoreBan)) {
                pausedSessions.add(s);
            }
        }
        sessions.removeAll(pausedSessions);
        for (Session s : pausedSessions) {
            KLog.p("drive cached session %s", s.id);
            req(false, s.ignoreBan, s.req, s.processor, s.resultListener, s.reqParas);
        }
    }


    /**会话处理器*/
    protected interface SessionProcessor<T>{
        /**
         * 请求已发出。（业务组件接口已调用完成）
         * @param resultListener 结果监听器,req()传入。
         *                 NOTE: 可能为null，如用户传入即为null，
         *                       或者会话过程中监听器被解绑，参见：{@link #req(Enum, SessionProcessor, IResultListener, Object...)}
         * @param req 请求消息，req()传入。
         * @param reqParas 请求参数列表，req()传入，顺序同传入时的
         * @param output  传出参数。有些业务组件接口会通过传出参数反馈结果而非响应消息，比如获取本地配置。
         * */
        default void onReqSent(IResultListener resultListener, T req, Object[] reqParas, Object output){}

        /**
         * 收到响应
         * @param rsp 响应消息
         * @param rspContent 响应内容，具体类型由响应消息决定。
         * @param isConsumed 是否已被消费。出参。true已消费，默认是true。 若未消费该消息继续向下流转到其他会话或通知处理器
         * */
        default void onRsp(T rsp, Object rspContent, IResultListener resultListener, T req, Object[] reqParas, boolean[] isConsumed){}

        /**
         * 会话超时
         * @param isConsumed 超时消息是否已被消费，出参，默认是未消费。若未消费则Caster会接管处理——上报用户已超时。
         * */
        default void onTimeout(IResultListener resultListener, T req, Object[] reqParas, boolean[] isConsumed){}
    }


    private static int sessionCount;
    private class Session {
        private int id;
        private boolean ignoreBan;
        private T req;
        private SessionProcessor<T> processor;
        private IResultListener resultListener;
        private Object[] reqParas;
        private int state = Idle;
        static final int Idle = 0;
        static final int Working = 1;
        static final int Paused = 2;
        static final int End = 3;

        public Session(boolean ignoreBan, T req, SessionProcessor<T> processor, IResultListener resultListener, Object[] reqParas) {
            id = sessionCount++;
            this.ignoreBan = ignoreBan;
            this.req = req;
            this.processor = processor;
            this.resultListener = resultListener;
            this.reqParas = reqParas;
        }
    }

}
