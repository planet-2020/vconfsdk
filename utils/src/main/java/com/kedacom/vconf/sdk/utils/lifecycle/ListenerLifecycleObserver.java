package com.kedacom.vconf.sdk.utils.lifecycle;


import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.kedacom.vconf.sdk.utils.log.KLog;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public final class ListenerLifecycleObserver implements DefaultLifecycleObserver {

    /*
     * 绑定到被监控的生命周期拥有者的监听器。
     * key为被监控的生命周期拥有者，value为跟该生命周期拥有者绑定的监听器。
     * */
    private static Map<LifecycleOwner, Set<ILifecycleOwner>> lifecycleOwnerBindListeners  = new HashMap<>();
    private static Map<ILifecycleOwner, Callback> listenerCallbackMap = new HashMap<>();

    private static ListenerLifecycleObserver instance;

    private ListenerLifecycleObserver(){}

    public static ListenerLifecycleObserver getInstance(){
        if (instance == null){
            instance = new ListenerLifecycleObserver();
        }
        return instance;
    }

    /**尝试监控对象的生命周期。
     *
     * NOTE：1、该方法使用了反射，可能随着所使用java版本的变化而失效；
     *       2、对于受监控对象为lambda的情形，若该lambda表达式内未使用外部类对象或外部类对象的成员则无法获取其外部类对象的引用，
     *       返回尝试监控失败。此种情形在实际使用中会较常遇见，使用者需特别留意，评估由此带来的影响。
     *
     * @param listener 监听器
     * @return 监听结果。true表示成功监听，后续监听器将感知生命周期对象的生命周期变化事件并通过Callback回调。
     * */
    public boolean tryObserve(@NonNull ILifecycleOwner listener, @NonNull Callback cb){
        if (null == listener){
            return false;
        }

        if (getBoundLifecycleOwner(listener) != null){
            return true;
        }

        if (null != listener.getLifecycleOwner()){ // listener指定了其需要绑定的生命周期对象
//            KLog.p("%s getLifecycleOwner = %s", listener, ((ILifecycleOwner)listener).getLifecycleOwner());
            observe(listener.getLifecycleOwner(), listener, cb);
        }else if (listener instanceof LifecycleOwner){ // listener本身即为生命周期拥有者
            // 监控该listener。该listener作为被监控的生命周期拥有者亦可被其他listener绑定。
//            KLog.p("%s is LifecycleOwner itself", listener);
            observe((LifecycleOwner) listener, listener, cb);
        }else{ // 没有指定绑定的生命周期对象，自身也不是生命周期拥有者，则尝试监控其外部类对象的生命周期
            Object encloser = getEncloserEx(listener);
            if (encloser instanceof ILifecycleOwner){
                LifecycleOwner owner = getBoundLifecycleOwner((ILifecycleOwner) encloser);
                if (null != owner){ // 外部类对象有绑定的生命周期对象
                    observe(owner, listener, cb); // 则将该listener绑定到其外部类所绑定的生命周期对象
                }else{
                    return false;
                }
            }else if (encloser instanceof LifecycleOwner){ // 外部类对象自身为生命周期对象
                observe((LifecycleOwner) encloser, listener, cb); // listener绑定到外部类对象
            }else{
                /* 该listener不能感知生命周期事件，因为它没有符合如下任一项：
                1、指定了绑定的生命周期对象；
                2、自身为生命周期拥有者；
                3、其外部类对象（如果有）指定绑定了的生命周期对象；
                4、其外部类对象（如果有）为生命周期拥有者；
                */
                KLog.p(KLog.DEBUG, "%s can not perceive lifecycle", listener);
                return false;
            }
        }

        return true;

    }


    /**取消监控生命周期。*/
    public void unobserve(@NonNull ILifecycleOwner listener){
        if (null == listener){
            return;
        }
        for (Map.Entry<LifecycleOwner, Set<ILifecycleOwner>> owner : lifecycleOwnerBindListeners.entrySet()){
            LifecycleOwner key = owner.getKey();
            Set<ILifecycleOwner> val = owner.getValue();
            if (val.contains(listener)){
                val.remove(listener);
                listenerCallbackMap.remove(listener);
                KLog.p(KLog.DEBUG,"unbind %s's lifecycle from %s", listener, key);
                if (val.isEmpty()){
                    // owner其下没有与之绑定的listener则取消对该owner的监控。
                    key.getLifecycle().removeObserver(this);
                    lifecycleOwnerBindListeners.remove(key);
                    KLog.p(KLog.DEBUG, "unobserve %s's lifecycle", key);
                }
                return;
            }
        }
    }


    private LifecycleOwner getBoundLifecycleOwner(ILifecycleOwner listener){
        for (Map.Entry<LifecycleOwner, Set<ILifecycleOwner>> bound : lifecycleOwnerBindListeners.entrySet()){
            if (bound.getValue().contains(listener)){
                return bound.getKey();
            }
        }
        return null;
    }

    /**
     * 监控生命周期。
     *
    *  @param owner 生命周期拥有者
     * @param listener 生命周期附庸者，绑定到owner上从而拥有和owner一样的生命周期。
     *                 可以为null表示owner暂时没有附庸者，owner自身生命周期仍被监控。
    * */
    private void observe(LifecycleOwner owner, ILifecycleOwner listener, Callback cb){
        KLog.p(KLog.DEBUG,"bind %s's lifecycle to %s", listener, owner);
        if (lifecycleOwnerBindListeners.containsKey(owner)) { // 该生命周期对象已经被监控了
            lifecycleOwnerBindListeners.get(owner).add(listener); // 绑定该listener到该生命周期对象
        }else { // 该生命周期对象尚未被监控
            Set<ILifecycleOwner> listeners = new HashSet<>();
            listeners.add(listener);
            lifecycleOwnerBindListeners.put(owner, listeners); // 绑定该listener到该生命周期对象
            owner.getLifecycle().addObserver(this); // 监控该生命周期对象
            KLog.p(KLog.DEBUG, "observe %s's lifecycle", owner);
        }
        listenerCallbackMap.put(listener, cb);
    }


    /**获取对象的外部类对象。*/
    private Object getEncloserEx(Object obj){
        Class<?> clz = obj.getClass();
        Class<?> enclosingClz = clz.getEnclosingClass();
        Object encloser = null;
        Field enclosingClzRef;

        if (null != enclosingClz) { // 为内部类对象的情形
            Field[] fields = clz.getDeclaredFields();
            for (Field field : fields){
//                KLog.p("======= field: name=%s, clz=%s", field.getName(), field.getDeclaringClass());
                if (field.getName().contains("this$")){ // 外部类对象的名称包含"this$"。NOTE: 此法没有官方文档说明，可能随着编译器的变化而失效。
                    Object tmp;
                    field.setAccessible(true);
                    try {
                        tmp = field.get(obj);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                        return null;
                    }
                    if (!enclosingClz.isInstance(tmp)){ // 该对象是外部类的实例。（尽力规避内部类中有定义形如"this$"成员变量的情形）
                        continue;
                    }

                    encloser = tmp;
                    break;
                }
            }

            return encloser;

        }else { // 为lambda对象的情形
            /*lambda可能持有外部类引用也可能未持有。
            若lambda内部引用了外部类或外部类的成员则持有否则未持有。*/
            try {
                enclosingClzRef = clz.getDeclaredField("arg$1");
            } catch (NoSuchFieldException e) {
                try {
                    enclosingClzRef = clz.getDeclaredField("f$0");
                } catch (NoSuchFieldException e1) {
                    return null;
                }
            }
        }

        enclosingClzRef.setAccessible(true);

        try {
            encloser = enclosingClzRef.get(obj);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return encloser;
    }


    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {

    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        act(owner, Lifecycle.Event.ON_RESUME);
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        act(owner, Lifecycle.Event.ON_PAUSE);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        act(owner, Lifecycle.Event.ON_STOP);
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        act(owner, Lifecycle.Event.ON_DESTROY);
        KLog.p(KLog.DEBUG, "unobserve %s's lifecycle", owner);
    }


    @SuppressWarnings("ConstantConditions")
    private void act(LifecycleOwner owner, Lifecycle.Event event){
        Set<ILifecycleOwner> listeners = lifecycleOwnerBindListeners.get(owner);
        if (null != listeners){
            Iterator<ILifecycleOwner> it = listeners.iterator();
            Map<ILifecycleOwner, Callback> cbMap = new HashMap<>();
            while (it.hasNext()){
                ILifecycleOwner listener = it.next();
                Callback cb = listenerCallbackMap.get(listener);
                if (cb != null){
                    cbMap.put(listener, cb);
                }
                if (listener.destroyWhenLifecycleOwner() == event) {
                    KLog.p(KLog.DEBUG, "unbind %s's lifecycle from %s", listener, owner);
                    it.remove();
                    listenerCallbackMap.remove(listener);
                }
            }

            if (event == Lifecycle.Event.ON_RESUME) {
                for (ILifecycleOwner listener : cbMap.keySet()) {
                    cbMap.get(listener).onListenerResumed(listener);
                }
            }else if (event == Lifecycle.Event.ON_PAUSE){
                for (ILifecycleOwner listener : cbMap.keySet()) {
                    cbMap.get(listener).onListenerPause(listener);
                }
            }else if (event == Lifecycle.Event.ON_STOP){
                for (ILifecycleOwner listener : cbMap.keySet()) {
                    cbMap.get(listener).onListenerStop(listener);
                }
            }else if (event == Lifecycle.Event.ON_DESTROY){
                for (ILifecycleOwner listener : cbMap.keySet()) {
                    cbMap.get(listener).onListenerDestroy(listener);
                }
            }
        }
    }

    public interface Callback{
        default void onListenerResumed(Object listener){}
        default void onListenerPause(Object listener){}
        default void onListenerStop(Object listener) {}
        default void onListenerDestroy(Object listener) {}
    }

}
