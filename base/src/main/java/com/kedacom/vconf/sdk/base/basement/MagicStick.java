
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
final class MagicStick implements IStick,
        IStick.ICommandStick,
        IStick.IRequestStick,
        IStick.IResponseStick,
        IStick.IEmitNotificationStick,
        IStick.INotificationStick,
        ICrystalBall.IYellback {

    private static final String TAG = MagicStick.class.getSimpleName();

    private static MagicStick instance;

    private Handler handler;

    private IFairy.IResponseFairy responseFairy;
    private IFairy.INotificationFairy notificationFairy;
    private ICrystalBall crystalBall;

    private MagicBook magicBook = MagicBook.instance();

    private MagicStick(){
        initHandler();
    }

    public synchronized static MagicStick instance() {
        if (null == instance) {
            instance = new MagicStick();
        }
        return instance;
    }

    @Override
    public int request(String reqName, Object... reqPara){
        if (null == crystalBall){
            return -1;
        }

        return crystalBall.yell(magicBook.getReqMethodOwner(reqName), reqName, reqPara);
    }

    @Override
    public int set(String setName, String setPara){
//        if (null == crystalBall){
//            return -1;
//        }
//        return crystalBall.yell(setName, setPara);
        return 0;
    }

    @Override
    public int get(String getName, String para, StringBuffer output){
//        if (null == crystalBall){
//            return -1;
//        }
//        return crystalBall.yell(getName, para, output);
        return 0;
    }

    @Override
    public int get(String getName, StringBuffer output){
//        if (null == crystalBall){
//            return -1;
//        }
//        return crystalBall.yell(getName, output);
        return 0;
    }

    @Override
    public boolean emit(String ntfName){
        if (null == crystalBall){
            return false;
        }
        return crystalBall.eject(ntfName);
    }

    @Override
    public boolean emit(String[] ntfNames) {
        if (null == crystalBall){
            return false;
        }
        return crystalBall.eject(ntfNames);
    }


    @Override
    public void yellback(String msgName, String msgBody){
        if (null == msgName || msgName.isEmpty()){
            Log.w(TAG, "empty msg name");
            return;
        }
        Message msg = Message.obtain();
        msg.obj = new MsgWrapper(msgName, msgBody);
        handler.sendMessage(msg);
    }


    private void initHandler(){
        HandlerThread handlerThread = new HandlerThread("MS.handler", Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper()){
            @Override
            public void handleMessage(Message msg) {
                MsgWrapper msgWrapper = (MsgWrapper) msg.obj;
                String msgName = msgWrapper.msgName;
                String msgBody = msgWrapper.msgBody;
                boolean processed = false;
                if (null!=responseFairy){
                    processed = responseFairy.processResponse(msgName, msgBody);
                }
                if (!processed && null!=notificationFairy){
                    processed = notificationFairy.processNotification(msgName, msgBody);
                }
                if (!processed){
                    Log.w(TAG, String.format("<-/- %s, unprocessed msg \n%s", msgName, msgBody));
                }
            }
        };
    }



    @Override
    public void setResponseFairy(IFairy.IResponseFairy responseFairy) {
        this.responseFairy = responseFairy;
    }

    @Override
    public void setNotificationFairy(IFairy.INotificationFairy notificationFairy) {
        this.notificationFairy = notificationFairy;
    }

    @Override
    public void setCrystalBall(ICrystalBall crystalBall){
        this.crystalBall = crystalBall;
        if (null != crystalBall) {
            crystalBall.setYellback(this);
        }
    }


    private class MsgWrapper {
        String msgName;
        String msgBody;
        MsgWrapper(String msgName, String msgBody){this.msgName =msgName; this.msgBody=msgBody;}
    }

}

