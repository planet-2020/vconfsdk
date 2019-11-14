package com.kedacom.vconf.sdk.common.type.vconf;

import com.kedacom.vconf.sdk.common.constant.EmMtVmpStyle;
import com.kedacom.vconf.sdk.common.constant.EmVmpMode;

import java.util.List;

/**
 * 画面合成信息
 */

public class TMTConfVMPInfomation {

    public boolean bEnable;
    public EmVmpMode emMode;
    public EmMtVmpStyle emStyle;
    public boolean bVoiceHint;
    public boolean bShowMTName;
    public boolean bIsBroadcast;
    public int dwMemberCount;
    public List<TMTTemplateVmpMember> atMemberList;


}
