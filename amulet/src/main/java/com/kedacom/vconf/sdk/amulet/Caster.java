package com.kedacom.vconf.sdk.amulet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kedacom.vconf.sdk.utils.lifecycle.ListenerLifecycleObserver;
import com.kedacom.vconf.sdk.utils.log.KLog;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
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

    /**
     * 设置ICrystalBall#IListener优先级，越小优先级越高。
     * 让IFairy.ISessionFairy优先级高于IFairy.INotificationFairy以保证ISessionFairy优先消费上报的消息。
     * */
    private static int count = 0;
    private static final int SESSION_FAIRY_BASE_PRIORITY = 0;
    private static final int NOTIFICATION_FAIRY_BASE_PRIORITY = SESSION_FAIRY_BASE_PRIORITY+10000;

    private final Set<Session> sessions = new LinkedHashSet<>();
    private final Map<T, NtfProcessor<T>> ntfProcessorMap = new LinkedHashMap<>();
    private final Map<T, LinkedHashSet<Object>> ntfListenersMap = new LinkedHashMap<>();

    private ListenerLifecycleObserver listenerLifecycleObserver;

    private Class<T> enumT;

    private String msgPrefix;

    @SuppressWarnings("ConstantConditions")
    protected Caster(){
        IMagicBook magicBook = null;
        enumT = (Class<T>)((ParameterizedType)getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        try {
            Class<?> magicBookClz = Class.forName(enumT.getPackage().getName()+".Msg$$Generated");
            Constructor<?> ctor = magicBookClz.getDeclaredConstructor();
            ctor.setAccessible(true);
            magicBook = (IMagicBook) ctor.newInstance();
            msgPrefix = magicBook.getChapter()+"_";
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

        crystalBall.addListener(sessionFairy, SESSION_FAIRY_BASE_PRIORITY+count);
        crystalBall.addListener(notificationFairy, NOTIFICATION_FAIRY_BASE_PRIORITY+count);
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
                delListener(listener);
            }
        });

        Map<T[], NtfProcessor<T>> ntfsProcessors = subscribeNtfs();
        if (null != ntfsProcessors){
            for (T[] ntfs : ntfsProcessors.keySet()){
                NtfProcessor<T> ntfProcessor = ntfsProcessors.get(ntfs);
                for (T ntf : ntfs){
                    if (notificationFairy.subscribe(this, prefixMsg(ntf.name()))) {
                        ntfProcessorMap.put(ntf, ntfProcessor);
                    }
                }
            }
        }

    }


    /**会话处理器*/
    protected interface SessionProcessor<T>{
        /**
         * 请求已发出。（业务组件接口已返回）
         * @param resultListener 结果监听器,req()传入。
         *                 NOTE: 可能为null。如用户传入即为null，或者会话过程中监听器被销毁，
         * @param req 请求ID，req()传入。
         * @param reqParas 请求参数列表，req()传入，顺序同传入时的
         * */
        default void onReqSent(IResultListener resultListener, T req, Object[] reqParas){}

        /**
         * 收到响应
         * @param rsp 响应ID
         * @param rspContent 响应内容，具体类型由响应ID决定。
         * @return 是否已消费该响应。true已消费。若未消费则该响应会被传递给其他期待该响应的会话或者订阅了该响应（通知）消息的通知处理器。
         * */
        default boolean onRsp(T rsp, Object rspContent, IResultListener resultListener, T req, Object[] reqParas){return true;};

        /**
         * 会话超时
         * @return 是否已消费。true已消费。若未消费则Caster会接管处理——上报用户已超时。
         * */
        default boolean onTimeout(IResultListener resultListener, T req, Object[] reqParas){return false;}
    }

    /**通知处理器*/
    protected interface NtfProcessor<T>{
        /**
         * @param ntf 通知ID
         * @param ntfContent 通知内容，具体类型由通知ID决定
         * @param ntfListeners 通知监听器集合
         * */
        void process(T ntf, Object ntfContent, Set<Object> ntfListeners);
    }

    /**订阅通知
     * 若要订阅通知需override此方法。
     * */
    protected Map<T[], NtfProcessor<T>> subscribeNtfs(){return null;}


    /**
     * 会话请求。
     * NOTE: 不同于{@link #set(Enum, Object...)}和{@link #get(Enum, Object...)}，
     * 该接口是异步的，响应通过rspProcessor反馈。
     * @param req 请求Id
     * @param sessionProcessor 会话处理器。为null则表示不关注响应或没有响应。
     * @param reqParas 请求参数列表，可以没有。
     * @param resultListener 请求结果监听器。
     *                       NOTE: 会话持有了resultListener的引用，而resultListener可能为生命周期对象如Activity，
     *                       若resultListener生命周期结束于会话之前则会话需要及时释放resultListener引用以避免内存泄漏以及不被用户期望的请求结果回调。
     *                       释放引用有两种方式：手动释放和自动释放。
     *                       手动释放指用户通过调用{@link #delListener(Object)}释放，自动释放则由Caster自动管理，目前实现是在resultListener生命周期结束时释放。
     *                       手动释放很很繁琐且易遗漏出错，所以最好由Caster自动释放。
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
     *                       IResultListener定义不要使用lambada，否则绑定可能失效（取决于java编译器具体实现），AS会提示转换为lambada，请suppress；
     *                       建议用户尽量参考例一例二；
     *
     * */
    protected void req(@NonNull T req, SessionProcessor<T> sessionProcessor, IResultListener resultListener, Object... reqParas){
        String prefixedReq = prefixMsg(req.name());
        Session s = new Session(req, sessionProcessor, resultListener);
        if (!sessionFairy.req(this, prefixedReq, s.id, reqParas)){
            KLog.p(KLog.ERROR, "%s failed", req);
            return;
        }
        sessions.add(s);
        listenerLifecycleObserver.tryObserve(resultListener);
        KLog.p(KLog.DEBUG,"req=%s, sid=%s, sessionProcessor=%s, resultListener=%s", req, s.id, sessionProcessor, resultListener);
    }

    /**
     * 取消会话请求。
     * @param req 请求id
     * @param resultListener 结果监听器，为null则取消所有请求id为req的会话。
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
     * 添加通知监听器
     * @param ntfListener 通知监听器。
     *                    NOTE: Caster会尝试监测该监听器的生命周期，并做相应处理，
     *                    参见{@link #req(Enum, SessionProcessor, IResultListener, Object...)}}对IResultListener的处理
     * */
    protected void addNtfListener(@NonNull T ntfId, @NonNull Object ntfListener){
        KLog.p(KLog.DEBUG,"ntfId=%s, ntfListener=%s", ntfId, ntfListener);
        Set<Object> listeners = ntfListenersMap.get(ntfId);
        if (null == listeners){
            listeners = new LinkedHashSet<>();
        }
        listeners.add(ntfListener);
        listenerLifecycleObserver.tryObserve(ntfListener);
    }

    /**
     * 添加通知监听器
     * */
    protected void addNtfListener(@NonNull T[] ntfIds, @NonNull Object ntfListener){
        for (T ntfId : ntfIds){
            addNtfListener(ntfId, ntfListener);
        }
    }


    /**
     * 获取通知监听器
     * */
    protected Set<Object> getNtfListeners(T ntfId){
        return ntfListenersMap.get(ntfId.name());
    }


    /**
     * 删除监听器。
     * */
    public void delListener(@NonNull Object listener){
        delResultListener(listener);
        delNtfListener(null, listener);
    }

    /**
     * 删除结果监听器
     * */
    protected void delResultListener(@NonNull Object listener){
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

    /**
     * 删除通知监听器
     * */
    protected void delNtfListener(@Nullable T[] ntfIds, Object listener){
        if (null != ntfIds) {
            for (T ntfId : ntfIds) {
                Set<Object> listeners = ntfListenersMap.get(ntfId);
                if (null != listeners) {
                    KLog.p(KLog.DEBUG,"delete ntfListener, ntfId=%s, listener=%s", ntfId, listener);
                    listeners.remove(listener);
                }
            }
        }else{
            for (Set<Object> ntfListeners : ntfListenersMap.values()) {
                ntfListeners.remove(listener);
            }
        }
        if(!containsListener(listener)){
            listenerLifecycleObserver.unobserve(listener);
        }
    }


    protected boolean containsListener(Object listener){
        boolean got = false;
        for (Set<Object> listeners : ntfListenersMap.values()){
            if (listeners.contains(listener)){
                got = true;
                break;
            }
        }
        if (!got){
            for (Session s : sessions){
                if (s.resultListener == listener){
                    got = true;
                    break;
                }
            }
        }
        return got;
    }

    private String prefixMsg(String msg){
        return msgPrefix +msg;
    }

    private String unprefixMsg(String prefixedMsg){
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

    private NtfProcessor<T> getNtfProcessor(T ntf){
        NtfProcessor<T> processor = ntfProcessorMap.get(ntf);
        if (null == processor){
            throw new RuntimeException("no processor for "+ntf);
        }
        return processor;
    }


    @Override
    public void onReqSent(boolean hasRsp, String reqName, int reqSn, Object[] reqParas) {
        T req = T.valueOf(enumT, unprefixMsg(reqName));
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
    public boolean onRsp(boolean bLast, String rspName, Object rspContent, String reqName, int reqSn, Object[] reqParas) {
        T req = T.valueOf(enumT, unprefixMsg(reqName));
        T rsp = T.valueOf(enumT, unprefixMsg(rspName));
        Session s = getSession(reqSn);
        IResultListener resultListener = s.resultListener;
        SessionProcessor<T> processor = s.processor;
        KLog.p(KLog.DEBUG,"req=%s, sid=%s, rsp=%s, resultListener=%s, \nrspContent=%s", req, s.id, rsp, resultListener, rspContent);
        if (null != processor){
            boolean bConsumed = processor.onRsp(rsp, rspContent, resultListener, req, reqParas);
            if (bConsumed){
                if (bLast){
                    sessions.remove(s);
                    listenerLifecycleObserver.unobserve(resultListener);
                }
            }
            return bConsumed;
        }

        return false;
    }

    @Override
    public void onTimeout(String reqName, int reqSn, Object[] reqParas) {
        T req = T.valueOf(enumT, unprefixMsg(reqName));
        Session s = getSession(reqSn);
        sessions.remove(s);
        IResultListener resultListener = s.resultListener;
        listenerLifecycleObserver.unobserve(resultListener);
        KLog.p(KLog.DEBUG,"req=%s, sid=%s, resultListener=%s", req, s.id, resultListener);
        SessionProcessor<T> processor = s.processor;
        boolean bConsumed = false;
        if (null != processor){
            bConsumed = processor.onTimeout(resultListener, req, reqParas);
        }
        if (!bConsumed){
            // 超时未被消费则此处通知用户超时
            reportTimeout(resultListener);
        }
    }


    @Override
    public void onNtf(String ntfName, Object ntfContent) {
        String unPrefixedNtfName = unprefixMsg(ntfName);
        T ntf = T.valueOf(enumT, unPrefixedNtfName);
        Set<Object> listeners = ntfListenersMap.get(ntf);
        StringBuilder sb = new StringBuilder();
        if (null != listeners) {
            for (Object listener : listeners) {
                sb.append(listener).append("\n");
            }
        }
        KLog.p(KLog.DEBUG,"ntf=%s, ntfContent=%s\nlisteners=%s", ntf, ntfContent, sb.toString());
        NtfProcessor<T> processor = getNtfProcessor(ntf);
        processor.process(ntf, ntfContent, ntfListenersMap.get(ntf));
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
