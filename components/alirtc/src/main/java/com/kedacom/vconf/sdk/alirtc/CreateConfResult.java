package com.kedacom.vconf.sdk.alirtc;

public class CreateConfResult {
    public String confName; // 会议名称
    public String confNum; // 会议号码

    public CreateConfResult(String confName, String confNum) {
        this.confName = confName;
        this.confNum = confNum;
    }
}
