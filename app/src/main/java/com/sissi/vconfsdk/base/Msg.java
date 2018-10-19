package com.sissi.vconfsdk.base;


import android.support.annotation.RestrictTo;

import com.sissi.vconfsdk.annotation.Get;
import com.sissi.vconfsdk.annotation.Message;
import com.sissi.vconfsdk.annotation.Notification;
import com.sissi.vconfsdk.annotation.Request;
import com.sissi.vconfsdk.annotation.Response;
import com.sissi.vconfsdk.annotation.Set;

/**
 * Created by Sissi on 2018/9/3.
 */

@SuppressWarnings("unused")
@RestrictTo(RestrictTo.Scope.LIBRARY)
@Message
public enum Msg {

    @Response
    Timeout,

    // startup
    @Request(para = MsgBeans.StartupPara.class,  // para表示传入参数
            rspSeq = {"StartupRsp"},
            timeout = 4)
    Startup, // 请求不带Req后缀

    @Response(clz = MsgBeans.StartupResult.class, // Result表示反馈结果，注意区别Info
            delay = 3000)
    StartupRsp,

    // login
    @Request(para = MsgBeans.LoginPara.class,
            rspSeq = {"LoginRsp", "LoginRspFin"},
            timeout = 6)
    Login,

    @Response(clz = MsgBeans.LoginResult.class, delay = 5000)
    LoginRsp,
    @Response(clz = MsgBeans.LoginResult.class, delay = 500)
    LoginRspFin,

    // logout
    @Request(para = MsgBeans.LogoutPara.class, rspSeq = {"LogoutRsp", "LogoutRspFin"}, timeout = 5)
    Logout,
    @Response(clz = String.class)
    LogoutRsp,
    @Response(clz = String.class)
    LogoutRspFin,

    @Get(result = MsgBeans.XmppServerInfo.class)
    GetXmppServerInfo,

    @Set(MsgBeans.NetConfig.class)
    SetNetConfig,

    @Notification(clz = MsgBeans.MemberState.class, delay = 6000)
    MemberStateChanged,  // 通知不加Ntf后缀，而使用动词分词表事件发生


    //>>>>>>>>>>>>>>>>>>> 数据协作

    @Request(para = MsgBeans.TDCSRegInfo.class)
    DCSLoginSrvReq,

    @Response(/*clz = MsgBeans.DcsLoginResult.class*/)
    DcsLoginSrv_Rsp,

    @Response(clz = MsgBeans.DcsLoginResult.class)
    DcsLoginResult_Ntf,  // 数据协作服务器登录响应

    @Request
    DCSLogoutReq,

    @Response(clz = MsgBeans.DcsConfResult.class)
    DcsConfResult_Ntf,

    @Request()
    DCSCreateConfReq, // 创建数据协作

    @Notification(clz = MsgBeans.TDCSCreateConfResult.class)
    @Response(clz = MsgBeans.TDCSCreateConfResult.class)
    DcsCreateConf_Rsp, // 数据协作创建反馈

    @Request()
    DCSQuitConfReq, // 退出数据协作

    @Request()
    DCSReleaseConfReq, // 结束数据协作

    @Request()
    DCSAddOperatorReq,


    //TODO 添加请求

    @Notification(clz = MsgBeans.TDCSBoardInfo.class)
    DcsCurrentWhiteBoard_Ntf, // 当前白板通知

    @Notification(clz = MsgBeans.TDCSBoardInfo.class)
    DcsNewWhiteBoard_Ntf, // 新建白板通知

    @Notification(clz = MsgBeans.TDCSBoardInfo.class)
    DcsSwitch_Ntf, // 白板切换通知

    @Notification(clz = MsgBeans.DcsElementOperBegin_Ntf.class)
    DcsElementOperBegin_Ntf,

    @Notification(clz = MsgBeans.DcsOperLineOperInfo_Ntf.class)
    DcsOperLineOperInfo_Ntf,

    @Notification(clz = MsgBeans.DcsOperCircleOperInfo_Ntf.class)
    DcsOperCircleOperInfo_Ntf,

    @Notification(clz = MsgBeans.DcsOperRectangleOperInfo_Ntf.class)
    DcsOperRectangleOperInfo_Ntf,

    @Notification(clz = MsgBeans.DcsOperPencilOperInfo_Ntf.class)
    DcsOperPencilOperInfo_Ntf,

    @Notification(clz = MsgBeans.DcsOperColorPenOperInfo_Ntf.class)
    DcsOperColorPenOperInfo_Ntf,

    @Notification(clz = MsgBeans.DcsOperInsertPic_Ntf.class)
    DcsOperInsertPic_Ntf,

    @Notification(clz = MsgBeans.DcsOperPitchPicDrag_Ntf.class)
    DcsOperPitchPicDrag_Ntf,

    @Notification(clz = MsgBeans.DcsOperPitchPicDel_Ntf.class)
    DcsOperPitchPicDel_Ntf,

    @Notification(clz = MsgBeans.DcsOperEraseOperInfo_Ntf.class)
    DcsOperEraseOperInfo_Ntf,

    @Notification(clz = MsgBeans.DcsOperFullScreen_Ntf.class)
    DcsOperFullScreen_Ntf,

    @Notification(clz = MsgBeans.DcsOperUndo_Ntf.class)
    DcsOperUndo_Ntf,

    @Notification(clz = MsgBeans.DcsOperRedo_Ntf.class)
    DcsOperRedo_Ntf,

    @Notification(clz = MsgBeans.TDCSOperContent.class)
    DcsOperClearScreen_Ntf,

    @Notification(clz = MsgBeans.TDcsCacheElementParseResult.class)
    DcsElementOperFinal_Ntf,

    @Notification(clz = MsgBeans.DcsDownloadImage_Rsp.class)
    DcsDownloadImage_Rsp,

    @Notification(clz = MsgBeans.TDCSImageUrl.class)
    DownloadImage_Ntf,

    @Notification(clz = MsgBeans.DcsDelWhiteBoard_Ntf.class)
    DcsDelWhiteBoard_Ntf,

    @Notification(clz = MsgBeans.TDCSResult.class)
    DcsQuitConf_Rsp,

    @Notification(clz = MsgBeans.DcsReleaseConf_Ntf.class)
    DcsReleaseConf_Ntf,

    @Notification(clz = MsgBeans.TDCSUserInfo.class)
    DcsUserApplyOper_Ntf,

    //<<<<<<<<<<<<<<<<<< 数据协作

}
