package com.kedacom.vconf.sdk.main.startup.bean.transfer;

public class TNetWorkInfo {
	public EmNetTransportType emUsedType;
	public int dwIp; // IP地址
	public int dwMask; // 子网掩码
	public int dwGateway; // 网关
	public int dwDns; // DNS
	public int dwDnsBackup; // DNS备份

	public TNetWorkInfo(EmNetTransportType emUsedType, int dwIp, int dwMask, int dwGateway, int dwDns) {
		this.emUsedType = emUsedType;
		this.dwIp = dwIp;
		this.dwMask = dwMask;
		this.dwGateway = dwGateway;
		this.dwDns = dwDns;
	}

}