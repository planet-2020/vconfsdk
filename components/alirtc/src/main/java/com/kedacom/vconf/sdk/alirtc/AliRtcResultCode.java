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


    /**
     * 错误码映射关系：
     * 消息    :   原始错误码   :   本地错误码
     * */
    private static Table<Msg, Object, Integer> resultCodes = HashBasedTable.create();

    static {
//        resultCodes.put(Msg.Login, 100, OK);
//        resultCodes.put(Msg.Login, 72, UnknownServerAddress);
//        resultCodes.put(Msg.Login, 73, NetworkUnreachable);
//        resultCodes.put(Msg.Login, 77, MultipleRegistration);
//        resultCodes.put(Msg.Logout, 90, OK);
//        resultCodes.put(Msg.CreateConf, 1000, OK);
//        resultCodes.put(Msg.CreateConf, 99997, InstantConfDenied);
        resultCodes.put(Msg.JoinConfRsp, 30337, IncorrectConfPassword); // 密码为空时报这个
        resultCodes.put(Msg.JoinConfRsp, 30327, IncorrectConfPassword);
        resultCodes.put(Msg.LoginStateChanged, 100, LoginSuccess);
        resultCodes.put(Msg.LoginStateChanged, 90, LogoutSuccess);
        resultCodes.put(Msg.LoginStateChanged, 161, HasLoggedOnOtherDeviceAndCannotBeKickedOff); // 密码为空时报这个
//        resultCodes.put(Msg.JoinConf, 39, ReachConfereeNumLimit);
//        resultCodes.put(Msg.AcceptInvitation, 39, ReachConfereeNumLimit);
//        resultCodes.put(Msg.QueryConfInfo, 1000, OK);
//        resultCodes.put(Msg.LoginStateChanged, 100, OK);
    }

    static int trans(@NonNull Msg msg, Object rawResultCode){
        Object localResultCode = resultCodes.row(msg).get(rawResultCode);
        if (null == localResultCode) return Failed;
        return (int)localResultCode;
    }

}