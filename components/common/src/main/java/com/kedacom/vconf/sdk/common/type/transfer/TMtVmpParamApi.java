package com.kedacom.vconf.sdk.common.type.transfer;

import java.util.ArrayList;
import java.util.List;

public class TMtVmpParamApi {

	public EmMtVmpMode emVmpMode;// /< vmp方式， 自动还是其他方式

	public boolean bIsBroadcast;// /< 是否广播

	public boolean bAddMmbAlias;// /< 合成图像是否叠加合成成员别名

	public EmMtVmpStyle emStyle;// /< 画面合成风格

	public List<TMtVmpItem> atVmpItem = new ArrayList<TMtVmpItem>(); // /画面合成成员

	public int dwCount;// /上面数组个数
}