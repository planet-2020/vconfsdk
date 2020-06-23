package com.kedacom.vconf.sdk.base.login;


import com.kedacom.vconf.sdk.amulet.Atlas;
import com.kedacom.vconf.sdk.annotation.Message;
import com.kedacom.vconf.sdk.annotation.Request;
import com.kedacom.vconf.sdk.annotation.Response;
import com.kedacom.vconf.sdk.base.login.bean.transfer.*;
import com.kedacom.vconf.sdk.common.bean.transfer.TSrvStartResult;
import com.kedacom.vconf.sdk.common.type.TRestErrorInfo;


@Message(
        module = "LI"
)
enum Msg {

    /**启动业务组件功能模块
     * */
    @Request(method = "SYSStartService",
            owner = Atlas.MtServiceCfgCtrl,
            paras = StringBuffer.class,
            userParas = String.class, // 模块名称，如"rest"接入模块、"upgrade"升级模块
            rspSeq = "StartMtServiceRsp"
    )
    StartMtService,

    @Response(id = "SrvStartResultNtf",
            clz = TSrvStartResult.class)
    StartMtServiceRsp,

    /**配置Aps*/
    @Request(method = "SetAPSListCfgCmd",
            owner = Atlas.ConfigCtrl,
            paras = StringBuffer.class,
            userParas = MtXAPSvrListCfg.class,
            rspSeq = "SetApsServerCfgRsp")
    SetApsServerCfg,

    @Response(id = "SetXAPListCfgNtf",
            clz = MtXAPSvrListCfg.class)
    SetApsServerCfgRsp,


    /**登录APS*/
    @Request(method = "LoginApsServerCmd",
            owner = Atlas.LoginCtrl,
            paras = StringBuffer.class,
            userParas = TMTApsLoginParam.class,
            rspSeq = "LoginApsRsp",
            timeout = 10)
    LoginAps,

    @Response(id = "ApsLoginResultNtf",
            clz = TApsLoginResult.class)
    LoginApsRsp,


    /**注销APS*/
    @Request(method = "LogoutApsServerCmd",
            owner = Atlas.LoginCtrl,
            rspSeq = "LogoutApsRsp")
    LogoutAps,

    @Response(id = "SetSvrLoginStatusRtNtf",
            clz = TMtSvrStateList.class)
    LogoutApsRsp,

    /**获取平台为用户分配的token*/
    @Request(method = "MGRestGetPlatformAccountTokenReq",
            owner = Atlas.MeetingCtrl,
            paras = StringBuffer.class,
            userParas = String.class, // 点分十进制形式平台ip，从TApsLoginResult.dwIP字段转换而来
            rspSeq = "QueryAccountTokenRsp")
    QueryAccountToken,

    @Response(id = "RestGetPlatformAccountTokenRsp",
            clz = TRestErrorInfo.class)
    QueryAccountTokenRsp,

    /**登录platform*/
    @Request(method = "LoginPlatformServerReq",
            owner = Atlas.LoginCtrl,
            paras = StringBuffer.class,
            userParas = TMTWeiboLogin.class, // LoginAps时的用户名密码
            rspSeq = "LoginPlatformRsp")
    LoginPlatform,

    @Response(id = "RestPlatformAPILoginRsp",
            clz = TLoginPlatformRsp.class)
    LoginPlatformRsp,

    /**获取用户简短信息
     * */
    @Request(method = "GetUserInfoFromApsCfg",
            owner = Atlas.ConfigCtrl,
            paras = StringBuffer.class,
            userParas = TMTUserInfoFromAps.class,
            isGet = true
    )
    GetUserBrief,

    /**查询用户详情
     * */
    @Request(method = "GetAccountInfoReq",
            owner = Atlas.RmtContactCtrl,
            paras = StringBuffer.class,
            userParas = TMTAccountManagerSystem.class,
            rspSeq = "QueryUserDetailsRsp"
    )
    QueryUserDetails,

    @Response(id = "RestGetAccountInfo_Rsp",
            clz = TQueryUserDetailsRsp.class)
    QueryUserDetailsRsp,


    END;

}