package com.kedacom.vconf.sdk.webrtc.bean;


/**
 * 创会参数
 * Created by Sissi on 2019/11/22
 */
public final class ConfPara {
    public String creatorE164;      // 创建者e164
    public String confName;         // 会议名
    public int duration;            // 会议时长。单位：分钟
    public boolean bAudio;          // 是否音频会议
    public boolean bHighDefinition; // 是否高清视频，高清1080P，其余720P。注：若音频会议则此字段无效
    public boolean enableDC;        // 是否开启数据协作
    public String virtualConfId;    // 虚拟会议Id。非虚拟会议填null

    public ConfPara(String creatorE164, String confName, int duration, boolean bAudio, boolean bHighDefinition, boolean enableDC, String virtualConfId) {
        this.creatorE164 = creatorE164;
        this.confName = confName;
        this.duration = duration;
        this.bAudio = bAudio;
        this.bHighDefinition = bHighDefinition;
        this.enableDC = enableDC;
        this.virtualConfId = virtualConfId;
    }

}