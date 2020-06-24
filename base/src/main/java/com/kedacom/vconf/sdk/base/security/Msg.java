package com.kedacom.vconf.sdk.base.security;


import com.kedacom.vconf.sdk.amulet.Atlas;
import com.kedacom.vconf.sdk.annotation.Message;
import com.kedacom.vconf.sdk.annotation.Request;
import com.kedacom.vconf.sdk.annotation.Response;
import com.kedacom.vconf.sdk.common.type.BaseTypeBool;


@Message(
        module = "SEC"
)
enum Msg {

    /**设置是否开启交互式调试*/
    @Request(method = "SetUseOspTelnetCfgCmd",
            owner = Atlas.ConfigCtrl,
            paras = StringBuffer.class,
            userParas = BaseTypeBool.class, // true启用
            rspSeq = "EnableInteractiveDebugRsp"
    )
    SetEnableInteractiveDebug,

    @Response(id = "SetUseOspTelnetCfg_Ntf",
            clz = BaseTypeBool.class)
    SetEnableInteractiveDebugRsp,

    /**判断交互式调试是否已开启*/
    @Request(method = "GetUseOspTelnetCfg",
            owner = Atlas.ConfigCtrl,
            paras = StringBuffer.class,
            userParas = BaseTypeBool.class, // true已启用
            isGet = true
    )
    HasEnabledInteractiveDebug,


    END;

}
