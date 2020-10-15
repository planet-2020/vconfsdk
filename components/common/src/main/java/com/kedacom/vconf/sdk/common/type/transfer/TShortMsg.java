package com.kedacom.vconf.sdk.common.type.transfer;

public class TShortMsg {
	public EmSMSType emType;
	public TMtId tSrcMtId;
	public short byDstNum;
	public TMtId[] arrMtDst;
	public EmRollMsgSpeed emSpeed; // 速度 (1-5)
	public short byRollTimes; // 滚动次数
	public String achText;
}
