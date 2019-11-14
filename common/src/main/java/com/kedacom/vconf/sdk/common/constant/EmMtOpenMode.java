package com.kedacom.vconf.sdk.common.constant;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Enum2IntJsonAdapter;

/**
 * 会议可见性
 */
@JsonAdapter(Enum2IntJsonAdapter.class)
public enum EmMtOpenMode {
	none,
	emMt_Open, // 完全开放
	emMt_Hide;// /< 隐藏会议，仅受邀列表里的终端可见
}