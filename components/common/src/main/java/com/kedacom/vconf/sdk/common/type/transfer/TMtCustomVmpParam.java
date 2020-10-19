package com.kedacom.vconf.sdk.common.type.transfer;

import java.util.ArrayList;
import java.util.List;

public class TMtCustomVmpParam {

	public EmMtVmpMode emVmpMode;// /< vmp方式， 自动还是其他方式:如果emVmpMode是emMt_VMP_MODE_AUTO_Api,emStyle 也填自动 emMt_VMP_STYLE_DYNAMIC;如果 emVmpMode 是emMt_VMP_MODE_CTRL_Api，emStyle 选择画面合成风格

	public EmMtVmpStyle emStyle;// /< 画面合成风格

	public List<TMtVmpItem> atVmpItem = new ArrayList<TMtVmpItem>(); // /画面合成成员

	public int dwCount;

	public boolean bForce;   ////是否强制
}