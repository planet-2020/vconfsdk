package com.kedacom.vconf.sdk.base;



import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.annotation.NonNull;

import com.kedacom.vconf.sdk.base.KLog;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ListenerLifecycleObserver implements DefaultLifecycleObserver {

    private Callback cb;
    private Map<LifecycleOwner, Set<Object>> ownerEnclosedListeners;

    ListenerLifecycleObserver(Callback cb){
        this.cb = cb;
        ownerEnclosedListeners = new HashMap<>();
    }

    /**尝试监控对象的生命周期。
     *
     * 若受监控对象本身即为生命周期拥有者，如androidx.appcompat.app.AppCompatActivity, androidx.fragment.app.Fragment，则监控该对象本身，
     * 否则尝试监控该对象的直接外部类对象，如:
     * class LoginActivity extends AppCompatActivity{
     *  tryObserve(new LoginResultListener(){...})
     * }
     * interface LoginResultListener{
     *     onLoginResult(int resultCode, Object result);
     * }
     * 则监控的是LoginResultListener匿名内部类中引用的外部类对象——LoginActivity对象。
     *
     * XXX 注意：1、该方法使用了反射，可能随着所使用java版本的变化而失效；
     *           2、对于受监控对象自身不拥有生命周期的情形，仅尝试监控其直接外部类，若该对象的直接外部类也不是生命周期拥有者则返回尝试监控失败，不做进一步递归（即尝试监控其外部类的外部类...）。
     *           3、对于受监控对象为lambda的情形，若该lambda表达式内未使用外部类对象或外部类对象的成员则无法获取其外部类对象的引用，返回尝试监控失败。此种情形在实际使用中会较常遇见，使用者需特别留意，评估由此带来的影响。
     *
     * @param listener 受监控对象
     * */
    boolean tryObserve(Object listener){
        KLog.p("%s instanceof LifecycleOwner? %s", listener, listener instanceof LifecycleOwner);
        if (listener instanceof LifecycleOwner){
            // listener本身即为生命周期拥有者，则直接监控其生命周期
            ((LifecycleOwner)listener).getLifecycle().addObserver(this);
            return true;
        }

        /* listener本身不拥有生命周期则尝试监控其外部类的生命周期（若其外部类为生命周期拥有者）。
         注意：不支持多级嵌套。即仅检查listener的直接外部类，不再往上递归。*/
        Object encloser = getEncloser(listener);
        KLog.p("%s instanceof LifecycleOwner? %s", encloser, encloser instanceof LifecycleOwner);
        if (encloser instanceof LifecycleOwner){
            LifecycleOwner owner = (LifecycleOwner) encloser;
            owner.getLifecycle().addObserver(this);
            Set<Object> objSet = ownerEnclosedListeners.get(owner);
            if (null == objSet){
                objSet = new HashSet<>();
                ownerEnclosedListeners.put(owner, objSet);
            }
            objSet.add(listener);

            return true;
        }

        return false;
    }

    /**获取对象的外部类对象。*/
    private Object getEncloser(Object obj){
        Class<?> clz = obj.getClass();
        Class<?> enclosingClz = clz.getEnclosingClass();
        Object encloser = null;
        Field enclosingClzRef;
        //for debug
//            Field[] fields = clz.getDeclaredFields();
//            for (Field field : fields){
//                KLog.p("======= field: %s", field);
//            }
        if (null != enclosingClz) { // 内部类
            try {
                enclosingClzRef = clz.getDeclaredField("this$0");
            } catch (NoSuchFieldException e) {
                return null;
            }
        }else { // lambda
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
            notify(owner, cb::onListenerResumed);
        }
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        if (null != cb){
            notify(owner, cb::onListenerPause);
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (null != cb){
            notify(owner, cb::onListenerStop);
        }
//        ownerEnclosedListeners.remove(owner);
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        if (null != cb){
            notify(owner, cb::onListenerDestroy);
        }
        ownerEnclosedListeners.remove(owner);
    }

    private void notify(LifecycleOwner owner, Action action){
        action.act(owner);

        if (ownerEnclosedListeners.keySet().contains(owner)){
            Set<Object> objects = ownerEnclosedListeners.get(owner);
            for (Object obj : objects){
                action.act(obj);
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
