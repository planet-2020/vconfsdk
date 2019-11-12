package com.kedacom.vconf.sdk.common.constant;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Enum2IntJsonAdapter;

/**
 * 会议协议类型
 */
@JsonAdapter(Enum2IntJsonAdapter.class)
public enum EmConfProtocol {
    emProtocolBegin,    // 起始值
    em323,              // H323
    emsip,              // SIP
    emsat,              // SAT
    emtip,              // TIP
    emrtc               // webrtc
}