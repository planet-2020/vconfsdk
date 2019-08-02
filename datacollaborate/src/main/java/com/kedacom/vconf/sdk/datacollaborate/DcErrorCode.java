package com.kedacom.vconf.sdk.datacollaborate;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * Created by Sissi on 2019/8/2
 */
public final class DcErrorCode {
    // 失败
    public static final int Failed = -1;
    // 登录数据协作建链失败
    public static final int BuildLink4LoginFailed = -2;
    // 加入数据协作建链失败
    public static final int BuildLink4ConfFailed = -3;
    // 会议服务器断链
    public static final int LinkDisconnected = -4;
    // 协作方数量已达上限
    public static final int OperatorAmountReachLimit = -5;
    // 申请协作权被拒
    public static final int ApplyOperatorRejected = -6;
    // 没有权限执行该操作
    public static final int NoPermission = -7;
    // 数据协作个数达上限
    public static final int DcAmountReachLimit = -8;
    // 白板数量达上限
    public static final int BoardAmountReachLimit = -9;
    // 获取协作服务器地址失败
    public static final int GetServerAddrFailed = -10;

    private static BiMap<Integer, Integer> errorCodeMap = HashBiMap.create();
    static {
        errorCodeMap.put(25603, DcAmountReachLimit);
        errorCodeMap.put(25606, NoPermission);
        errorCodeMap.put(25607, OperatorAmountReachLimit);
        errorCodeMap.put(25701, BoardAmountReachLimit);
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
