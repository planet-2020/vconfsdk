package com.kedacom.vconf.sdk.main.startup.bean.transfer;

public enum EmNetTransportType {
	None, // 不可用
	Wifi, // WIFI
	PPPoE, // PPPOE
	MobileData, // 移动数据3G/4G
	EthnetCard1, // 以太网Lan1
	EthnetCard2, // 以太网Lan2
	E1;
}