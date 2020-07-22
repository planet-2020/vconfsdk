package com.kedacom.vconf.sdk.base.upgrade.bean.transfer;

public class TMTUpgradeDeviceInfo {
	public String achDevType = ""; // 设备类型
	public String achOemMark = ""; // 设备OEM标识
	public String achE164 = ""; // 设备164标识
	public String achCurSoftVer = ""; // 设备目前软件版本
	public long dwDevIp; // 终端IP (网络序)
	public String achIPv6; // IPv6地址

	public TMTUpgradeDeviceInfo(String achDevType, String achE164, String achCurSoftVer, long dwDevIp, String oemMark) {
		this.achDevType = achDevType;
		this.achE164 = achE164;
		this.achCurSoftVer = achCurSoftVer;
		this.dwDevIp = dwDevIp;
		achOemMark = oemMark;
	}
}
