package com.kedacom.vconf.sdk.amulet;

import android.util.Log;

import com.kedacom.vconf.sdk.utils.json.Kson;
import com.kedacom.vconf.sdk.utils.log.KLog;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;


final class NotificationFairy implements IFairy.INotificationFairy{

    private IMagicBook magicBook;

    private Map<String, LinkedHashSet<IListener>> subscribers = new LinkedHashMap<>();

    private ICrystalBall crystalBall;

    NotificationFairy() {}


    @Override
    public boolean subscribe(IListener subscriber, String ntfName) {
        if (null == magicBook){
            KLog.p(KLog.ERROR, "no magicBook ");
            return false;
        }
        if (null == subscriber){
            KLog.p(KLog.ERROR, "null subscriber ");
            return false;
        }
        if (null == magicBook.getRspId(ntfName)){
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
        if (null == magicBook){
            KLog.p(KLog.ERROR, "no magicBook ");
            return;
        }
        if (null == subscriber){
            KLog.p(KLog.ERROR, "null subscriber ");
            return;
        }
        if (null == magicBook.getRspId(ntfName)){
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
        if (null == magicBook){
            KLog.p(KLog.ERROR, "no magicBook ");
            return false;
        }
        List<String> rspNames = magicBook.getRspNames(msgId);
        if (rspNames==null || rspNames.isEmpty()){
            return false;
        }
        for (String rspName : rspNames) {
            LinkedHashSet<IListener> subs = subscribers.get(rspName);
            if (null == subs || 0 == subs.size()) {
                return false;
            }

            Log.d(TAG, String.format("<-~- %s(%s)\n%s", rspName, msgId, msgContent));

            Object ntfContent = Kson.fromJson(msgContent, magicBook.getRspClazz(rspName));

            for (IListener sub : subs) {
                sub.onNtf(rspName, ntfContent);
            }
        }

        return false;  // 始终返回false，通知可共享。
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