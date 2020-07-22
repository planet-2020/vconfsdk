package com.kedacom.vconf.sdk.common.constant;


import com.kedacom.vconf.sdk.utils.json.EnumOrdinalStrategy;

/**
 * 会议可见性
 */
@EnumOrdinalStrategy
public enum EmMtOpenMode {
	none,
	emMt_Open, // 完全开放
	emMt_Hide;// /< 隐藏会议，仅受邀列表里的终端可见
}