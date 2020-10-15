package com.kedacom.vconf.sdk.common.type;

import com.kedacom.vconf.sdk.common.type.transfer.EmMtCallDisReason;

public class Converter {
    public static EmMtCallDisReason HangupConfReason2EmMtCallDisReason(HangupConfReason reason){
        switch (reason){
            case Normal:
                return EmMtCallDisReason.emDisconnect_Normal;
            case NoPassword:
                return EmMtCallDisReason.emDisconnect_CancleInputPwd;
            case NotMyContact:
                return EmMtCallDisReason.emDisconnect_NotInPeerContact;
            case AlreadyInConf:
                return EmMtCallDisReason.emDisconnect_PeerInConf;
            default:
                return EmMtCallDisReason.emDisconnect_Normal;
        }
    }
}
