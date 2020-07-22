package com.kedacom.vconf.sdk.common.type;

import com.kedacom.vconf.sdk.common.constant.EmIpAddrType;

public class TNetAddr {

	// IP地址类型V4/V6
	public EmIpAddrType emIpType;

	// IP地址(网络序)
	public long dwIp;

	// IPV6信息
	public String achIpV6;

	// 端口
	public int dwPort;
}