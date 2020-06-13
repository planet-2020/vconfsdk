package com.kedacom.vconf.sdk.amulet;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.kedacom.vconf.sdk.utils.json.Kson;
import com.kedacom.vconf.sdk.utils.log.KLog;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;


final class NotificationFairy implements IFairy.INotificationFairy{

    private static MagicBook magicBook = MagicBook.instance();

    private Map<String, LinkedHashSet<IListener>> subscribers = new LinkedHashMap<>();

    private ICrystalBall crystalBall;

    NotificationFairy() {}


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
        String msgName = magicBook.getRspName(msgId);
        if (null == msgName){
            return false;
        }
        if (!magicBook.isNotification(msgName)){
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


    @Override
    public void emit(String ntfName, Object ntfContent) {
        String ntfId = magicBook.getRspId(ntfName);
        String ntfCont = Kson.toJson(ntfContent);
        int delay = magicBook.getRspDelay(ntfName);
        handler.postDelayed(() -> onMsg(ntfId, ntfCont), delay);
    }

    private static Handler handler = new Handler(Looper.getMainLooper());

}