package com.kedacom.vconf.sdk.common.type.transfer;

import com.kedacom.vconf.sdk.common.type.transfer.EmMtAddrType;
import com.kedacom.vconf.sdk.common.type.transfer.EmMtVMPMmbType;

/**
  * 画面合成成员列表
  */

public class TMTTemplateVmpMember {

	public String achName; // 账号名称
	public String achAccount; // 账号
	public EmMtAddrType emAccountType; // 帐号类型
	public EmMtVMPMmbType emFollowType; // 跟随类型
	public int dwIndex; // 在画画合成中的位置
}
