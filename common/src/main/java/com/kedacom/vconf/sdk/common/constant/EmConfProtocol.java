package com.kedacom.vconf.sdk.common.constant;

import com.kedacom.vconf.sdk.utils.json.EnumOrdinalStrategy;

/**
 * 会议协议类型
 */
@EnumOrdinalStrategy
public enum EmConfProtocol {
    emProtocolBegin,    // 起始值
    em323,              // H323
    emsip,              // SIP
    emsat,              // SAT
    emtip,              // TIP
    emrtc,               // webrtc
    emaliyun,
}