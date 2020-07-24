package com.kedacom.vconf.sdk.base.login;

import androidx.annotation.NonNull;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * Rtc相关结果码
 * Created by Sissi on 2020/1/6
 */
public final class LIResultCode {
    public static final int Failed = -1; // 未知错误
    static final int Success = 0; // 成功

    public static final int IncorrectVerificationInfo = 100;  // 帐号或密码错误

    /**
     * 错误码映射关系：
     * 消息    :   原始错误码   :   面向用户的错误码
     * */
    private static Table<Msg, Object, Integer> resultCodes = HashBasedTable.create();

    static {
        resultCodes.put(Msg.LoginApsRsp, 0, Success);
        resultCodes.put(Msg.LoginApsRsp, 22007, IncorrectVerificationInfo);
    }

    public static int trans(@NonNull Msg msg, int rawResultCode){
        Object userResultCode = resultCodes.row(msg).get(rawResultCode);
        if (null == userResultCode) return Failed;
        return (int)userResultCode;
    }

}
