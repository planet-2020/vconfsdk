package com.kedacom.vconf.sdk.amulet;


public interface IFairy {

    String TAG = "Fairy";

    interface ISessionFairy extends IFairy, ICrystalBall.IListener{
        boolean req(IListener listener, String reqId, int reqSn, Object... reqParas);
        boolean cancelReq(int reqSn);

        /**
         * 会话监听器。
         * NOTE: 不要在该监听器中做耗时操作
         * */
        interface IListener{
            /**
             * 请求已发出
             * @param hasRsp 该请求是否有对应的响应消息。
             * @param reqId 请求ID。由{@link #req(IListener, String, int, Object...)}传入
             * @param reqSn 请求序号。由{@link #req(IListener, String, int, Object...)}传入
             * @param reqParas 请求参数。由{@link #req(IListener, String, int, Object...)}传入
             * @param output  传出参数。有些接口会通过传出参数反馈结果而非响应消息，比如获取本地配置。
             * */
            void onReqSent(boolean hasRsp, String reqId, int reqSn, Object[] reqParas, Object output);

            /**
             * 收到响应
             * @param bLast 是否为响应序列中的最后一条响应。
             *              一个请求可对应多个候选响应序列（但一次请求最终只匹配一个响应序列），一个响应序列可以包含多个响应。
             * @param rspId 响应ID
             * @param rspContent 响应内容
             * @param reqId 请求ID。由{@link #req(IListener, String, int, Object...)}传入
             * @param reqSn 请求序号。由{@link #req(IListener, String, int, Object...)}传入
             * @param reqParas 请求参数。由{@link #req(IListener, String, int, Object...)}传入
             * @return 返回true表示该条消息被消费，false表示未被消费。若消息未被消费则此条消息不计入会话中，继续等待同名消息。
             * 比如有下列“请求——响应序列”映射：
             * req——rsp1，rsp2
             * 发出req请求后收到了rsp1，通过onRsp回调给用户，若用户返回了true则认为已经匹配了正确的rsp1，则下一个期望的匹配响应是rsp2，
             * 若用户返回了false则认为此条rsp1不匹配用户期望，将继续匹配后续收到的rsp1直到用户返回true或者等待超时。
             * */
            boolean onRsp(boolean bLast, String rspId, Object rspContent, String reqId, int reqSn, Object[] reqParas);

            /**
             * 等待响应超时。
             * */
            void onTimeout(String reqId, int reqSn, Object[] reqParas);
        }

    }


    interface INotificationFairy extends IFairy, ICrystalBall.IListener{
        boolean subscribe(IListener subscriber, String ntfId);
        void unsubscribe(IListener subscriber, String ntfId);

        /**
         * 通知监听器。
         * NOTE: 不要在该监听器中做耗时操作
         * */
        interface IListener{
            /**
             * 收到通知
             * @param ntfId 通知ID
             * @param ntfContent 通知内容 */
            void onNtf(String ntfId, Object ntfContent);
        }

    }

    void setCrystalBall(ICrystalBall crystalBall);

    void setMagicBook(IMagicBook magicBook);

}
