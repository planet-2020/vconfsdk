
package com.kedacom.vconf.sdk.common.bean.transfer;

import com.kedacom.vconf.sdk.common.constant.EmMtMediaType;
import com.kedacom.vconf.sdk.common.constant.EmMtModel;

/**
  * 终端状态
  */

public class TTerStatus  {
	public EmMtModel emMtModel; // 终端型号
	public int byEncVol; // 当前编码音量(单位:等级)
	public int byDecVol; // 当前解码音量(单位:等级)
	public boolean bIsMute; // 是否哑音
	public boolean bIsQuiet; // 是否静音
	public boolean bMatrixStatus; // 外置矩阵状态 (TRUE ok FALSE err)
	public EmMtMediaType emViewedType; // /被选看类型
	public TMultiVideoInfo tVideoInfoList;
}
