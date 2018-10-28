package com.kedacom.vconf.sdk.base.basement;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


final class NotificationFairy implements /*ISubscribeProcessor, INotificationProcessor, INotificationEmitter*/ IFairy.ISubscribeFairy, IFairy.IEmitNotificationFairy, IFairy.INotificationFairy{

    private static final String TAG = NotificationFairy.class.getSimpleName();

    private static NotificationFairy instance;

//    private MagicStick magicStick;
    private IStick.IEmitNotificationStick stick;
    private MagicBook magicBook;
    private JsonProcessor jsonProcessor;

    private Map<String, Set<Handler>> subscribers;

    private NotificationFairy(){
//        magicStick = MagicStick.instance();
        magicBook = MagicBook.instance();
        jsonProcessor = JsonProcessor.instance();

        subscribers = new HashMap<>();
    }

    synchronized static NotificationFairy instance() {
        if (null == instance) {
            instance = new NotificationFairy();
        }

        return instance;
    }



    @Override
    public synchronized boolean subscribe(Handler subscriber, String ntfId) {
        if (null == subscriber){
            return false;
        }

        if (!magicBook.isNotification(ntfId)){
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

        if (!magicBook.isNotification(ntfId)){
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
    public synchronized boolean processNotification(String ntfName, String ntfBody) {
        if (!magicBook.isNotification(ntfName)){
            return false;
        }
        Set<Handler> subs = subscribers.get(ntfName);
        if (null == subs || 0==subs.size()){
            return false;
        }

        Log.d(TAG, String.format("<-~- %s\n%s", ntfName, ntfBody));

        Object ntfContent = jsonProcessor.fromJson(ntfBody, magicBook.getNtfClazz(ntfName));

        for (Handler sub : subs){
            Message msg = Message.obtain();
            msg.obj = new FeedbackBundle(ntfName, ntfContent, FeedbackBundle.NTF);
            sub.sendMessage(msg);
        }

        return true;
    }


    @Override
    public synchronized boolean emitNotification(String ntfName) {

        if (null == stick){
            Log.e(TAG, "no emit notification stick ");
            return false;
        }

        if (!magicBook.isNotification(ntfName)){
            Log.e(TAG, "Unknown notification "+ntfName);
            return false;
        }

        return stick.emitNotification(ntfName);
    }

    @Override
    public void setEmitNotificationStick(IStick.IEmitNotificationStick emitNotificationStick) {
        stick = emitNotificationStick;
    }

}