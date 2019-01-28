
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


    public static void setCrystalBall(ICrystalBall crystalBall){
        MagicStick.instance().setCrystalBall(crystalBall);
    }


    public boolean req(String reqId, int reqSn, Object... reqPara){
        return requestFairy.processRequest(feedbackHandler, reqId, reqSn, reqPara);
    }

    public void cancelReq(String reqId, int reqSn){
        requestFairy.processCancelRequest(feedbackHandler, reqId, reqSn);
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
     * （驱使下层）发射通知。仅用于模拟模式。
     * */
    public void eject(String[] ntfIds){
        emitNotificationFairy.processEmitNotifications(ntfIds);
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

        FeedbackBundle bundle = (FeedbackBundle) msg.obj;
        int type = bundle.type;

        if (FeedbackBundle.NTF == type){

            onFeedbackListener.onFeedbackNtf(bundle.msgId, bundle.body);

        }else if (FeedbackBundle.RSP == type){

            onFeedbackListener.onFeedbackRsp(bundle.msgId, bundle.body, bundle.reqId, bundle.reqSn, bundle.reqParas);

        }else if (FeedbackBundle.RSP_FIN == type){

            onFeedbackListener.onFeedbackRspFin(bundle.msgId, bundle.body, bundle.reqId, bundle.reqSn, bundle.reqParas);

        }else if (FeedbackBundle.RSP_TIMEOUT == type){

            onFeedbackListener.onFeedbackTimeout(bundle.reqId, bundle.reqSn, bundle.reqParas);

        }else if (FeedbackBundle.RSP_USER_CANCELED == type){

            onFeedbackListener.onFeedbackUserCanceled(bundle.reqId, bundle.reqSn, bundle.reqParas);

        }else if (FeedbackBundle.RSP_USER_CANCEL_FAILED == type){

            onFeedbackListener.onFeedbackUserCancelFailed(bundle.reqId, bundle.reqSn);

        }

    }


    public void setOnFeedbackListener(IOnFeedbackListener onFeedbackListener){
        this.onFeedbackListener = onFeedbackListener;
    }

    public interface IOnFeedbackListener{
        void onFeedbackNtf(String ntfId, Object ntfContent);
        void onFeedbackRsp(String rspId, Object rspContent, String reqId, int reqSn, Object[] reqParas);
        void onFeedbackRspFin(String rspId, Object rspContent, String reqId, int reqSn, Object[] reqParas);
        void onFeedbackTimeout(String reqId, int reqSn, Object[] reqParas);
        void onFeedbackUserCanceled(String reqId, int reqSn, Object[] reqParas);
        void onFeedbackUserCancelFailed(String reqId, int reqSn);
    }
}
