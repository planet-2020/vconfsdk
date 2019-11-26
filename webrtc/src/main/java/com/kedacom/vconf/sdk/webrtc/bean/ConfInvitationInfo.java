package com.kedacom.vconf.sdk.webrtc.bean;

/**
 * 会议邀请信息
 * Created by Sissi on 2019/11/26
 */
public final class ConfInvitationInfo {
    public String e164;     // 邀请方e164
    public String alias;    // 邀请方别名
    public boolean bP2p;    // 是否为点对点。true 点对点，false 多点
    public int bitRate;     // 会议码率。单位: KB/s

    public ConfInvitationInfo(String e164, String alias, boolean bP2p, int bitRate) {
        this.e164 = e164;
        this.alias = alias;
        this.bP2p = bP2p;
        this.bitRate = bitRate;
    }
}
