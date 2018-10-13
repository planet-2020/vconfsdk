package com.sissi.vconfsdk.base;


public class ResultCode {

    public static final int SUCCESS = 0;

    private static final int COMMON_ERROR_BASE = 100;
    public static final int FAILED = COMMON_ERROR_BASE +1;
    public static final int TIMEOUT = COMMON_ERROR_BASE +2;
    public static final int NETWORK_UNAVAILABLE = COMMON_ERROR_BASE +3; // TODO 框架层发送请求前检查网络？
}
