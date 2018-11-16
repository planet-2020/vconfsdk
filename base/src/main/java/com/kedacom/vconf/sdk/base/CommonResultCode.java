/**
 * 通用的请求结果码。
 * 范围[0, 100]
 * */


package com.kedacom.vconf.sdk.base;


public class CommonResultCode {
    public static final int SUCCESS = 0; // 成功
    public static final int FAILED = 1; // 失败
    public static final int TIMEOUT = 2; // 超时
    public static final int NETWORK_UNAVAILABLE = 3; // 网络不可用 TODO 框架层发送请求前检查网络？
}
