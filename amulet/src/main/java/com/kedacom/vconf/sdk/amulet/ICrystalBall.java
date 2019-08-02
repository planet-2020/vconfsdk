package com.kedacom.vconf.sdk.amulet;

/**
 * Created by gaofan_kd7331, 2018-10-25
 */

public interface ICrystalBall {

    /**
     * 下发请求。
     * */
    int spell(String methodOwner, String methodName, Object[] para, Class[] paraType);

    /**
     * 上报消息。
     * */
    void onAppear(String msgId, String msgContent);

//    /**
//     * 主动触发消息。
//     * FORDEBUG 仅用于模拟模式
//     * */
//    default void emit(String msgId){}
//    /**
//     * 主动触发消息。
//     * FORDEBUG 仅用于模拟模式
//     * */
//    default void emit(String[] msgIds){}

    /**
     * 消息监听器
     * */
    interface IListener {
        /**
         * @return 返回true若消息被消费，否则返回false。
         * */
        boolean onMsg(String msgId, String msgContent);
    }
    /**
     * 添加消息监听器
     * @param priority 优先级（>=0），越小越高。高优先级的监听器优先消费消息，{@link IListener#onMsg(String, String)}
     * */
    void addListener(IListener listener, int priority);
    /**
     * 删除监听器
     * */
    void delListener(IListener listener);
    /**
     * 清除所有监听器
     * */
    void clearListeners();

    /**
     * 获取监听器的优先级
     * @return 优先级。-1表示该监听器尚未注册。
     * */
    int getPriority(IListener listener);
}
