package com.kedacom.vconf.sdk.base.upgrade.bean.transfer;

import com.kedacom.vconf.sdk.utils.json.EnumOrdinalStrategy;

@EnumOrdinalStrategy
public enum EmUpgradeReleaseAttr {
	emUpgradeAttr, emUpgradeAttrCommon, // 普通版本。 NOTE:该类型版本目前无法被检测到。
	emUpgradeAttrRecommend, // 推荐版本。所有用户都能检测到
	emUpgradeAttrGray, // 灰度版本。 只有特定的用户能检测到
}