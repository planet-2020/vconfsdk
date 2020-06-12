package com.kedacom.vconf.sdk.base.startup.bean.transfer;

import com.kedacom.vconf.sdk.utils.json.EnumCustomValueStrategy;

@EnumCustomValueStrategy
public enum EmMtcLoginFailReason {
    emPwdErr_Api(1),	//密码错误
    emTerStarting_Api(2),	//启动中
    emTerUpgrading_Api(3),   //升级中
    emFailReasonEnd_Api(10);

    private int value;

    EmMtcLoginFailReason( int value ) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
