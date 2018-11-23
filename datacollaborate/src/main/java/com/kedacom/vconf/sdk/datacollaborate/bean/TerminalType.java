package com.kedacom.vconf.sdk.datacollaborate.bean;

import com.kedacom.vconf.sdk.base.MsgConst.EmDcsType;

public enum TerminalType {
    TrueLinkWindows,    // 致邻Windows版
    TrueLinkIosPhone,   // 致邻IOS手机版
    TrueLinkIosPad,     // 致邻IOS平板版
    TrueLinkAndroidPhone,// 致邻Android手机版
    TrueLinkAndroidPad, // 致邻Android平板版
    TrueSens,           // 硬终端
    Imix,               // 网呈IMIX
    Other;              // 其他终端

    /**
     * 转为传给下层时需要使用的类型
     * */
    public EmDcsType toTransferType(){
        switch (this){
            case TrueLinkWindows:
                return EmDcsType.emTypeTrueLink;
            case TrueLinkIosPhone:
                return EmDcsType.emTypeTrueTouchPhoneIOS;
            case TrueLinkIosPad:
                return EmDcsType.emTypeTrueTouchPadIOS;
            case TrueLinkAndroidPhone:
                return EmDcsType.emTypeTrueTouchPhoneAndroid;
            case TrueLinkAndroidPad:
                return EmDcsType.emTypeTrueTouchPadAndroid;
            case TrueSens:
                return EmDcsType.emTypeTrueSens;
            case Imix:
                return EmDcsType.emTypeIMIX;
            case Other:
                return EmDcsType.emTypeThirdPartyTer;
            default:
                return EmDcsType.emTypeUnknown;
        }
    }
}
