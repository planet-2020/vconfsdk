package com.kedacom.vconf.sdk.common.constant;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Enum2IntJsonAdapter;

/**
 * FEC模式
 */
@JsonAdapter(Enum2IntJsonAdapter.class)
public enum EmMtFecMode {
    emMtFec_Close_Api,
    emMtFec_Open_Api;
}
