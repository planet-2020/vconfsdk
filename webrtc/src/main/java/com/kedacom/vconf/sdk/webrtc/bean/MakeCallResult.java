package com.kedacom.vconf.sdk.webrtc.bean;

/**
 * Created by Sissi on 2019/11/14
 */
public final class MakeCallResult {
    public String e164;
    public String alias;
    public String email;
    public int callBitRate;  // Unit: KB/s
    public boolean isP2p; // 是否点对点。true点对点；false 多点

    public MakeCallResult(String e164, String alias, String email, int callBitRate, boolean isP2p) {
        this.e164 = e164;
        this.alias = alias;
        this.email = email;
        this.callBitRate = callBitRate;
        this.isP2p = isP2p;
    }

}
