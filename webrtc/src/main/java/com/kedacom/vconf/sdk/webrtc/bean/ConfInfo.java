package com.kedacom.vconf.sdk.webrtc.bean;

/**
 * 会议详情
 * Created by Sissi on 2020/1/2
 */
public final class ConfInfo {
    public String title; // 会议名称
    public String e164;
    public String startTime; // 会议开始时间
    public String endTime;   // 会议结束时间

    public ConfInfo(String title, String e164, String startTime, String endTime) {
        this.title = title;
        this.e164 = e164;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
