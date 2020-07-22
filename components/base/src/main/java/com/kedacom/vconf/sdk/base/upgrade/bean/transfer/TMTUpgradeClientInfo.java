package com.kedacom.vconf.sdk.base.upgrade.bean.transfer;

/**
 * 检查升级参数
 * */
public class TMTUpgradeClientInfo {
	public TMTUpgradeNetParam tServerInfo; // sus服务器信息
	public TMTUpgradeDeviceInfo tDevInfo; // 设备信息

	public TMTUpgradeClientInfo(TMTUpgradeNetParam tServerInfo, TMTUpgradeDeviceInfo tDevInfo) {
		this.tServerInfo = tServerInfo;
		this.tDevInfo = tDevInfo;
	}
}