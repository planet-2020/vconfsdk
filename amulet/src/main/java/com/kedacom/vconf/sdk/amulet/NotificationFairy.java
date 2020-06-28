package com.kedacom.vconf.sdk.amulet;

import android.util.Log;

import com.kedacom.vconf.sdk.utils.json.Kson;
import com.kedacom.vconf.sdk.utils.log.KLog;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;


final class NotificationFairy implements IFairy.INotificationFairy{

    private IMagicBook magicBook;

    private Map<String, LinkedHashSet<IListener>> subscribers = new LinkedHashMap<>();

    private ICrystalBall crystalBall;

    NotificationFairy() {}


    @Override
    public boolean subscribe(IListener subscriber, String ntfId) {
        if (null == magicBook){
            KLog.p(KLog.ERROR, "no magicBook ");
            return false;
        }
        if (null == subscriber){
            KLog.p(KLog.ERROR, "null subscriber ");
            return false;
        }
        if (null == magicBook.ntfClass(ntfId)){
            KLog.p(KLog.ERROR, "Unknown notification %s", ntfId);
            return false;
        }

        LinkedHashSet<IListener> subs = subscribers.get(ntfId);
        if (null == subs){
            subs = new LinkedHashSet<>();
            subscribers.put(ntfId, subs);
        }

        subs.add(subscriber);

        return true;
    }

    @Override
    public void unsubscribe(IListener subscriber, String ntfId) {
        if (null == magicBook){
            KLog.p(KLog.ERROR, "no magicBook ");
            return;
        }
        if (null == subscriber){
            KLog.p(KLog.ERROR, "null subscriber ");
            return;
        }
        if (null == magicBook.ntfClass(ntfId)){
            KLog.p(KLog.ERROR, "Unknown notification %s", ntfId);
            return;
        }

        LinkedHashSet<IListener> subs = subscribers.get(ntfId);
        if (null != subs){
            subs.remove(subscriber);
            if (subs.isEmpty()){
                subscribers.remove(ntfId);
            }
        }
    }


    @Override
    public boolean onMsg(String msgName, String msgContent) {
        if (null == magicBook){
            KLog.p(KLog.ERROR, "no magicBook ");
            return false;
        }
        Set<String> ntfIds = magicBook.ntfIds(msgName);
        if (ntfIds==null || ntfIds.isEmpty()){
            return false;
        }
        boolean consumed = false;
        for (String ntfId : ntfIds) {
            Class<?> ntfClass = magicBook.ntfClass(ntfId);
            if (ntfClass == null){
                continue;
            }
            LinkedHashSet<IListener> subs = subscribers.get(ntfId);
            if (null == subs || 0 == subs.size()) {
                continue;
            }

            consumed = true;
            Log.d(TAG, String.format("<-~- %s(%s)\n%s", ntfId, msgName, msgContent));

            Object ntfContent = Kson.fromJson(msgContent, ntfClass);

            for (IListener sub : subs) {
                sub.onNtf(ntfId, ntfContent);
            }
        }

        return consumed;
    }


    @Override
    public void setCrystalBall(ICrystalBall crystalBall) {
//        this.crystalBall = crystalBall;
    }

    @Override
    public void setMagicBook(IMagicBook magicBook) {
        this.magicBook = magicBook;
    }


}