package com.sissi.vconfsdk.base.amulet;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;


public class Caster {

    private static final String TAG = Caster.class.getSimpleName();

    private Handler feedbackHandler;
    private IOnFeedbackListener onFeedbackListener;

    private static IRequestProcessor requestProcessor;
    private static ICommandProcessor commandProcessor;
    private static ISubscribeProcessor subscribeProcessor;
    private static INotificationEmitter notificationEmitter;

    static {

        requestProcessor = SessionManager.instance();
        commandProcessor = CommandManager.instance();
        subscribeProcessor = NotifiManager.instance();
        notificationEmitter = NotifiManager.instance();

        NativeInteractor.instance()
                .setResponseProcessor(SessionManager.instance())
                .setNotificationProcessor(NotifiManager.instance());

        if (NativeEmulatorOnOff.on) {
            NativeInteractor.instance().setNativeEmulator(NativeEmulator.instance());
        }

    }

    public Caster(){
        feedbackHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                processFeedback(msg);
            }
        };
    }


    public boolean req(String reqId, int reqSn, Object reqPara){
        return requestProcessor.processRequest(feedbackHandler, reqId, reqPara, reqSn);
    }

    /**
     * 订阅通知
     * */
    public boolean subscribe(String ntfId){
        return subscribeProcessor.subscribe(feedbackHandler, ntfId);
    }

    /**
     * 取消订阅通知
     * */
    public void unsubscribe(String ntfId){
        subscribeProcessor.unsubscribe(feedbackHandler, ntfId);
    }


    /**
     * 批量订阅通知
     * */
    public void subscribe(String[] ntfIds){
        for (String ntfId : ntfIds){
            subscribe(ntfId);
        }
    }

    /**
     * 批量取消订阅通知
     * */
    public void unsubscribe(String[] ntfIds){
        for (String ntfId : ntfIds) {
            unsubscribe(ntfId);
        }
    }

    /**
     * （驱使下层）发射通知。仅用于模拟模式。
     * */
    public void eject(String ntfId){
        notificationEmitter.emitNotification(ntfId);
    }

    /**
     * 设置配置
     * */
    public void set(String setId, Object para){
        commandProcessor.set(setId, para);
    }

    /**
     * 获取配置
     * */
    public Object get(String getId){
        return commandProcessor.get(getId);
    }

    public Object get(String getId, Object para){
        return commandProcessor.get(getId, para);
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
