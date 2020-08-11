package com.kedacom.vconf.sdk.alirtc.bean;

public class ConfAboutToEnd {
    public String confNum;   //会议号
    public int leftTime;   // 剩余时长。单位：分钟
    public boolean isExperience;  // 是否为体验会议

    public ConfAboutToEnd(String confNum, int leftTime, boolean isExperience) {
        this.confNum = confNum;
        this.leftTime = leftTime;
        this.isExperience = isExperience;
    }
}
