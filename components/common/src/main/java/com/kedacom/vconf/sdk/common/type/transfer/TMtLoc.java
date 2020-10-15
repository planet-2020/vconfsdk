package com.kedacom.vconf.sdk.common.type.transfer;

import com.kedacom.vconf.sdk.common.type.transfer.EmBitStreamIndex;
import com.kedacom.vconf.sdk.common.type.transfer.EmMtMediaDirection;
import com.kedacom.vconf.sdk.common.type.transfer.EmMtMediaType;

public class TMtLoc{
	public EmMtMediaDirection emDirect;
	public EmMtMediaType emMediatype;
	public EmBitStreamIndex emStreamId; // 通道id 从0开始
	public String achStream_Alias; // 通道别名
}