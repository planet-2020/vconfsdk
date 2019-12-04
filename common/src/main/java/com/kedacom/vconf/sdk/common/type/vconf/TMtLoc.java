package com.kedacom.vconf.sdk.common.type.vconf;

import com.kedacom.vconf.sdk.common.constant.EmBitStreamIndex;
import com.kedacom.vconf.sdk.common.constant.EmMtMediaDirection;
import com.kedacom.vconf.sdk.common.constant.EmMtMediaType;

public class TMtLoc{
	public EmMtMediaDirection emDirect;
	public EmMtMediaType emMediatype;
	public EmBitStreamIndex emStreamId; // 通道id 从0开始
	public String achStream_Alias; // 通道别名
}