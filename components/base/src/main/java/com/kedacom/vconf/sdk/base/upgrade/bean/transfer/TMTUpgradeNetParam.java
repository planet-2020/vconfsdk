package com.kedacom.vconf.sdk.base.upgrade.bean.transfer;

public class TMTUpgradeNetParam {
	public long dwServerIp; // sus服务器IP (网络序)
	public boolean bUsedProxy; // 是否启用代理服务器
	public TMTOspSock5Proxy tProxyInfo; // 启用代理的话 代理服务器信息
	public int dwPort;
	public String achDomain; // 域名
	public String achIPv6; // IPv6地址

	public TMTUpgradeNetParam() {
	}

	public TMTUpgradeNetParam(long dwServerIp) {
		this.dwServerIp = dwServerIp;
	}
}
