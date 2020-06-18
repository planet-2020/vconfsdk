package com.kedacom.vconf.sdk.base.login.bean.transfer;

public class MtXAPSvrCfg {
    public int emAddrType;   ///<地址类型
    public String  achDomain;             ///< 域名
    public String achAlias;
    public long     dwIp;                  ///< ip
    public boolean bIsIpSvr;                ///< 是否是IP类型的aps服务器
    public long dwPort;                   ///端口号 新增

    public MtXAPSvrCfg(int emAddrType, String achDomain, String achAlias, long dwIp, boolean bIsIpSvr, long dwPort) {
        this.emAddrType = emAddrType;
        this.achDomain = achDomain;
        this.achAlias = achAlias;
        this.dwIp = dwIp;
        this.bIsIpSvr = bIsIpSvr;
        this.dwPort = dwPort;
    }
}