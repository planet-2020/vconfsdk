package com.kedacom.vconf.sdk.common.type.transfer;

public enum EmRecordState {
    emDoNotRecord_Api,      // 未录像
    emRecording_Api,        // 正在录像
    emSuspendRecord_Api,    // 暂停
    emCallingMT_Api,        // 正在呼叫实体
    emPrepareRecord_Api     // 准备录像
}