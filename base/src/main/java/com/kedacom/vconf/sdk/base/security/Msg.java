package com.kedacom.vconf.sdk.base.security;


import com.kedacom.vconf.sdk.amulet.Atlas;
import com.kedacom.vconf.sdk.annotation.Module;
import com.kedacom.vconf.sdk.annotation.Request;
import com.kedacom.vconf.sdk.annotation.Response;
import com.kedacom.vconf.sdk.common.type.BaseTypeBool;

@Module(name = "SEC")
enum Msg {

    /**设置是否开启交互式调试*/
    @Request(name = "SetUseOspTelnetCfgCmd",
            owner = Atlas.ConfigCtrl,
            paras = StringBuffer.class,
            userParas = BaseTypeBool.class, // true启用
            rspSeq = "EnableInteractiveDebugRsp"
    )
    SetEnableInteractiveDebug,

    @Response(name = "SetUseOspTelnetCfg_Ntf",
            clz = BaseTypeBool.class)
    SetEnableInteractiveDebugRsp,

    /**判断交互式调试是否已开启*/
    @Request(name = "GetUseOspTelnetCfg",
            owner = Atlas.ConfigCtrl,
            paras = StringBuffer.class,
            userParas = BaseTypeBool.class, // true已启用
            isGet = true
    )
    HasEnabledInteractiveDebug,


    END;

}
