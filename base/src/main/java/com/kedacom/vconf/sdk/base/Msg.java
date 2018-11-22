package com.kedacom.vconf.sdk.base;


import androidx.annotation.RestrictTo;

import com.kedacom.vconf.sdk.annotation.Get;
import com.kedacom.vconf.sdk.annotation.Message;
import com.kedacom.vconf.sdk.annotation.Notification;
import com.kedacom.vconf.sdk.annotation.Request;
import com.kedacom.vconf.sdk.annotation.Response;
import com.kedacom.vconf.sdk.annotation.Set;

// TODO 使用import static MsgBeans.*？

/**
 * Created by Sissi on 2018/9/3.
 * 消息。
 *
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
            timeout = 4,
            isMutualExclusive = true)
    Startup, // 请求不带Req后缀

    @Response(clz = MsgBeans.StartupResult.class, // Result表示反馈结果，注意区别Info
            delay = 500)
    StartupRsp,

    // login
    @Request(para = MsgBeans.LoginPara.class,
            rspSeq = {"LoginRsp", "LoginRspFin"},
            timeout = 6)
    Login,

    @Response(clz = MsgBeans.LoginResult.class, delay = 500)
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

    @Notification(clz = MsgBeans.MemberState.class, delay = 500)
    MemberStateChanged,


    //>>>>>>>>>>>>>>>>>>> 数据协作

    // 数据协作基础

    /**获取数据协作服务器地址*/
    @Get(result = MsgBeans.TMtDCSSvrAddr.class)
    DCGetServerAddr,

    /**登录数据协作建链*/
    @Deprecated  // 下层自动调用了，上层不需感知
    @Request(name = "DCSLoginConnectCmd",
            rspSeq = {"DcsLoginResult_Ntf"})
    DCBuildLink4Login,

    /**登录数据协作建链响应*/
    @Response(name = "DcsLoginResult_Ntf",
            clz = MsgBeans.DcsLinkCreationResult.class)
    DCBuildLink4LoginRsp,

    /**登录数据协作服务器。
     * 注：登录前需先建链。*/
    @Request(name = "DCSLoginSrvReq",
            para = MsgBeans.TDCSRegInfo.class,
            /*执行DCSLoginSrvReq时，下层自动执行了DCSLoginConnectCmd并把DcsLoginResult_Ntf抛了上来，
            所以上层看起来就是DCSLoginSrvReq对应{"DcsLoginResult_Ntf", "DcsLoginSrv_Rsp"}响应序列*/
            rspSeq = {"DcsLoginResult_Ntf", "DcsLoginSrv_Rsp"})
    DCLogin,

    /**登录数据协作服务器响应*/
    @Response(name = "DcsLoginSrv_Rsp",
            clz = MsgBeans.DcsLoginResult.class)
    DCLoginRsp,

    /**注销数据协作服务器*/
    @Request(name = "DCSLogoutReq",
            rspSeq = {"DcsLogout_Rsp"})
    DCLogout,

    /**注销数据协作服务器响应*/
    @Response(name = "DcsLogout_Rsp",
            clz = MsgBeans.TDCSResult.class)
    DCLogoutRsp,

    /**获取会议地址*/
    @Deprecated // 下层自动调用了，上层不需感知
    @Request(rspSeq = {"DcsGetConfAddr_Rsp"})
    DCSGetConfAddrReq,

    /**获取会议地址响应*/
    @Deprecated
    @Response
    DcsGetConfAddr_Rsp,

    /**创建数据协作建链*/
    @Deprecated // 下层自动调用了，上层不需感知
    @Request(name = "DCSConfConnectCmd",
            rspSeq = {"DcsConfResult_Ntf"})
    DCBuildLink4Conf,

    /**创建数据协作建链响应*/
    @Response(name = "DcsConfResult_Ntf",
            clz = MsgBeans.DcsConfResult.class)
    DCBuildLink4ConfRsp,

    /**创建数据协作。
     * 注：创建数据协作前需先建链。*/
    @Request(name = "DCSCreateConfReq",
            para = MsgBeans.DCSCreateConf.class,
            /*执行DCSCreateConfReq时，下层自动执行了DCSConfConnectCmd并把DcsConfResult_Ntf抛了上来，
            所以上层看起来就是DCSCreateConfReq对应{"DcsConfResult_Ntf", "DcsCreateConf_Rsp"}响应序列*/
            rspSeq = {"DcsConfResult_Ntf", "DcsCreateConf_Rsp"})
    DCCreateConf,

    /**己端创建数据协作时的响应；
     * 其他终端创建数据协作时的通知；
     *
     * 当会议中有人创建数据协作时，平台会发送一个邀请通知给各个与会方，
     * 下层（组件层）收到邀请会主动加入该数据协作，然后再上报该条消息给界面，
     * 所以该消息既是响应也是通知。*/
    @Notification(name = "DcsCreateConf_Rsp",
            clz = MsgBeans.TDCSCreateConfResult.class)
    @Response(name = "DcsCreateConf_Rsp",
            clz = MsgBeans.TDCSCreateConfResult.class, delay = 500)
    DCCreateConfRsp,


    /**加入会议时候，对会议地址的域名查询*/
    @Deprecated // 下层自动调用了，上层不需感知
    @Request(para=MsgBeans.TDCSConfAddr.class)
    DCSJoinConfDomainCmd,

    /**加入数据协作*/
    @Deprecated // 下层自动调用了，上层不需感知
    @Request(rspSeq = {"DcsJoinConf_Rsp"})
    DCSJoinConfReq,

    /**加入数据协作响应*/
    @Deprecated // 下层自动调用了，上层不需感知
    @Response
    DcsJoinConf_Rsp,

    /**退出数据协作。
     * 注：仅自己退出，协作仍存在，不影响其他人继续*/
    @Request(name = "DCSQuitConfReq",
            para = MsgBeans.DCSQuitConf.class,
            rspSeq = {"DcsQuitConf_Rsp"})
    DCQuitConf,

    /**退出数据协作响应*/
    @Response(name = "DcsQuitConf_Rsp",
            clz = MsgBeans.TDCSResult.class)
    DCQuitConfRsp,

    /**结束数据协作*/
    @Request(name = "DCSReleaseConfReq",
            para = MsgBeans.DCSBriefConfInfo.class,
            rspSeq = {"DcsReleaseConf_Rsp"})
    DCReleaseConf,

    /**结束数据协作响应*/
    @Response(name = "DcsReleaseConf_Rsp",
            clz = MsgBeans.TDCSResult.class)
    DCReleaseConfRsp,

    /**结束数据协作响应。
     * 注：该响应也会作为通知广播给其他与会者。*/
    @Notification(clz = MsgBeans.DcsReleaseConf_Ntf.class)
    @Response(clz = MsgBeans.DcsReleaseConf_Ntf.class) // TODO 确认是否确实需要作为response
    DcsReleaseConf_Ntf,

    /**当前终端拒绝入会*/
    @Request
    DCSRejectJoinConfCmd,


    // 数据协作权限控制相关

    /**（主席）添加协作方*/
    @Request(name = "DCSAddOperatorReq",
            para = MsgBeans.TDCSOperator.class,
            rspSeq = {"DcsAddOperator_Rsp"})
    DCAddOperator,

    /**添加协作方响应*/
    @Response(name = "DcsAddOperator_Rsp",
            clz = MsgBeans.TDCSResult.class)
    DCAddOperatorRsp,

    /**（主席）删除协作方*/
    @Request(name = "DCSDelOperatorReq",
            para = MsgBeans.TDCSOperator.class,
            rspSeq = {"DcsDelOperator_Rsp"})
    DCDelOperator,

    /**删除协作方响应*/
    @Response(name = "DcsDelOperator_Rsp",
            clz = MsgBeans.TDCSResult.class)
    DCDelOperatorRsp,

    /**（自己）申请作为协作方*/
    @Request(name = "DCSApplyOperReq",
            para =  MsgBeans.DCSBriefMemberInfo.class,
            rspSeq = {"DcsApplyOper_Rsp"})
    DCApplyOperator,

    /**申请作为协作方响应*/
    @Response(name = "DcsApplyOper_Rsp",
            clz = MsgBeans.TDCSResult.class)
    DCApplyOperatorRsp,

    /**（自己）取消作为协作方*/
    @Request(name = "DCSCancelOperReq",
            para =  MsgBeans.DCSBriefMemberInfo.class,
            rspSeq = {"DcsCancelOper_Rsp"})
    DCCancelOperator,

    /**取消作为协作方响应*/
    @Response(name = "DcsCancelOper_Rsp",
            clz = MsgBeans.TDCSResult.class)
    DCCancelOperatorRsp,

    /**成员申请协作权通知*/
    @Notification(name = "DcsUserApplyOper_Ntf",
            clz = MsgBeans.TDCSUserInfo.class)
    DCApplyOperatorNtf,

    /**（主席）拒绝成员申请作为协作方的请求*/
    @Request(name = "DCSRejectOperatorCmd",
            para=MsgBeans.TDCSOperator.class)
    DCRejectApplyOperator,

    /**获取数据协作会议中的所有成员（包括协作方普通方）*/
    @Request(name = "DCSGetUserListReq",
            para=MsgBeans.DCSBriefConfInfo.class,
            rspSeq = {"DcsGetUserList_Rsp"})
    DCQueryAllMembers,

    /**获取数据协作会议中的所有成员响应*/
    @Response(name = "DcsGetUserList_Rsp",
            clz = MsgBeans.DCSGetUserListRsp.class)
    DCQueryAllMembersRsp,


    // 数据协作画板相关

    /**新建画板*/
    @Request(name = "DCSNewWhiteBoardReq",
            para=MsgBeans.TDCSNewWhiteBoard.class,
            rspSeq = {"DcsNewWhiteBoard_Rsp"})
    DCNewPaintBoard,

    /**新建画板响应*/
    @Response(name = "DcsNewWhiteBoard_Rsp",
            clz=MsgBeans.DCSWhiteBoardResult.class)
    DCNewPaintBoardRsp,

    /**删除画板*/
    @Request(name = "DCSDelWhiteBoardReq",
            para=MsgBeans.DCSWhiteBoardIndex.class,
            rspSeq = {"DcsDelWhiteBoard_Rsp"})
    DCDelPaintBoard,

    /**删除画板响应*/
    @Response(name = "DcsDelWhiteBoard_Rsp",
            clz=MsgBeans.TDCSBoardResult.class)
    DCDelPaintBoardRsp,

    /**查询画板*/
    @Request(name = "DCSGetWhiteBoardReq",
            para=MsgBeans.DCSWhiteBoardIndex.class,
            rspSeq = {"DcsGetWhiteBoard_Rsp"})
    DCQueryPaintBoard,

    /**查询画板响应*/
    @Response(name = "DcsGetWhiteBoard_Rsp",
            clz=MsgBeans.DCSWhiteBoardResult.class)
    DCQueryPaintBoardRsp,

    /**查询所有画板*/
    @Request(name = "DCSGetAllWhiteBoardReq",
            para= MsgBeans.DCSBriefConfInfo.class,
            rspSeq = {"DcsGetAllWhiteBoard_Rsp"})
    DCQueryAllPaintBoards,

    /**获取所有白板响应*/
    @Response(name = "DcsGetAllWhiteBoard_Rsp",
            clz=MsgBeans.DCSGetAllWhiteBoardRsp.class)
    DCQueryAllPaintBoardsRsp,



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
    @Notification(clz = MsgBeans.TDCSBoardInfo.class)
    DcsDelWhiteBoard_Ntf,


    // 数据协作图元操作

    /**线操作*/
    @Request(para=MsgBeans.DCSOperLineOper.class)
    DCSOperLineOperInfoCmd,

    /**圆/椭圆操作*/
    @Request(para=MsgBeans.DCSOperCircleOper.class)
    DCSOperCircleOperInfoCmd,

    /**矩形操作*/
    @Request(para=MsgBeans.DCSOperRectangleOper.class)
    DCSOperRectangleOperInfoCmd,

    /**铅笔操作*/
    @Request(para=MsgBeans.DCSOperPencilOper.class)
    DCSOperPencilOperInfoCmd,

    /**彩笔操作*/
    @Deprecated
    @Request(para=MsgBeans.DCSOperColorPenOper.class)
    DCSOperColorPenOperInfoCmd,

    /**图片操作*/
    @Request(para=MsgBeans.DCSOperImageOper.class)
    DCSOperImageOperInfoCmd,

    /**添加子页*/
    @Request(para=MsgBeans.DCSOperAddSubPageOper.class)
    DCSOperAddSubPageInfoCmd,

    /**矩形擦除*/
    @Request(para=MsgBeans.DCSOperEraseOper.class)
    DCSOperEraseOperInfoCmd,

    /**放缩操作*/
    @Request(para=MsgBeans.DCSOperZoomOper.class)
    DCSOperZoomInfoCmd,

    /**发布图片信息*/
    @Request
    DCSSendImgFileInfoCmd,

    /**撤销*/
    @Request(para=MsgBeans.DCSOperUndoOper.class)
    DCSOperUndoCmd,

    /**恢复（恢复被撤销的操作）*/
    @Request(para=MsgBeans.DCSOperRedoOper.class)
    DCSOperRedoCmd,

    /**左旋转*/
    @Request(para=MsgBeans.TDCSOperReq.class)
    DCSOperRotateLeftCmd,

    /**右旋转*/
    @Request(para=MsgBeans.TDCSOperReq.class)
    DCSOperRotateRightCmd,

    /**清屏*/
    @Request(para=MsgBeans.TDCSOperReq.class)
    DCSOperClearScreenCmd,

    /**滚屏*/
    @Request(para=MsgBeans.DCSOperScrollOper.class)
    DCSOperScrollScreenCmd,

    /**获取图片地址*/
    @Request
    DCSGetImageUrlReq,

    /**上传文件*/
    @Request(para=MsgBeans.DCSTransferFile.class,
            rspSeq = {"DcsUploadFile_Ntf"})
    DCSUploadFileCmd,

    /**上传文件响应*/
    @Response(clz= MsgBeans.BaseTypeBool.class)
    DcsUploadFile_Ntf,


    /**上传图片地址*/
    @Request(para=MsgBeans.TDCSImageUrl.class,
            rspSeq = {"DcsUploadImage_Rsp"})
    DCSUploadImageReq,

    /**上传图片地址响应*/
    @Response(clz= MsgBeans.DCTransferPicUrlRsp.class)
    DcsUploadImage_Rsp,




    /**下载（图元、图片等）*/
    @Request(name = "DCSDownloadFileReq",
            para = MsgBeans.DownloadFilePara.class,
            rspSeq = {"DcsDownloadFile_Rsp"})
    DCDownload,

    /**下载响应*/
    @Response(name = "DcsDownloadFile_Rsp",
            clz = MsgBeans.TDCSFileLoadResult.class)
    DCDownloadRsp,

    /**获取图片下载地址*/
    @Request(name = "DCSDownloadImageReq",
            para=MsgBeans.TDCSImageUrl.class,
            rspSeq = {"DcsDownloadImage_Rsp"})
    DCQueryPicUrl,

    /**获取下载图片地址响应*/
    @Response(name = "DcsDownloadImage_Rsp",
            clz = MsgBeans.DCTransferPicUrlRsp.class)
    DCQueryPicUrlRsp,

    /**下载图片通知*/ //??? 干嘛的
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

    /**铅笔操作通知*/
    @Notification(clz = MsgBeans.DcsOperPencilOperInfo_Ntf.class)
    DcsOperPencilOperInfo_Ntf,

    /**彩笔操作通知*/
    @Deprecated
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

    /**matrix操作通知（缩放、移动、旋转）*/
    @Notification(clz = MsgBeans.DcsOperFullScreen_Ntf.class)
    DcsOperFullScreen_Ntf,

    /**撤销操作通知*/
    @Notification(clz = MsgBeans.DcsOperUndo_Ntf.class)
    DcsOperUndo_Ntf,

    /**恢复（恢复被撤销的操作）通知*/
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
