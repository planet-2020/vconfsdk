package com.kedacom.vconf.sdk.common.type.transfer;

import com.kedacom.vconf.sdk.utils.json.EnumOrdinalStrategy;

/**
 * 画面合成方式
 */
@EnumOrdinalStrategy
public enum EmMtVmpMode {
	emMt_VMP_MODE_NONE, // 不进行画面合成
	emMt_VMP_MODE_CTRL, // 会控或主席选择成员合成
	emMt_VMP_MODE_AUTO, // 动态分屏与设置成员
}
