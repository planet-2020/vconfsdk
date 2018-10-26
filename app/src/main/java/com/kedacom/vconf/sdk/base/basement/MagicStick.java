
/**
 * Created by gaofan_kd7331, 2018-10-25
 */


package com.kedacom.vconf.sdk.base.basement;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.util.Log;


@SuppressWarnings({"JniMissingFunction", /*"unused", */"UnusedReturnValue"})
final class MagicStick implements ICrystalBall.IYellback {

    private static final String TAG = MagicStick.class.getSimpleName();

    private static MagicStick instance;

    private Handler handler;

    private IResponseProcessor responseProcessor;
    private INotificationProcessor notificationProcessor;
    private ICrystalBall crystalBall;

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
        if (null == crystalBall){
            return -1;
        }
        return crystalBall.yell(methodName, reqPara);
    }

    int set(String methodName, String setPara){
        if (null == crystalBall){
            return -1;
        }
        return crystalBall.yell(methodName, setPara);
    }

    int get(String methodName, String para, StringBuffer output){
        if (null == crystalBall){
            return -1;
        }
        return crystalBall.yell(methodName, para, output);
    }

    int get(String methodName, StringBuffer output){
        if (null == crystalBall){
            return -1;
        }
        return crystalBall.yell(methodName, output);
    }

    boolean emitNotification(String ntfId){
        if (null == crystalBall){
            return false;
        }
        return crystalBall.ejectNotification(ntfId);
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

    MagicStick setCrystalBall(ICrystalBall crystalBall){
        this.crystalBall = crystalBall;
        if (null != crystalBall) {
            crystalBall.setYellback(this);
        }
        return this;
    }

    private class MsgWrapper {
        String msgId;
        String msgBody;
        MsgWrapper(String msgId, String msgBody){this.msgId=msgId; this.msgBody=msgBody;}
    }

}
