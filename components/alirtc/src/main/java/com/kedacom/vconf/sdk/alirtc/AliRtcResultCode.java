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
    }

    static int trans(@NonNull Msg msg, Object rawResultCode){
        Object localResultCode = resultCodes.row(msg).get(rawResultCode);
        if (null == localResultCode) return Failed;
        return (int)localResultCode;
    }

}