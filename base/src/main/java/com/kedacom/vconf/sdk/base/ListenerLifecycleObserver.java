package com.kedacom.vconf.sdk.base;



import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.annotation.NonNull;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class ListenerLifecycleObserver implements DefaultLifecycleObserver {

    private Callback cb;

    /*
     * 绑定到被监控的生命周期拥有者的监听器。
     * key为被监控的生命周期拥有者，value为跟该生命周期拥有者绑定的监听器。
     * */
    private Map<LifecycleOwner, Set<Object>> lifecycleOwnerBindListeners;

    ListenerLifecycleObserver(Callback cb){
        this.cb = cb;
        lifecycleOwnerBindListeners = new HashMap<>();
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
    boolean tryObserve(Object listener){
        if (null == listener){
            return false;
        }

        if (hasObserved(listener)){
            return true;
        }

        if (listener instanceof ILifecycleOwner &&
                null != ((ILifecycleOwner)listener).getLifecycleOwner()){ // listener指定了其需要绑定的生命周期对象
//            KLog.p("%s getLifecycleOwner = %s", listener, ((ILifecycleOwner)listener).getLifecycleOwner());
            observe(((ILifecycleOwner)listener).getLifecycleOwner(), listener);
        }else if (listener instanceof LifecycleOwner){ // listener本身即为生命周期拥有者
            // 监控该listener。该listener作为被监控的生命周期拥有者亦可被其他listener绑定。
//            KLog.p("%s is LifecycleOwner itself", listener);
            observe((LifecycleOwner) listener, listener);
        }else{ // 没有指定绑定的生命周期对象，自身也不是生命周期拥有者，则尝试监控其外部类对象的生命周期（如果有外部类对象且该外部类对象拥有生命周期）。
            Object encloser = getEncloserEx(listener);
            LifecycleOwner owner = getBoundLifecycleOwner(encloser);
//            KLog.p("%s's encloser %s has bind LifecycleOwner %s", listener, encloser, owner);
            if (null != owner){ // 外部类对象有绑定的生命周期对象
                observe(owner, listener); // 则将该listener绑定到其外部类所绑定的生命周期对象
            }else{
                if (encloser instanceof LifecycleOwner){ // 外部类对象自身为生命周期对象
                    observe((LifecycleOwner) encloser, listener); // listener绑定到外部类对象
                }else{
                    /* 该listener不能感知生命周期事件，因为它没有符合如下任一项：
                    1、指定了绑定的生命周期对象；
                    2、自身为生命周期拥有者；
                    3、其外部类对象（如果有）指定绑定了的生命周期对象；
                    4、其外部类对象（如果有）为生命周期拥有者；
                    */
                    KLog.p(KLog.WARN, "%s can not perceive lifecycle", listener);
                    return false;
                }
            }

        }

        return true;

    }


    /**取消监控生命周期。*/
    void unobserve(Object listener){
        for (Map.Entry<LifecycleOwner, Set<Object>> owner : lifecycleOwnerBindListeners.entrySet()){
            LifecycleOwner key = owner.getKey();
            Set<Object> val = owner.getValue();
            if (val.contains(listener)){
                val.remove(listener);
                KLog.p(KLog.WARN,"unbind %s's lifecycle from %s", listener, key);
                if (val.isEmpty()){
                    // owner其下没有与之绑定的listener则取消对该owner的监控。
                    key.getLifecycle().removeObserver(this);
                    lifecycleOwnerBindListeners.remove(key);
                    KLog.p(KLog.WARN, "unobserve %s's lifecycle", key);
                }
                return;
            }
        }
    }


    private LifecycleOwner getBoundLifecycleOwner(Object listener){
        for (LifecycleOwner owner : lifecycleOwnerBindListeners.keySet()){
            Set<Object> listeners = lifecycleOwnerBindListeners.get(owner);
            if (listeners.contains(listener)){
                return owner;
            }
        }
        return null;
    }

    private boolean isLifecycleOwnerExists(Object owner){
        return lifecycleOwnerBindListeners.containsKey(owner);
    }

    private boolean hasObserved(Object listener){
        for (Map.Entry<LifecycleOwner, Set<Object>> owner : lifecycleOwnerBindListeners.entrySet()){
            if (owner.getValue().contains(listener)){
                return true;
            }
        }
        return false;
    }

    /**
     * 监控生命周期。
     *
    *  @param owner 生命周期拥有者
     * @param listener 生命周期附庸者，绑定到owner上从而拥有和owner一样的生命周期。
     *                 可以为null表示owner暂时没有附庸者，owner自身生命周期仍被监控。
    * */
    private void observe(LifecycleOwner owner, Object listener){
        KLog.p(KLog.WARN,"bind %s's lifecycle to %s", listener, owner);
        if (isLifecycleOwnerExists(owner)) { // 该生命周期对象已经被监控了
            lifecycleOwnerBindListeners.get(owner).add(listener); // 绑定该listener到该生命周期对象
        }else { // 该生命周期对象尚未被监控
            Set<Object> listeners = new HashSet<>();
            listeners.add(listener);
            lifecycleOwnerBindListeners.put(owner, listeners); // 绑定该listener到该生命周期对象
            owner.getLifecycle().addObserver(this); // 监控该生命周期对象
            KLog.p(KLog.WARN, "observe %s's lifecycle", owner);
        }
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
        if (null != cb){
            notify(lifecycleOwnerBindListeners.get(owner), cb::onListenerResumed);
        }
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        if (null != cb){
            notify(lifecycleOwnerBindListeners.get(owner), cb::onListenerPause);
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (null != cb){
            notify(lifecycleOwnerBindListeners.get(owner), cb::onListenerStop);
        }
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        if (null != cb){
            notify(lifecycleOwnerBindListeners.get(owner), cb::onListenerDestroy);
        }
        Set<Object> listeners = lifecycleOwnerBindListeners.remove(owner);
        if (null != listeners) {
            for (Object listener : listeners) {
                KLog.p(KLog.WARN, "unbind %s's lifecycle from %s", listener, owner);
            }
        }
        KLog.p(KLog.WARN, "unobserve %s's lifecycle", owner);
    }

    private void notify(Set<Object> listeners, Action action){
        if (null != listeners){
            for (Object listener : listeners){
                action.act(listener);
            }
        }
    }

    private interface Action {
        void act(Object listener);
    }

    public interface Callback{
        default void onListenerResumed(Object listener){}
        default void onListenerPause(Object listener){}
        default void onListenerStop(Object listener) {}
        default void onListenerDestroy(Object listener) {}
    }

}
