package com.kedacom.vconf.sdk.base.upgrade.bean.transfer;

import com.kedacom.vconf.sdk.utils.json.EnumOrdinalStrategy;

@EnumOrdinalStrategy
public enum EmUpgradeVerLevel {
	emUpgradeLevelForced, // /< 强制
	emUpgradeLevelSuggested, // /< 建议
	emUpgradeLevelNormal, // /< 普通
}