package com.kedacom.vconf.sdk.webrtc.bean.trans;

/**
 * Created by Sissi on 2019/10/25
 */
public final class TMtRtcSvrAddr {
    public boolean bUsedRtc;                             ///< 是否使用Rtc
    public String  achDomain; ///< Rtc域名
    public int dwIp;                                    ///< RtcIp (网络序)
    public int dwPort;                                  ///< Rtc端口， 如果不填，标准是1720， 非标的是用平台返回的值
    public String achNumber;  ///< 注册时使用的号码
    public String achPassword;    ///< 密码
    public int dwTimeToLive;                            ///< Rtc保活超时时间 单位(s)
    public String  achAuthName; ///< 认证名称, 填写密码后会用到认证名称
    public String  achIpv6;            //Rtc ipv6

    public TMtRtcSvrAddr(int dwIp, int dwPort, String achNumber) {
        bUsedRtc = true;
        this.dwIp = dwIp;
        this.dwPort = dwPort;
        this.achNumber = achNumber;
    }
}
