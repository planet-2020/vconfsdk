package com.kedacom.vconf.sdk.common.type.vconf;

import com.kedacom.vconf.sdk.common.constant.EmMtMixType;

import java.util.List;

/**
 * 混音信息
 */
public class TMTConfMixInfo {

    public EmMtMixType emMode;                                   ///< 0-不混音，1-讨论开始，2-定制混音
    public int dwMixMemberCount;                                     ///< 成员数量
    public List<TMTCreateConfMember> atMixMemberList; ///< 混音列表
}
