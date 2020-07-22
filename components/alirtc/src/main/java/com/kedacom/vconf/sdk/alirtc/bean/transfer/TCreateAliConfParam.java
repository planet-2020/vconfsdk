package com.kedacom.vconf.sdk.alirtc.bean.transfer;


public class TCreateAliConfParam {
    int       byConfType;   // 会议类型：2-即时会议；4-根据虚拟会议室创会
    String       achConfName; // 会议名称
    int      dwDuration;    // 会议持续时间，单位：分钟。 0 为永久
    boolean   bAutoEnd;     // 会议中无终端时，是否自动结束会议。true自动结会

    public TCreateAliConfParam(int byConfType, String achConfName, int dwDuration, boolean bAutoEnd) {
        this.byConfType = byConfType;
        this.achConfName = achConfName;
        this.dwDuration = dwDuration;
        this.bAutoEnd = bAutoEnd;
    }
}
