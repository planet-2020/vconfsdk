package com.kedacom.vconf.sdk.amulet;


import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.kedacom.vconf.sdk.utils.lang.ClassHelper;
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
            Object encloser = getEnclosingObject(listener);
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
                KLog.p(KLog.DEBUG, "can not perceive lifecycle for %s", listener + ClassHelper.getParents(listener.getClass()));
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
                KLog.p(KLog.DEBUG,"unbind lifecycle from %s for %s", key, listener+ClassHelper.getParents(listener.getClass()));
                if (val.isEmpty()){
                    // owner其下没有与之绑定的listener则取消对该owner的监控。
                    key.getLifecycle().removeObserver(this);
                    lifecycleOwnerBindListeners.remove(key);
                    KLog.p(KLog.DEBUG, "unobserve lifecycle of %s", key);
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
        KLog.p(KLog.DEBUG,"bind lifecycle to %s for %s", owner, listener+ClassHelper.getParents(listener.getClass()));
        if (lifecycleOwnerBindListeners.containsKey(owner)) { // 该生命周期对象已经被监控了
            lifecycleOwnerBindListeners.get(owner).add(listener); // 绑定该listener到该生命周期对象
        }else { // 该生命周期对象尚未被监控
            Set<ILifecycleOwner> listeners = new HashSet<>();
            listeners.add(listener);
            lifecycleOwnerBindListeners.put(owner, listeners); // 绑定该listener到该生命周期对象
            owner.getLifecycle().addObserver(this); // 监控该生命周期对象
            KLog.p(KLog.DEBUG, "observe lifecycle of %s", owner);
        }
        listenerCallbackMap.put(listener, cb);
    }


    /**获取对象的直接外部类对象。
     * @return 返回obj的直接外部类对象，若没有则返回null。
     *
     * NOTE：该方法使用了反射，但依赖的规则没有官方文档说明。
     *      在如下编译环境下验证通过：
     *      jdk：openjdk8或openjdk11
     *      compileOptions {
     *          sourceCompatibility JavaVersion.VERSION_1_8
     *          targetCompatibility JavaVersion.VERSION_1_8
     *      }
     * */
    private Object getEnclosingObject(Object obj){
        Class<?> clz = obj.getClass();
        // 尝试获取该对象的外部类。
        // 我们使用该外部类作为辅助尽力保证我们获取到的外部类对象的正确性
        Class<?> enclosingClz = null;
        try {
            enclosingClz = clz.getEnclosingClass();
        }catch (Throwable e){  // if clz enclosed by lambda IncompatibleClassChangeError will be thrown out!
            ;
        }

        /*
        * 我们尝试利用如下不成文的规则查找外部类对象（请注意我们是在openjdk8验证通过，其他版本的jdk可能实现不一样）：
        * 1、内部类对象持有外部类对象的引用，该引用名以“this$”开头；
        * 2、lambda对象持有外部类对象的引用，若该lambda对象引用了外部类对象或其成员，该引用名为"f$0"；
        * 3、obj被lambda表达式包裹时，obj持有的外部类对象引用“this$”或"f$0"并非指向lambda对象，而是穿透lambda指向上一层的外部类对象；
        * 4、用户自定义的成员中不包含"this$"或"f$0"；
        * */
        Object enclosingObj = getField(obj, "this$", enclosingClz);
        if (enclosingObj == null){
            enclosingObj = getField(obj, "f$0", enclosingClz);
        }

        return enclosingObj;
    }


    private Object getField(Object obj, String filedName, Class<?> fieldCls){
        Class<?> cls = obj.getClass();
        Field[] fields = cls.getDeclaredFields();
        for (Field field : fields){
//                KLog.p("======= field: name=%s, clz=%s", field.getName(), field.getDeclaringClass());
            if (field.getName().startsWith(filedName)){
                Object fieldObj = null;
                field.setAccessible(true);
                try {
                    fieldObj = field.get(obj);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                if (fieldCls == null || fieldCls.isInstance(fieldObj)){ // 除了名字匹配外，我们还尽力验证类型是否匹配
                    return fieldObj; // 找到外部类对象
                }
            }
        }
        return null;
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
        KLog.p(KLog.DEBUG, "unobserve lifecycle of %s", owner);
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
                    KLog.p(KLog.DEBUG, "unbind lifecycle from %s for %s", owner, listener+ClassHelper.getParents(listener.getClass()));
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
