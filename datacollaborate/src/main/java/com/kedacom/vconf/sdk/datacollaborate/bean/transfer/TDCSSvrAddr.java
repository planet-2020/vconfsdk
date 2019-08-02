package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;

public class TDCSSvrAddr {
    public String achDomain;        // 域名
    public long dwIp;               // ip
    public boolean bUseDefAddr;     // 是否使用默认地址
    public String achCustomDomain;  // 用户自定义域名
    public long dwCustomIp;         // 用户自定义ip
    public int dwPort;

    public TDCSSvrAddr(long dwIp, int dwPort) {
        this.dwIp = dwIp;
        this.dwPort = dwPort;
    }
}