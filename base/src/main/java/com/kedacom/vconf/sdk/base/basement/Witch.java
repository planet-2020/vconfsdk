
/**
 * Created by gaofan_kd7331, 2018-10-25
 */


package com.kedacom.vconf.sdk.base.basement;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.RestrictTo;


@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressWarnings("UnusedReturnValue")
public final class Witch {

//    private static final String TAG = Witch.class.getSimpleName();

    private Handler feedbackHandler;
    private IOnFeedbackListener onFeedbackListener;

    private static IFairy.ICommandFairy commandFairy;
    private static IFairy.IRequestFairy requestFairy;
    private static IFairy.ISubscribeFairy subscribeFairy;
    private static IFairy.IEmitNotificationFairy emitNotificationFairy;

    static {

        CommandFairy cmdFairy = CommandFairy.instance();
        SessionFairy sessionFairy = SessionFairy.instance();
        NotificationFairy notificationFairy = NotificationFairy.instance();

        MagicStick magicStick = MagicStick.instance();
        magicStick.setCrystalBall(EmulationModeOnOff.on ? FakeCrystalBall.instance() : NativeInteractor.instance());
        magicStick.setResponseFairy(sessionFairy);
        magicStick.setNotificationFairy(notificationFairy);

        cmdFairy.setCommandStick(magicStick);
        sessionFairy.setRequestStick(magicStick);
        notificationFairy.setEmitNotificationStick(magicStick);

        commandFairy = cmdFairy;
        requestFairy = sessionFairy;
        subscribeFairy = notificationFairy;
        emitNotificationFairy = notificationFairy;

    }

    public Witch(){
        feedbackHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                processFeedback(msg);
            }
        };
    }


    public boolean req(String reqId, int reqSn, Object reqPara){
        return requestFairy.processRequest(feedbackHandler, reqId, reqPara, reqSn);
    }

    public boolean cancelReq(int reqSn){
        return requestFairy.processCancelRequest(feedbackHandler, reqSn);
    }


    /**
     * 订阅通知
     * */
    public boolean subscribe(String ntfId){
        return subscribeFairy.processSubscribe(feedbackHandler, ntfId);
    }

    /**
     * 取消订阅通知
     * */
    public void unsubscribe(String ntfId){
        subscribeFairy.processUnsubscribe(feedbackHandler, ntfId);
    }


    /**
     * （驱使下层）发射通知。仅用于模拟模式。
     * */
    public void eject(String ntfId){
        emitNotificationFairy.processEmitNotification(ntfId);
    }

    /**
     * 设置配置
     * */
    public void set(String setId, Object para){
        commandFairy.processSet(setId, para);
    }

    /**
     * 获取配置
     * */
    public Object get(String getId){
        return commandFairy.processGet(getId);
    }

    public Object get(String getId, Object para){
        return commandFairy.processGet(getId, para);
    }


    private void processFeedback(Message msg){

        if (null == onFeedbackListener){
            return;
        }

        FeedbackBundle feedbackBundle = (FeedbackBundle) msg.obj;
        int type = feedbackBundle.type;

        if (FeedbackBundle.RSP == type){

            onFeedbackListener.onFeedbackRsp(feedbackBundle.name, feedbackBundle.body, feedbackBundle.reqName, feedbackBundle.reqSn);

        }else if (FeedbackBundle.RSP_FIN == type){

            onFeedbackListener.onFeedbackRspFin(feedbackBundle.name, feedbackBundle.body, feedbackBundle.reqName, feedbackBundle.reqSn);

        }else if (FeedbackBundle.RSP_TIMEOUT == type){

            onFeedbackListener.onFeedbackTimeout(feedbackBundle.reqName, feedbackBundle.reqSn);

        }else if (FeedbackBundle.NTF == type){

            onFeedbackListener.onFeedbackNtf(feedbackBundle.name, feedbackBundle.body);

        }

    }


    public void setOnFeedbackListener(IOnFeedbackListener onFeedbackListener){
        this.onFeedbackListener = onFeedbackListener;
    }

    public interface IOnFeedbackListener{
        void onFeedbackRsp(String rspId, Object rspContent, String reqId, int reqSn);
        void onFeedbackRspFin(String rspId, Object rspContent, String reqId, int reqSn);
        void onFeedbackTimeout(String reqId, int reqSn);
        void onFeedbackNtf(String ntfId, Object ntfContent);
    }
}
