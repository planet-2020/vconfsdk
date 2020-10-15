package com.kedacom.vconf.sdk.common.type;

/**
 * 挂会原因
 * */
public enum HangupConfReason {
    Normal, // 正常挂会
    NoPassword, // 会议需要密码，用户没有会议密码。因为目前流程是先入会成功，再输入密码，所以如果没有密码还需执行一下挂会。
    NotMyContact, // 非联系人
    AlreadyInConf, // 已在会议中，拒绝会议邀请
}
