package com.kedacom.vconf.sdk.common.bean;

/**
* 终端类型
* */
public enum TerminalType {
    Sky("SKY for Android Phone"),
    TT("TrueTouch for android"),
    Movision("Movision Meetings for Android"),
    Unknown("unknown");

    String val;

    TerminalType(String val){
        this.val = val;
    }

    public String getVal() {
        return val;
    }
}