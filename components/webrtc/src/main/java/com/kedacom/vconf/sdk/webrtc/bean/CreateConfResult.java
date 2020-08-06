package com.kedacom.vconf.sdk.webrtc.bean;

/**
 * Created by Sissi on 2019/11/26
 */
public final class CreateConfResult { // XXX 参数厘清
    public String confE164;
    public String e164;
    public String alias;
    public String email;
    public int callBitRate;  // Unit: KB/s

    public CreateConfResult(String confE164, String e164, String alias, String email, int callBitRate) {
        this.confE164 = confE164;
        this.e164 = e164;
        this.alias = alias;
        this.email = email;
        this.callBitRate = callBitRate;
    }

}