package com.kedacom.vconf.sdk.main.startup;

import com.kedacom.vconf.sdk.main.startup.bean.ETerminalType;
import com.kedacom.vconf.sdk.main.startup.bean.transfer.EmMtModel;

/**
 * Created by Sissi on 2019/8/5
 */
final class ToDoConverter {

    static ETerminalType fromTransferObj(EmMtModel mtModel){
        switch (mtModel){
            case emSkyAndroidPhone:
                return ETerminalType.TrueLinkAndroidPhone;
            case emSkyAndroidPad:
                return ETerminalType.TrueLinkAndroidPad;
            case emTrueTouchAndroidPhone:
                return ETerminalType.SkyAndroidPhone;
            case emTrueTouchAndroidPad:
                return ETerminalType.SkyAndroidPad;
            default:
                return ETerminalType.Other;
        }
    }

    static EmMtModel toTransferObj(ETerminalType terminalType){
        switch (terminalType){
            case TrueLinkAndroidPhone:
                return EmMtModel.emSkyAndroidPhone;
            case TrueLinkAndroidPad:
                return EmMtModel.emSkyAndroidPad;
            case SkyAndroidPhone:
                return EmMtModel.emTrueTouchAndroidPhone;
            case SkyAndroidPad:
                return EmMtModel.emTrueTouchAndroidPad;
            default:
                return EmMtModel.emSkyAndroidPhone;
        }
    }

}
