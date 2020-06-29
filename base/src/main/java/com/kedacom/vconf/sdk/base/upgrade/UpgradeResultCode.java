package com.kedacom.vconf.sdk.base.upgrade;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;


public final class UpgradeResultCode {
    static final int OK = 0; // 成功
    public static final int FAILED = -1; // 未知错误
    public static final int ALREADY_NEWEST = 1; // 当前版本已为最新版本
    public static final int NO_UPGRADE_PACKAGE = 2; // 服务器上没有升级包

    // native层错误码到用户结果码映射
    private static BiMap<Integer, Integer> errorCodeMap = HashBiMap.create();
    static {
        errorCodeMap.put(100, OK);
    }

    public static int fromTransfer(int transError){
        Integer domainError = errorCodeMap.get(transError);
        return null != domainError ? domainError : FAILED;
    }

    public static int toTransfer(int domainError){
        Integer transError = errorCodeMap.inverse().get(domainError);
        return null != transError ? transError : domainError;
    }

}
