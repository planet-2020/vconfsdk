package com.sissi.vconfsdk.frame;

import android.os.Handler;
import android.os.Message;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 通知处理器
 *
 * Created by Sissi on 1/9/2017.
 */
final class NotifyProcessor {
    private static NotifyProcessor instance;
    private HashMap<String, ArrayList<Handler>> subscribers;

    private NotifyProcessor(){
        subscribers = new HashMap<>();
    }

    synchronized static NotifyProcessor instance() {
        if (null == instance) {
            instance = new NotifyProcessor();
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

    /**
     * 上报通知。
     * @param ntfId 通知ID.
     * @param ntfContent 通知内容.
     * @return 返回真若上报成功，返回假若上报失败。
     * */
    synchronized boolean notify(String ntfId, Object ntfContent){
        ArrayList<Handler> subs = subscribers.get(ntfId);
        if (null == subs || 0==subs.size()){
            return false;
        }

        for (Handler sub : subs){
            Message msg = Message.obtain();
            msg.obj = new ResponseBundle(ntfId, ntfContent, ResponseBundle.NTF);
            sub.sendMessage(msg);
        }

        return true;
    }
}