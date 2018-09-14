package com.sissi.vconfsdk.base.engine;

import android.os.Handler;
import android.os.Message;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 通知处理器
 *
 * Created by Sissi on 1/9/2017.
 */
final class NotifiManager implements INotificationProcessor{
    private static NotifiManager instance;
    private HashMap<String, ArrayList<Handler>> subscribers;

    private NotifiManager(){
        subscribers = new HashMap<>();
        NativeInteractor.instance().setNotificationProcessor(this);
    }

    synchronized static NotifiManager instance() {
        if (null == instance) {
            instance = new NotifiManager();
        }

        return instance;
    }

    /**
     * 订阅通知。
     * @param subscriber 订阅者.
     * @param ntfId 通知ID.
     * */
    synchronized void subscribeNtf(Handler subscriber, String ntfId){
        ArrayList<Handler> subs = subscribers.get(ntfId);
        if (null == subs){
            subs = new ArrayList<Handler>();
            subscribers.put(ntfId, subs);
        }

        if (!subs.contains(subscriber)) {
            subs.add(subscriber);
        }
    }

    /**
     * 取消订阅通知。
     * @param subscriber 订阅者.
     * @param ntfId 通知ID.
     * */
    synchronized void unsubscribeNtf(Handler subscriber, String ntfId){
        ArrayList<Handler> subs = subscribers.get(ntfId);
        if (null != subs){
            subs.remove(subscriber);
            if (subs.isEmpty()){
                subscribers.remove(ntfId);
            }
        }
    }


    @Override
    public synchronized boolean process(String ntfName, Object ntfContent) {
        ArrayList<Handler> subs = subscribers.get(ntfName);
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
}