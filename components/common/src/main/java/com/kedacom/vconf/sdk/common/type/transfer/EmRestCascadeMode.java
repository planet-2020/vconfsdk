package com.kedacom.vconf.sdk.common.type.transfer;


import com.kedacom.vconf.sdk.utils.json.EnumOrdinalStrategy;

/**
  * 级联模式
  */
@EnumOrdinalStrategy
public enum EmRestCascadeMode {
	emRestCascade_Simple, // 简单级联
	emRestCascade_Merge, // 合并级联
}
