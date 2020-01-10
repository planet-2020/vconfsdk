package com.kedacom.vconf.sdk.webrtc;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * Rtc相关结果码
 * Created by Sissi on 2020/1/6
 */
public final class RtcResultCode {
    public static final int OK = 0; // 成功
    public static final int IncorrectConfPassword = 1; // 会议密码错误
    public static final int NetworkUnreachable = 2; // 网络不可达
    public static final int LoginedAlready = 3; // 已注册（重复注册）
    public static final int ConfOK = 1000; // 创会、查询会议等操作成功

    private static BiMap<Integer, Integer> errorCodeMap = HashBiMap.create();
    static {
        errorCodeMap.put(100, OK);
        errorCodeMap.put(73, NetworkUnreachable);
        errorCodeMap.put(77, LoginedAlready);
        errorCodeMap.put(1000, ConfOK);
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
