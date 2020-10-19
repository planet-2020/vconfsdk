package com.kedacom.vconf.sdk.common.type.transfer;

public class TMtVmpItem {
	public int dwVmpItem_Idx;               ///< 在画面合成中的位置, 不要用外面的数组的下标
	public EmMtVMPMmbType emMmbType;  ///< 如果type是emVMPMmbMCSspec_Api, 平台会关心tMtid字段;如果type不是emVMPMmbMCSspec_Api， 平台不关心tMtid字段
	public TMtId tMtid;                            ///< 画面合成成员
	public EmCodecComponentIndex   emStream_id; ///< 通道id 从0开始, 用在多流中，没有多流可以忽略

	//rtc下使用
	public String achStreamId;  ///流标识
}
