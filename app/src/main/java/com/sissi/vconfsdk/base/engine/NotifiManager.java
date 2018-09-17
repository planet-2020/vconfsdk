package com.sissi.vconfsdk.base.engine;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 通知处理器
 *
 * Created by Sissi on 1/9/2017.
 */
final class NotifiManager implements ISubscribeProcessor, INotificationProcessor, INotificationEmitter{

    private static final String TAG = NotifiManager.class.getSimpleName();

    private static NotifiManager instance;

    private NativeInteractor nativeInteractor;  // NativeInteractor 应该是个接口，并且注入进来， NotifiManager不感知具体NativeInteractor实现。它所需要的只是一个发射器。

    private MessageRegister messageRegister;

    private Map<String, Set<Handler>> subscribers;

    private NotifiManager(){
        nativeInteractor = NativeInteractor.instance();
        nativeInteractor.setNotificationProcessor(this);
        messageRegister = MessageRegister.instance();
        if (NativeEmulatorOnOff.on) {
            nativeInteractor.setNativeEmulator(NativeEmulator.instance());
        }

        subscribers = new HashMap<>();
    }

    synchronized static NotifiManager instance() {
        if (null == instance) {
            instance = new NotifiManager();
        }

        return instance;
    }



    @Override
    public synchronized boolean subscribe(Handler subscriber, String ntfId) {
        if (null == subscriber){
            return false;
        }

        if (!messageRegister.isNotification(ntfId)){
            Log.e(TAG, "Unknown notification "+ntfId);
            return false;
        }

        Set<Handler> subs = subscribers.get(ntfId);
        if (null == subs){
            subs = new HashSet<>();
            subscribers.put(ntfId, subs);
        }

        subs.add(subscriber);

        return true;
    }

    @Override
    public synchronized void unsubscribe(Handler subscriber, String ntfId) {

        if (null == subscriber){
            return;
        }

        if (!messageRegister.isNotification(ntfId)){
            Log.e(TAG, "Unknown notification "+ntfId);
            return;
        }

        Set<Handler> subs = subscribers.get(ntfId);
        if (null != subs){
            subs.remove(subscriber);
            if (subs.isEmpty()){
                subscribers.remove(ntfId);
            }
        }
    }


    @Override
    public synchronized boolean processNotification(String ntfName, Object ntfContent) {
        Set<Handler> subs = subscribers.get(ntfName);
        if (null == subs || 0==subs.size()){
            return false;
        }

        for (Handler sub : subs){
            Message msg = Message.obtain();
            msg.obj = new ResponseBundle(ntfName, ntfContent, ResponseBundle.NTF);
            sub.sendMessage(msg);
        }

        return true;
    }


    @Override
    public synchronized boolean emitNotification(String ntfName, Object ntfContent) {
        if (!messageRegister.isNotification(ntfName)){
            Log.e(TAG, "Unknown notification "+ntfName);
            return false;
        }

        return nativeInteractor.emulateNotify(ntfName, ntfContent);
    }

}