package com.kedacom.vconf.sdk.base.upgrade.bean.transfer;

public class TMTUpgradeVersionInfo {
	public int dwVer_id; // 版本唯一标识
	public String achOemMark; // 设备OEM标识
	public String achDevType; // 设备类型
	public EmUpgradeVerLevel emUpgradeVerLevel; // 升级类型
	public String achCurSoftVer;// 设备目前软件版本
	public String achVerNotes; // 版本说明信息
	public int dwSize; // <版本大小
	public String achFileName; // 文件名
	public EmUpgradeReleaseAttr emUpgradeAttr; // 升级属性
	public TMTUpgradeGrayRange tGrayRange; // 灰度发布范围

}
