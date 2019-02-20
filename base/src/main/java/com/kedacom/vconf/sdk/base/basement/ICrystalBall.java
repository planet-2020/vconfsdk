/**
 * Created by gaofan_kd7331, 2018-10-25
 */


package com.kedacom.vconf.sdk.base.basement;

public interface ICrystalBall {

    /**
     * 下发请求。
     * */
    int spell(String methodOwner, String methodName, Object[] para, Class[] paraType);

    /**
     * 上报消息。
     * */
    void onAppear(String msgId, String msgContent);

    /**
     * 主动触发消息。
     * FORDEBUG 仅用于模拟模式
     * */
    default void emit(String msgId){}
    /**
     * 主动触发消息。
     * FORDEBUG 仅用于模拟模式
     * */
    default void emit(String[] msgIds){}

    /**
     * 消息监听器
     * */
    interface IListener {
        /**
         * @return 返回true若消息被消费，否则返回false。
         * */
        boolean onMsg(String msgId, String msgContent);
    }
    void addRspListener(IListener listener);
    void addNtfListener(IListener listener);
    void delListener(IListener listener);
    void clearListeners();

}
