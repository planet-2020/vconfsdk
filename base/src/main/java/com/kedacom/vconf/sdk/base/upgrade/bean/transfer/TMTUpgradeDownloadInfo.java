package com.kedacom.vconf.sdk.base.upgrade.bean.transfer;

public class TMTUpgradeDownloadInfo {
	public int dwErrcode; // 错误码
	public int dwTotalPercent; // 文件下载总进度
	public int dwCurPercent; // 当前文件的下载进度
	public String achCurFileName; // 当前正在下载的文件名
}
