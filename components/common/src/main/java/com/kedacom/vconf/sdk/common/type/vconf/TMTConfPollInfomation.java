package com.kedacom.vconf.sdk.common.type.vconf;

import com.kedacom.vconf.sdk.common.constant.EmPollMode;

import java.util.List;

/**
 * 轮询信息
 * */
public class TMTConfPollInfomation {
    public boolean bEnable;
    public EmPollMode emMode;
    public int dwNum;
    public int dwInterval;
    public int dwMemberCount;
    public List<TMTCreateConfMember> atMemberList;
}
