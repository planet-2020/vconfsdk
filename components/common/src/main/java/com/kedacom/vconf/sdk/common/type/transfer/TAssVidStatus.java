package com.kedacom.vconf.sdk.common.type.transfer;

import com.kedacom.vconf.sdk.common.type.transfer.EmMtChanState;
import com.kedacom.vconf.sdk.common.type.transfer.EmMtVidLabel;

public class TAssVidStatus{

	public EmMtChanState emChanState; // TRUE, 当前终端有辅流， FLASE, 当前没有辅流
	public EmMtVidLabel emVidLab; // 辅流的vid标签
	public boolean bActive; // 当通道为接收通道，才会用到,标识发送方是否激活通道
}