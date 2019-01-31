package com.kedacom.vconf.sdk.base.basement;

import android.util.Log;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;


public final class NotificationFairy2 implements IFairy2.INotificationFairy{

    private static final String TAG = NotificationFairy2.class.getSimpleName();

    private static NotificationFairy2 instance;

    private MagicBook magicBook = MagicBook.instance();
    private JsonProcessor jsonProcessor = JsonProcessor.instance();

    private Map<String, LinkedHashSet<IListener>> subscribers = new LinkedHashMap<>();

    private ICrystalBall2 crystalBall;

    private NotificationFairy2(){}

    public synchronized static NotificationFairy2 instance() {
        if (null == instance) {
            instance = new NotificationFairy2();
        }

        return instance;
    }


    @Override
    public boolean subscribe(IListener subscriber, String ntfId) {
        if (null == subscriber){
            Log.e(TAG, "null subscriber ");
            return false;
        }

        String ntfName = magicBook.getMsgName(ntfId);

        if (!magicBook.isNotification(ntfName)){
            Log.e(TAG, "Unknown notification "+ntfName);
            return false;
        }

        LinkedHashSet<IListener> subs = subscribers.get(ntfName);
        if (null == subs){
            subs = new LinkedHashSet<>();
            subscribers.put(ntfName, subs);
        }

        subs.add(subscriber);

        return true;
    }

    @Override
    public void unsubscribe(IListener subscriber, String ntfId) {
        if (null == subscriber){
            Log.e(TAG, "null subscriber ");
            return;
        }

        String ntfName = magicBook.getMsgName(ntfId);

        if (!magicBook.isNotification(ntfName)){
            Log.e(TAG, "Unknown notification "+ntfName);
            return;
        }

        LinkedHashSet<IListener> subs = subscribers.get(ntfName);
        if (null != subs){
            subs.remove(subscriber);
            if (subs.isEmpty()){
                subscribers.remove(ntfName);
            }
        }
    }


    @Override
    public boolean onMsg(String msgName, String msgContent) {
        if (!magicBook.isNotification(msgName)){
            Log.w(TAG, "Unknown notification "+ msgName);
            return false;
        }
        LinkedHashSet<IListener> subs = subscribers.get(msgName);
        if (null == subs || 0==subs.size()){
            Log.w(TAG, "no subscriber for "+ msgName);
            return false;
        }

        Log.d(TAG, String.format("<-~- %s\n%s", msgName, msgContent));

        Object ntfContent = jsonProcessor.fromJson(msgContent, magicBook.getNtfClazz(msgName));

        String ntfId = magicBook.getMsgId(msgName);
        for (IListener sub : subs){
            sub.onNtf(ntfId, ntfContent);
        }

        return false;
    }



    @Override
    public void emit(String ntfId) {
        if (null == crystalBall){
            Log.e(TAG, "no crystalBall ");
            return;
        }

        String ntfName = magicBook.getMsgName(ntfId);

        if (!magicBook.isNotification(ntfName)){
            Log.e(TAG, "Unknown notification "+ ntfName);
            return;
        }

        // TODO
//        crystalBall.emit(ntfName);
    }

    // XXX 上层循环调用
//    @Override
//    public void emit(String[] ntfIds) {
//        for (String ntfId : ntfIds){
//            emit(ntfId);
//        }
//    }


    @Override
    public void setCrystalBall(ICrystalBall2 crystalBall) {
        this.crystalBall = crystalBall;
    }

}