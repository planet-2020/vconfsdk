package com.kedacom.vconf.sdk.base.basement;

import android.util.Log;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;


public final class NotificationFairy implements IFairy.INotificationFairy{

    private static final String TAG = NotificationFairy.class.getSimpleName();

    private static NotificationFairy instance;

    private MagicBook magicBook = MagicBook.instance();
    private JsonProcessor jsonProcessor = JsonProcessor.instance();

    private Map<String, LinkedHashSet<IListener>> subscribers = new LinkedHashMap<>();

    private ICrystalBall crystalBall;

    private NotificationFairy(){}

    public synchronized static NotificationFairy instance() {
        if (null == instance) {
            instance = new NotificationFairy();
        }

        return instance;
    }


    @Override
    public boolean subscribe(IListener subscriber, String ntfName) {
        if (null == subscriber){
            Log.e(TAG, "null subscriber ");
            return false;
        }

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
    public void unsubscribe(IListener subscriber, String ntfName) {
        if (null == subscriber){
            Log.e(TAG, "null subscriber ");
            return;
        }

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
    public boolean onMsg(String msgId, String msgContent) {
        String msgName = magicBook.getMsgName(msgId);
        if (!magicBook.isNotification(msgName)){
            Log.w(TAG, "Unknown notification "+ msgName);
            return false;
        }
        LinkedHashSet<IListener> subs = subscribers.get(msgName);
        if (null == subs || 0==subs.size()){
            Log.w(TAG, "no subscriber for "+ msgName);
            return false;
        }

        Log.d(TAG, String.format("<-~- %s(%s)\n%s", msgName, msgId, msgContent));

        Object ntfContent = jsonProcessor.fromJson(msgContent, magicBook.getRspClazz(msgName));

        for (IListener sub : subs){
            sub.onNtf(msgName, ntfContent);
        }

        return false;
    }


    @Override
    public void setCrystalBall(ICrystalBall crystalBall) {
        this.crystalBall = crystalBall;
    }

}