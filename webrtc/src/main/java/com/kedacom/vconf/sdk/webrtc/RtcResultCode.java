package com.kedacom.vconf.sdk.webrtc;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * Rtc相关结果码
 * Created by Sissi on 2020/1/6
 */
public final class RtcResultCode {
    static final int OK = 0; // 成功
    public static final int IncorrectConfPassword = 1; // 会议密码错误
    public static final int NetworkUnreachable = 2; // 网络不可达
    static final int LoginedAlready = 3; // 已注册（重复注册）
    static final int ConfOK = 100; // 创会、查询会议等操作成功
    public static final int ReachConfereeNumLimit = 101; // 与会方人数达上限（RTC最多8方）
    public static final int InstantConfDenied = 102; // 平台会议审批功能已开启，不能创建即时会议，只能预约创会。

    private static BiMap<Integer, Integer> errorCodeMap = HashBiMap.create();
    static {
        errorCodeMap.put(100, OK);
        errorCodeMap.put(73, NetworkUnreachable);
        errorCodeMap.put(77, LoginedAlready);
        errorCodeMap.put(1000, ConfOK);
        errorCodeMap.put(39, ReachConfereeNumLimit);
        errorCodeMap.put(99997, InstantConfDenied);
    }

    public static int fromTransfer(int transError){
        Integer domainError = errorCodeMap.get(transError);
        return null != domainError ? domainError : transError;
    }

    public static int toTransfer(int domainError){
        Integer transError = errorCodeMap.inverse().get(domainError);
        return null != transError ? transError : domainError;
    }

}
