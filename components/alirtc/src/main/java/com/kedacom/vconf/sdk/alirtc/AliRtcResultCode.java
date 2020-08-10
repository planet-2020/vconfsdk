package com.kedacom.vconf.sdk.alirtc;

import androidx.annotation.NonNull;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public final class AliRtcResultCode {
    public static final int Failed = -1; // 未知错误
    public static final int OK = 0; // 成功

    public static final int LoginSuccess = 10; // 登录成功
    public static final int LogoutSuccess = 11; // 注销成功
    public static final int HasLoggedOnOtherDeviceAndCannotBeKickedOff = 12;   // 已在其他设备上登录且不能被抢登

    public static final int IncorrectConfPassword = 20;  // 会议密码错误
    public static final int VerificationAttemptsReachLimit = 21;  // 密码验证次数达到上限
    public static final int ConfereeNumReachLimit = 22;  // 此会议中与会人数达到上限
    public static final int AllConfsConfereeNumReachLimit = 23;  // 整个企业所有会议的与会总人数达到上限（会议本身的上限很可能没达到）
    public static final int NotAllowed = 24;  // 不允许加入该会议
    public static final int Expired = 25;  // 该会议已过期（仅企业会议室才可能过期，个人专属会议室一直存在）

    // NOTE：企业会议室——会议号固定公开，成员由管理员配置，仅配置过的成员可加入该会议，有期限，到期解散。


    /**
     * 错误码映射关系：
     * 消息    :   原始错误码   :   本地错误码
     * */
    private static Table<Msg, Object, Integer> resultCodes = HashBasedTable.create();

    static {
        resultCodes.put(Msg.LoginStateChanged, 100, LoginSuccess);
        resultCodes.put(Msg.LoginStateChanged, 90, LogoutSuccess);
        resultCodes.put(Msg.LoginStateChanged, 161, HasLoggedOnOtherDeviceAndCannotBeKickedOff);
        resultCodes.put(Msg.JoinConfRsp, 30337, IncorrectConfPassword); // 密码为空时报这个
        resultCodes.put(Msg.JoinConfRsp, 30327, IncorrectConfPassword);
        resultCodes.put(Msg.JoinConfRsp, 30336, VerificationAttemptsReachLimit);
        resultCodes.put(Msg.JoinConfRsp, 30328, ConfereeNumReachLimit);
        resultCodes.put(Msg.JoinConfRsp, 30325, AllConfsConfereeNumReachLimit);
        resultCodes.put(Msg.JoinConfRsp, 30334, Expired);
        resultCodes.put(Msg.JoinConfRsp, 30333, NotAllowed);
        resultCodes.put(Msg.JoinConfRsp, 30335, NotAllowed);
    }

    static int trans(@NonNull Msg msg, Object rawResultCode){
        Object localResultCode = resultCodes.row(msg).get(rawResultCode);
        if (null == localResultCode) return Failed;
        return (int)localResultCode;
    }

}