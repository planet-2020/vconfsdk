package com.kedacom.vconf.sdk.utils.thread;

import android.os.Handler;
import android.os.Message;

/**
 * Created by Sissi on 2020/1/3
 */
public final class HandlerHelper {

    public static void sendMessageDelayed(Handler handler, int what, Object obj, int delay){
        Message msg = Message.obtain();
        msg.what = what;
        msg.obj = obj;
        handler.sendMessageDelayed(msg, delay);
    }

    public static void sendMessage(Handler handler, int what, Object obj){
        Message msg = Message.obtain();
        msg.what = what;
        msg.obj = obj;
        handler.sendMessage(msg);
    }

}
