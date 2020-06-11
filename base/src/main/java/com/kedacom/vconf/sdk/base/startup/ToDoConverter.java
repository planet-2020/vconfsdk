package com.kedacom.vconf.sdk.base.startup;

import com.kedacom.vconf.sdk.base.startup.bean.TerminalType;
import com.kedacom.vconf.sdk.common.constant.EmMtModel;

/**
 * Created by Sissi on 2019/8/5
 */
final class ToDoConverter {

    static TerminalType fromTransferObj(EmMtModel mtModel){
        switch (mtModel){
            case emSkyAndroidPhone:
                return TerminalType.Sky;
//            case emSkyAndroidPad:
//                return TerminalType.Sky;
            case emTrueTouchAndroidPhone:
                return TerminalType.TT;
//            case emTrueTouchAndroidPad:
//                return TerminalType.TT;
            default:
                return TerminalType.Unknown;
        }
    }

    static EmMtModel toTransferObj(TerminalType terminalType){
        switch (terminalType){
            case Sky:
                return EmMtModel.emSkyAndroidPhone;
//            case TT:
//                return EmMtModel.emSkyAndroidPad;
            case TT:
                return EmMtModel.emTrueTouchAndroidPhone;
//            case SkyAndroidPad:
//                return EmMtModel.emTrueTouchAndroidPad;
            case Movision:
                return EmMtModel.emSkyAndroidPhone;
            default:
                return EmMtModel.emModelBegin;
        }
    }

}
