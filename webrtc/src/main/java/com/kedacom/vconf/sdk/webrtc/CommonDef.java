package com.kedacom.vconf.sdk.webrtc;

/**
 * Created by Sissi on 2019/10/17
 */
class CommonDef {
    // PeerConnection类型，注意需得和业务消息中定义的一致。
    static final int CONN_TYPE_PUBLISHER = 0; // 主流发布
    static final int CONN_TYPE_SUBSCRIBER = 1; // 主流订阅
    static final int CONN_TYPE_ASS_PUBLISHER = 2; // 辅流发布
    static final int CONN_TYPE_ASS_SUBSCRIBER = 3; // 辅流订阅

    static final int MEDIA_TYPE_UNKNOWN = 0;
    static final int MEDIA_TYPE_VIDEO = 1;
    static final int MEDIA_TYPE_AUDIO = 2;
    static final int MEDIA_TYPE_AV = 3;
    static final int MEDIA_TYPE_ASS_VIDEO = 4;
}
