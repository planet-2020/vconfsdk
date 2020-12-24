package com.kedacom.vconf.sdk.webrtc;

/**
 * Created by Sissi on 2019/10/17
 */
class CommonDef {
    // PeerConnection类型，注意需得和业务消息中定义的一致。
    enum ConnType{
        PUBLISHER,
        SUBSCRIBER,
        ASS_PUBLISHER,
        UNKNOWN;

        static ConnType getInstance(int ordinal){
            ConnType[] vals = ConnType.values();
            for (ConnType val : vals){
                if (val.ordinal()==ordinal){
                    return val;
                }
            }
            return UNKNOWN;
        }
    }
    // 媒体类型，注意需得和业务消息中定义的一致。
    enum MediaType{
        UNKNOWN,
        VIDEO,
        AUDIO,
        AV,
        ASS_VIDEO;

        static MediaType getInstance(int ordinal){
            MediaType[] vals = MediaType.values();
            for (MediaType val : vals){
                if (val.ordinal()==ordinal){
                    return val;
                }
            }
            return UNKNOWN;
        }
    }

}
