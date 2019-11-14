package com.kedacom.vconf.sdk.common.constant;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Enum2IntJsonAdapter;

/**
 * 画面合成模式
 */
@JsonAdapter(Enum2IntJsonAdapter.class)
public enum EmVmpMode {
    emScreenVmp,
    emCustomScreenVmp,
    emAutoScreenVmp
}
