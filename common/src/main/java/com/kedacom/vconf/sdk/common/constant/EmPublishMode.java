package com.kedacom.vconf.sdk.common.constant;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Enum2IntJsonAdapter;

/**
 * 发布模式
 */
@JsonAdapter(Enum2IntJsonAdapter.class)
public enum EmPublishMode {
    emNotPublishMode,   // 不发布
    emPublishMode       // 发布
}
