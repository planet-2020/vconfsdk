package com.kedacom.vconf.sdk.webrtc;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * Rtc相关结果码
 * Created by Sissi on 2020/1/6
 */
public final class RtcResultCode {
    static final int OK = 0; // 成功
    public static final int Failed = -1; // 未知错误
    public static final int NetworkUnreachable = 1; // 网络不可达
    static final int LoginedAlready = 2; // 已注册（重复注册）
    public static final int UnknownServerAddress = 3; // 未知服务器

    static final int ConfOK = 100; // 创会、查询会议等操作成功
    public static final int IncorrectConfPassword = 101; // 会议密码错误
    public static final int ReachConfereeNumLimit = 102; // 与会方人数达上限（RTC最多8方）
    public static final int InstantConfDenied = 103; // 平台会议审批功能已开启，不能创建即时会议，只能预约创会。
    public static final int NotLoginedYet = 104; // 尚未成功登录

    private static BiMap<Integer, Integer> errorCodeMap = HashBiMap.create();
    static {
        errorCodeMap.put(100, OK);
        errorCodeMap.put(72, UnknownServerAddress);
        errorCodeMap.put(73, NetworkUnreachable);
        errorCodeMap.put(77, LoginedAlready);

        errorCodeMap.put(1000, ConfOK);
        errorCodeMap.put(39, ReachConfereeNumLimit);
        errorCodeMap.put(40, NotLoginedYet);
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
