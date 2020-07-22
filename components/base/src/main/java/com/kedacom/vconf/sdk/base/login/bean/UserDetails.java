package com.kedacom.vconf.sdk.base.login.bean;

import java.util.Map;

/**
 * 用户详情
 * */
public class UserDetails {
    public String account; // 帐号/序列号
    public String moid; // 账号moid
    public String jid; // xmpp账号
    public String e164; // E164号
    public String email; // 企业邮箱(微博登录账号)

    public String name; // 姓名
    public boolean isMale; // 性别
    public String jobNumber; // 工号
    public String birthDate; // 出生日期
    public String brief;  // 简介
    public String phoneNumber; // 手机号码
    public String extensionNumber; // 分机号码
    public String seat;     // 座位
    public String officeLocation; // 办公地址
    public String portrait32; // 32位头像地址
    public String portrait40; // 40位头像地址
    public String portrait64; // 64位头像地址
    public String portrait128; // 128位头像地址
    public String portrait256; // 256位头像地址

    public Map<DepartmentInfo, String> positions;  // 职位（可能身处多个部门多个职位）

    public String aliroomId; // 阿里会议室ID。每个帐号都绑定有一个阿里会议室ID。

    @Override
    public String toString() {
        return "UserDetails{" +
                "alias='" + account + '\'' +
                ", moid='" + moid + '\'' +
                ", jid='" + jid + '\'' +
                ", e164='" + e164 + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", isMale=" + isMale +
                ", jobNumber='" + jobNumber + '\'' +
                ", birthDate='" + birthDate + '\'' +
                ", brief='" + brief + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", extensionNumber='" + extensionNumber + '\'' +
                ", seat='" + seat + '\'' +
                ", officeLocation='" + officeLocation + '\'' +
                ", portrait32='" + portrait32 + '\'' +
                ", portrait40='" + portrait40 + '\'' +
                ", portrait64='" + portrait64 + '\'' +
                ", portrait128='" + portrait128 + '\'' +
                ", portrait256='" + portrait256 + '\'' +
                ", positions=" + positions +
                '}';
    }
}
