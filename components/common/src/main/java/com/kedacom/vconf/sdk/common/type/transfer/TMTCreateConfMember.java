package com.kedacom.vconf.sdk.common.type.transfer;

import com.kedacom.vconf.sdk.common.type.transfer.EmMtAddrType;

/**
  * 创会人员信息
  */

public class TMTCreateConfMember{

	public String achName;//平台过来 填的是一般都是 164号 和 别名
	public String achAccount;//平台过来填的一般都是moid
	public EmMtAddrType emAccountType; // 类型
}
