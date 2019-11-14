package com.kedacom.vconf.sdk.common.constant;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Enum2IntJsonAdapter;

/**
  * 级联模式
  */
@JsonAdapter(Enum2IntJsonAdapter.class)
public enum EmRestCascadeMode {
	emRestCascade_Simple, // 简单级联
	emRestCascade_Merge, // 合并级联
}
