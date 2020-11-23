package com.kedacom.vconf.sdk.webrtc.bean;

/**
 * 与会成员信息
 * Created by Sissi on 2019/12/5
 */
public final class ConfMemberInfo {
    public String e164;
    public String moid;
    public String alias;
    public String email;


    public ConfMemberInfo(String e164, String moid) {
        this(e164, moid, null, null);
    }

    public ConfMemberInfo(String e164, String moid, String alias, String email) {
        this.e164 = e164;
        this.moid = moid;
        this.alias = alias;
        this.email = email;
    }
}
