package com.kedacom.vconf.sdk.base.settings;


import com.kedacom.vconf.sdk.amulet.Atlas;
import com.kedacom.vconf.sdk.annotation.Message;
import com.kedacom.vconf.sdk.annotation.Request;
import com.kedacom.vconf.sdk.annotation.Response;
import com.kedacom.vconf.sdk.common.type.BaseTypeBool;


@Message(
        module = "SET"
)
enum Msg {

    /**设置是否启用telnet调试*/
    @Request(method = "SetUseOspTelnetCfgCmd",
            owner = Atlas.ConfigCtrl,
            paras = StringBuffer.class,
            userParas = BaseTypeBool.class, // true启用
            rspSeq = "SetTelnetDebugEnableRsp"
    )
    SetTelnetDebugEnable,

    @Response(id = "SetUseOspTelnetCfg_Ntf",
            clz = BaseTypeBool.class)
    SetTelnetDebugEnableRsp,


    END;

}
