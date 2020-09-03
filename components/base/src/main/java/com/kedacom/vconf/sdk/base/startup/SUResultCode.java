package com.kedacom.vconf.sdk.base.startup;

import androidx.annotation.NonNull;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public final class SUResultCode {
    public static final int Failed = -1; // 未知错误
    public static final int OK = 0; // 成功

    public static final int StartInProgress = 10; // 正在启动中
    public static final int StartedAlready = 11; // 已启动
    public static final int StartMtSdkFailed = 12;   // 启动业务组件的sdk失败

    /**
     * 错误码映射关系：
     * 消息    :   原始错误码   :   本地错误码
     * */
    private static Table<Msg, Object, Integer> resultCodes = HashBasedTable.create();

    static {
//        resultCodes.put(Msg.LoginStateChanged, 100, LoginSuccess);
    }

    static int trans(@NonNull Msg msg, Object rawResultCode){
        Object localResultCode = resultCodes.row(msg).get(rawResultCode);
        if (null == localResultCode) return Failed;
        return (int)localResultCode;
    }

}