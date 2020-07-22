package com.kedacom.vconf.sdk.webrtc.bean;

import javax.annotation.Nullable;

/**
 * 与会成员信息
 * Created by Sissi on 2019/12/5
 */
public final class ConfMemberInfo {
    public String e164;
    public String moid;
    public String alias;
    public String email;

    /**
     * 与会成员信息，至少一项不为空，可通过该项呼叫到该成员。
     * */
    public ConfMemberInfo(@Nullable String e164, @Nullable String moid, @Nullable String alias, @Nullable String email) {
        this.e164 = e164;
        this.moid = moid;
        this.alias = alias;
        this.email = email;
    }
}
