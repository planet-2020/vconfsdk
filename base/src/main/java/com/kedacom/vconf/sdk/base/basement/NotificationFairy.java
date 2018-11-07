package com.kedacom.vconf.sdk.base.basement;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


final class NotificationFairy implements IFairy.ISubscribeFairy,
        IFairy.IEmitNotificationFairy,
        IFairy.INotificationFairy{

    private static final String TAG = NotificationFairy.class.getSimpleName();

    private static NotificationFairy instance;

    private IStick.IEmitNotificationStick stick;
    private MagicBook magicBook;
    private JsonProcessor jsonProcessor;

    private Map<String, Set<Handler>> subscribers;

    private NotificationFairy(){
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
    public synchronized boolean processSubscribe(Handler subscriber, String ntfId) {
        if (null == subscriber){
            return false;
        }

        String ntfName = magicBook.getMsgName(ntfId);

        if (!magicBook.isNotification(ntfName)){
            Log.e(TAG, "Unknown notification "+ntfName);
            return false;
        }

        Set<Handler> subs = subscribers.get(ntfName);
        if (null == subs){
            subs = new HashSet<>();
            subscribers.put(ntfName, subs);
        }

        subs.add(subscriber);

        return true;
    }

    @Override
    public synchronized void processUnsubscribe(Handler subscriber, String ntfId) {

        if (null == subscriber){
            return;
        }

        String ntfName = magicBook.getMsgName(ntfId);

        if (!magicBook.isNotification(ntfName)){
            Log.e(TAG, "Unknown notification "+ntfName);
            return;
        }

        Set<Handler> subs = subscribers.get(ntfName);
        if (null != subs){
            subs.remove(subscriber);
            if (subs.isEmpty()){
                subscribers.remove(ntfName);
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
            msg.obj = new FeedbackBundle(magicBook.getMsgId(ntfName),
                    ntfContent,
                    FeedbackBundle.NTF);
            sub.sendMessage(msg);
        }

        return true;
    }


    @Override
    public synchronized boolean processEmitNotification(String ntfId) {

        if (null == stick){
            Log.e(TAG, "no emit notification stick ");
            return false;
        }

        String ntfName = magicBook.getMsgName(ntfId);

        if (!magicBook.isNotification(ntfName)){
            Log.e(TAG, "Unknown notification "+ ntfName);
            return false;
        }

        return stick.emit(ntfName);
    }

    @Override
    public boolean processEmitNotifications(String[] ntfIds) {
        if (null == stick){
            Log.e(TAG, "no emit notification stick ");
            return false;
        }

        String[] ntfNames = new String[ntfIds.length];

        for (int i=0; i<ntfNames.length; ++i) {
            ntfNames[i] = magicBook.getMsgName(ntfIds[i]);
            if (!magicBook.isNotification(ntfNames[i])) {
                Log.e(TAG, "Unknown notification " + ntfNames[i]);
                return false;
            }
        }

        return stick.emit(ntfNames);
    }

    @Override
    public void setEmitNotificationStick(IStick.IEmitNotificationStick emitNotificationStick) {
        stick = emitNotificationStick;
    }

}