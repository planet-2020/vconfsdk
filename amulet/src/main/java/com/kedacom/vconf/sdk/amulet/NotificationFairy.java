package com.kedacom.vconf.sdk.amulet;

import android.util.Log;

import com.kedacom.vconf.sdk.utils.json.Kson;
import com.kedacom.vconf.sdk.utils.log.KLog;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;


final class NotificationFairy implements IFairy.INotificationFairy{
    private static final String TAG = NotificationFairy.class.getSimpleName();

    private MagicBook magicBook = MagicBook.instance();

    private Map<String, LinkedHashSet<IListener>> subscribers = new LinkedHashMap<>();

    private ICrystalBall crystalBall;

    private static NotificationFairy instance = null;

    private NotificationFairy() {}

    public static NotificationFairy getInstance() {
        if (instance == null) {
            synchronized (NotificationFairy.class) {
                if (instance == null) {
                    instance = new NotificationFairy();
                }
            }
        }
        return instance;
    }

    @Override
    public boolean subscribe(IListener subscriber, String ntfName) {
        if (null == subscriber){
            KLog.p(KLog.ERROR, "null subscriber ");
            return false;
        }

        if (!magicBook.isNotification(ntfName)){
            KLog.p(KLog.ERROR, "Unknown notification %s", ntfName);
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
            KLog.p(KLog.ERROR, "null subscriber ");
            return;
        }

        if (!magicBook.isNotification(ntfName)){
            KLog.p(KLog.ERROR, "Unknown notification %s", ntfName);
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
            KLog.p(KLog.ERROR, "Unknown notification %s", msgName);
            return false;
        }
        LinkedHashSet<IListener> subs = subscribers.get(msgName);
        if (null == subs || 0==subs.size()){
            KLog.p(KLog.ERROR, "no subscriber for %s", msgName);
            return false;
        }

        Log.d(TAG, String.format("<-~- %s(%s)\n%s", msgName, msgId, msgContent));

        Object ntfContent = Kson.fromJson(msgContent, magicBook.getRspClazz(msgName));

        for (IListener sub : subs){
            sub.onNtf(msgName, ntfContent);
        }

        return false;
    }


    @Override
    public void setCrystalBall(ICrystalBall crystalBall) {
//        this.crystalBall = crystalBall;
    }

}