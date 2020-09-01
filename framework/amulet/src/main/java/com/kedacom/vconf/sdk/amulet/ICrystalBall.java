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
     * 收到消息。
     * */
    void onAppear(String msgId, String msgContent);

    /**
     * 消息监听器
     * */
    interface IListener {
        /**
         * @return 返回true若消息被消费，否则返回false。
         * */
        boolean onMsg(String msgName, String msgContent);
    }
    /**
     * 添加响应消息监听器
     * */
    void addRspListener(IListener listener);
    /**
     * 添加通知消息监听器
     * */
    void addNtfListener(IListener listener);
    /**
     * 删除监听器
     * */
    void delListener(IListener listener);
    /**
     * 清除所有监听器
     * */
    void clearListeners();

}
