
/**
 * Created by gaofan_kd7331, 2018-10-25
 */


package com.kedacom.vconf.sdk.base.amulet;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.util.Log;


@SuppressWarnings({"JniMissingFunction", /*"unused", */"UnusedReturnValue"})
final class MagicStick implements IEchoWall.IYellback {

    private static final String TAG = MagicStick.class.getSimpleName();

    private static MagicStick instance;

    private Handler handler;

    private IResponseProcessor responseProcessor;
    private INotificationProcessor notificationProcessor;
    private IEchoWall echoWall;

    private MagicStick(){
        initHandler();
    }

    public synchronized static MagicStick instance() {
        if (null == instance) {
            instance = new MagicStick();
        }
        return instance;
    }

    int request(String methodName, String reqPara){
        if (null == echoWall){
            return -1;
        }
        return echoWall.yell(methodName, reqPara);
    }

    int set(String methodName, String setPara){
        if (null == echoWall){
            return -1;
        }
        return echoWall.yell(methodName, setPara);
    }

    int get(String methodName, String para, StringBuffer output){
        if (null == echoWall){
            return -1;
        }
        return echoWall.yell(methodName, para, output);
    }

    int get(String methodName, StringBuffer output){
        if (null == echoWall){
            return -1;
        }
        return echoWall.yell(methodName, output);
    }

    boolean emitNotification(String ntfId){
        if (null == echoWall){
            return false;
        }
        return echoWall.ejectNotification(ntfId);
    }


    @Override
    public void yellback(String msgId, String msgBody){
        if (null == msgId || msgId.isEmpty()){
            Log.w(TAG, "empty msg id");
            return;
        }
        Message msg = Message.obtain();
        msg.obj = new MsgWrapper(msgId, msgBody);
        handler.sendMessage(msg);
    }


    private void initHandler(){
        HandlerThread handlerThread = new HandlerThread("MS.handler", Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper()){
            @Override
            public void handleMessage(Message msg) {
                MsgWrapper msgWrapper = (MsgWrapper) msg.obj;
                String msgId = msgWrapper.msgId;
                String msgBody = msgWrapper.msgBody;
                boolean consumed = false;
                if (null!=responseProcessor){
                    consumed = responseProcessor.processResponse(msgId, msgBody);
                }
                if (!consumed  && null!=notificationProcessor){
                    consumed = notificationProcessor.processNotification(msgId, msgBody);
                }
                if (!consumed){
                    Log.w(TAG, String.format("<-/- %s, unconsumed msg \n%s", msgId, msgBody));
                }
            }
        };
    }


    MagicStick setResponseProcessor(IResponseProcessor responseProcessor){
        this.responseProcessor = responseProcessor;
        return this;
    }

    MagicStick setNotificationProcessor(INotificationProcessor notificationProcessor){
        this.notificationProcessor = notificationProcessor;
        return this;
    }

    MagicStick setEchoWall(IEchoWall echoWall){
        this.echoWall = echoWall;
        if (null != echoWall) {
            echoWall.setYellback(this);
        }
        return this;
    }

    private class MsgWrapper {
        String msgId;
        String msgBody;
        MsgWrapper(String msgId, String msgBody){this.msgId=msgId; this.msgBody=msgBody;}
    }

}

