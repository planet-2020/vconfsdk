package com.kedacom.vconf.sdk.alirtc;


import androidx.annotation.NonNull;

import com.kedacom.vconf.sdk.alirtc.bean.ConfInvitationInfo;
import com.kedacom.vconf.sdk.alirtc.bean.transfer.TConfInvitation;

final class ToDoConverter {
    static ConfInvitationInfo TConfInvitation2ConfInvitationInfo(@NonNull TConfInvitation confInvitation){
        return new ConfInvitationInfo(confInvitation.achConfE164, confInvitation.achConfName);
    }
}
