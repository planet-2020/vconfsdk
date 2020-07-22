package com.kedacom.vconf.sdk.common.type.vconf;

import com.kedacom.vconf.sdk.common.constant.EmMtAddrType;
import com.kedacom.vconf.sdk.common.constant.EmMtVMPMmbType;

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
