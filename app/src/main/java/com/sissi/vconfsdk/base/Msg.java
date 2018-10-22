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

    @Get(result = MsgBeans.TMtDCSSvrAddr.class)
    GetDCSCfg,

    /**登录数据协作服务器*/
    @Request(para = MsgBeans.TDCSRegInfo.class, rspSeq = {"DcsLoginResult_Ntf", "DcsLoginSrv_Rsp"})
    DCSLoginSrvReq,

    /** 登录数据协作服务器链路建立结果响应。
     * 注：登录数据协作服务器前需先建链，若建链成功才会有DcsLoginSrv_Rsp。*/
    @Response(clz = MsgBeans.DcsLinkCreationResult.class)
    DcsLoginResult_Ntf,

    /**登录数据协作服务器响应*/
    @Response(clz = MsgBeans.DcsLoginResult.class)
    DcsLoginSrv_Rsp,

    /**创建数据协作会议*/
    @Request()
    DCSCreateConfReq,

    /**创建数据协作会议链路建立结果响应。
     * 注：创建数据协作会议前需先建链，若建链成功才会有DcsCreateConf_Rsp。*/
    @Response(clz = MsgBeans.DcsConfResult.class)
    DcsConfResult_Ntf,

    /**创建数据协作会议响应。
     * 注：该响应也会作为通知广播给其他与会者。*/
    @Notification(clz = MsgBeans.TDCSCreateConfResult.class)
    @Response(clz = MsgBeans.TDCSCreateConfResult.class)
    DcsCreateConf_Rsp,

    /**注销数据协作服务器*/
    @Request
    DCSLogoutReq,

    /**退出数据协作。（仅自己退出，协作仍存在，不影响其他人继续）*/
    @Request()
    DCSQuitConfReq,

    /**退出数据协作响应*/
    @Response(clz = MsgBeans.TDCSResult.class)
    DcsQuitConf_Rsp,

    /**结束数据协作。*/
    @Request()
    DCSReleaseConfReq,

    /**结束数据协作响应。
     * 注：该响应也会作为通知广播给其他与会者。*/
    @Notification(clz = MsgBeans.DcsReleaseConf_Ntf.class)
    @Response(clz = MsgBeans.DcsReleaseConf_Ntf.class)
    DcsReleaseConf_Ntf,

    /**成员申请协作权通知*/
    @Notification(clz = MsgBeans.TDCSUserInfo.class)
    DcsUserApplyOper_Ntf,

    @Request()
    DCSAddOperatorReq,


    //TODO 添加请求

    /** 当前白板通知*/
    @Notification(clz = MsgBeans.TDCSBoardInfo.class)
    DcsCurrentWhiteBoard_Ntf,

    /**新建白板通知*/
    @Notification(clz = MsgBeans.TDCSBoardInfo.class)
    DcsNewWhiteBoard_Ntf,

    /**切换白板通知*/
    @Notification(clz = MsgBeans.TDCSBoardInfo.class)
    DcsSwitch_Ntf,

    /**删除白板通知*/
    @Notification(clz = MsgBeans.DcsDelWhiteBoard_Ntf.class)
    DcsDelWhiteBoard_Ntf,

    /**下载图元*/
    @Request
    DCSDownloadFileReq,

    /**下载图元响应*/
    @Response
    DcsDownloadFile_Rsp,

    /**获取下载图片地址*/
    @Request
    DCSDownloadImageReq,

    /**获取下载图片地址响应*/
    @Notification(clz = MsgBeans.DcsDownloadImage_Rsp.class)
    DcsDownloadImage_Rsp,

    /**下载图片通知*/
    @Notification(clz = MsgBeans.TDCSImageUrl.class)
    DownloadImage_Ntf,


    /**图元序列开始通知。
     * 注：新加入数据协作会议后，服务器会将当前数据协作会议中已存在的图元序列同步到新加入的与会方。*/
    @Notification(clz = MsgBeans.DcsElementOperBegin_Ntf.class)
    DcsElementOperBegin_Ntf,

    /**直线操作通知*/
    @Notification(clz = MsgBeans.DcsOperLineOperInfo_Ntf.class)
    DcsOperLineOperInfo_Ntf,

    /**圆/椭圆操作通知*/
    @Notification(clz = MsgBeans.DcsOperCircleOperInfo_Ntf.class)
    DcsOperCircleOperInfo_Ntf,

    /**矩形操作通知*/
    @Notification(clz = MsgBeans.DcsOperRectangleOperInfo_Ntf.class)
    DcsOperRectangleOperInfo_Ntf,

    /**曲线操作通知*/
    @Notification(clz = MsgBeans.DcsOperPencilOperInfo_Ntf.class)
    DcsOperPencilOperInfo_Ntf,

    /**曲线操作通知（彩笔）*/
    @Notification(clz = MsgBeans.DcsOperColorPenOperInfo_Ntf.class)
    DcsOperColorPenOperInfo_Ntf,

    /**图片插入操作通知*/
    @Notification(clz = MsgBeans.DcsOperInsertPic_Ntf.class)
    DcsOperInsertPic_Ntf,

    /**图片拖动操作通知*/
    @Notification(clz = MsgBeans.DcsOperPitchPicDrag_Ntf.class)
    DcsOperPitchPicDrag_Ntf,

    /**图片删除操作通知*/
    @Notification(clz = MsgBeans.DcsOperPitchPicDel_Ntf.class)
    DcsOperPitchPicDel_Ntf,

    /**矩形擦除操作通知*/
    @Notification(clz = MsgBeans.DcsOperEraseOperInfo_Ntf.class)
    DcsOperEraseOperInfo_Ntf,

    /**缩放或滚动操作通知*/
    @Notification(clz = MsgBeans.DcsOperFullScreen_Ntf.class)
    DcsOperFullScreen_Ntf,

    /**撤销操作通知*/
    @Notification(clz = MsgBeans.DcsOperUndo_Ntf.class)
    DcsOperUndo_Ntf,

    /**重复操作通知*/
    @Notification(clz = MsgBeans.DcsOperRedo_Ntf.class)
    DcsOperRedo_Ntf,

    /**清屏操作通知*/
    @Notification(clz = MsgBeans.TDCSOperContent.class)
    DcsOperClearScreen_Ntf,

    /**图元序列结束通知*/
    @Notification(clz = MsgBeans.TDcsCacheElementParseResult.class)
    DcsElementOperFinal_Ntf,


    //<<<<<<<<<<<<<<<<<< 数据协作

}
