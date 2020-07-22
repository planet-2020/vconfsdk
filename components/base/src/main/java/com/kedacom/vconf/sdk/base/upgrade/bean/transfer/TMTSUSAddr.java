package com.kedacom.vconf.sdk.base.upgrade.bean.transfer;

public class TMTSUSAddr {
	public String achDomain; // 域名
	public long dwIP; // ip (使用网络序)
	public int dwPort;
	public boolean bUseDefaultAddr; // 使用默认地址
	public String achCustomDomain;
	public long dwCustomIp;
	public String achIPv6;
	public String achCustomIPv6;
}