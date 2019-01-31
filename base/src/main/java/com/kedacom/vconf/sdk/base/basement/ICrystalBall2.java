/**
 * Created by gaofan_kd7331, 2018-10-25
 */


package com.kedacom.vconf.sdk.base.basement;

public interface ICrystalBall2 {

    /**
     * 下发请求。
     * */
    int spell(String methodOwner, String methodName, Object[] para, Class[] paraType);

    /**
     * 上报消息。
     * */
    void onAppear(String msgName, String msgContent);


    /**
     * 消息监听器
     * */
    interface IListener {
        /**
         * @return 返回true若消息被消费，否则返回false。
         * */
        boolean onMsg(String msgName, String msgContent);
    }
    void addListener(IListener listener);
    void delListener(IListener listener);
    void clearListeners();

}
