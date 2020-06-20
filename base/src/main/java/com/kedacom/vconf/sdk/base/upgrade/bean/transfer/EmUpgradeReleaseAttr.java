package com.kedacom.vconf.sdk.base.upgrade.bean.transfer;

import com.kedacom.vconf.sdk.utils.json.EnumOrdinalStrategy;

@EnumOrdinalStrategy
public enum EmUpgradeReleaseAttr {
	emUpgradeAttr, emUpgradeAttrCommon, // 普通版本
	emUpgradeAttrRecommend, // 推荐版本
	emUpgradeAttrGray, // 灰度版本
}