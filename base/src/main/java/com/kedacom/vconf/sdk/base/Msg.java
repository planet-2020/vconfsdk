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

//    /**获取数据协作服务器地址*/
//    @Get(result = MsgBeans.DCServerAddr.class)
//    DCGetServerAddr,

//    /**登录数据协作建链*/
//    @Deprecated  // 下层自动调用了，上层不需感知
//    @Request(rspSeq = {"DCBuildLink4LoginRsp"})
//    DCBuildLink4Login,

    /**登录数据协作建链响应*/
    @Response(clz = MsgBeans.CommonResult.class)
    DCBuildLink4LoginRsp,

    /**登录数据协作服务器。
     * 注：登录前需先建链。*/
    @Request(para = MsgBeans.DCLoginPara.class,
            rspSeq = {"DCBuildLink4LoginRsp",  // 登录时下层自动建链，然后就抛了这条消息上来。NOTE: 对于失败的情形只会收到DCBuildLink4LoginRsp而没有DCLoginRsp。
                    "DCLoginRsp"})
    DCLogin,

    /**登录数据协作服务器响应*/
    @Response(clz = MsgBeans.CommonResult.class)
    DCLoginRsp,

    /**注销数据协作服务器*/
    @Request(rspSeq = {"DCLogoutRsp"})
    DCLogout,

    /**注销数据协作服务器响应*/
    @Response(clz = MsgBeans.CommonResult.class)
    DCLogoutRsp,

//    /**获取会议地址*/
//    @Deprecated // 下层自动调用了，上层不需感知
//    @Request(rspSeq = {"DCQueryConfAddrRsp"})
//    DCQueryConfAddr,
//
//    /**获取会议地址响应*/
//    @Deprecated
//    @Response
//    DCQueryConfAddrRsp,
//
//    /**创建数据协作建链*/
//    @Deprecated // 下层自动调用了，上层不需感知
//    @Request(rspSeq = {"DCBuildLink4ConfRsp"})
//    DCBuildLink4Conf,

    /**创建数据协作建链响应*/
    @Response(clz = MsgBeans.CommonResult.class)
    DCBuildLink4ConfRsp,

    /**创建数据协作。
     * 注：创建数据协作前需先建链。*/
    @Request(para = MsgBeans.DCCreateConfPara.class,
            rspSeq = {"DCBuildLink4ConfRsp",  // 创建数据协作时下层自动建链，然后就抛了这条消息上来。NOTE: 对于失败的情形只会收到DCBuildLink4ConfRsp而没有DCCreateConfRsp。
                    "DCConfCreated"})
    DCCreateConf,

    /**己端创建数据协作时的响应；
     * 其他终端创建数据协作时的通知；
     *
     * 当会议中有人创建数据协作时，平台会发送一个邀请通知给各个与会方，
     * 下层（组件层）收到邀请会主动加入该数据协作，然后再上报该条消息给界面，
     * 所以该消息既是响应也是通知。*/
    @Notification(clz = MsgBeans.DCCreateConfResult.class)
    @Response(clz = MsgBeans.DCCreateConfResult.class)
    DCConfCreated,  // 命名规范，对于响应、通知双重身份的消息命名规则为：名词+分词


//    /**加入会议时候，对会议地址的域名查询*/
//    @Deprecated // 下层自动调用了，上层不需感知
//    @Request(para=MsgBeans.TDCSConfAddr.class)
//    DCSJoinConfDomainCmd,
//
//    /**加入数据协作*/
//    @Deprecated // 下层自动调用了，上层不需感知
//    @Request(rspSeq = {"DcsJoinConf_Rsp"})
//    DCSJoinConfReq,
//
//    /**加入数据协作响应*/
//    @Deprecated // 下层自动调用了，上层不需感知
//    @Response
//    DcsJoinConf_Rsp,

    /**退出数据协作。
     * 注：仅自己退出，协作仍存在，不影响其他人继续*/
    @Request(para = MsgBeans.DCSQuitConf.class,
            rspSeq = {"DCQuitConfRsp"})
    DCQuitConf,

    /**退出数据协作响应*/
    @Response(clz = MsgBeans.CommonResult.class)
    DCQuitConfRsp,

    /**结束数据协作*/
    @Request(para = MsgBeans.DCConfId.class,
            rspSeq = {"DCReleaseConfRsp"})
    DCReleaseConf,

    /**结束数据协作响应*/
    @Response(clz = MsgBeans.CommonResult.class)
    DCReleaseConfRsp,

    /**结束数据协作响应。
     * 注：该响应也会作为通知广播给其他与会者。*/
    @Notification(clz = MsgBeans.DcsReleaseConf_Ntf.class)
    @Response(clz = MsgBeans.DcsReleaseConf_Ntf.class) // TODO 确认是否确实需要作为response
    DcsReleaseConf_Ntf, // TODO 重命名

//    /**当前终端拒绝入会*/
//    @Deprecated // 下层已经做掉了（目前是始终同意入数据协作）。入会后若当前会议存在数据协作，平台会通知各终端入数据协作，各终端可选择加入或拒绝。
//    @Request
//    DCSRejectJoinConfCmd,


    // 数据协作权限控制相关

    /**（主席）添加协作方*/
    @Request(para = MsgBeans.DCMember[].class,
            rspSeq = {"DCAddOperatorRsp"})
    DCAddOperator,

    /**添加协作方响应*/
    @Response(clz = MsgBeans.CommonResult.class)
    DCAddOperatorRsp,

    /**（主席）删除协作方*/
    @Request(para = MsgBeans.DCMember[].class,
            rspSeq = {"DCDelOperatorRsp"})
    DCDelOperator,

    /**删除协作方响应*/
    @Response(clz = MsgBeans.CommonResult.class)
    DCDelOperatorRsp,

    /**（自己）申请作为协作方*/
    @Request(para =  MsgBeans.DCMemberId.class,
            rspSeq = {"DCApplyOperatorRsp"})
    DCApplyOperator,

    /**申请作为协作方响应*/
    @Response(clz = MsgBeans.CommonResult.class)
    DCApplyOperatorRsp,

    /**（自己）取消作为协作方*/
    @Request(para =  MsgBeans.DCMemberId.class,
            rspSeq = {"DCCancelOperatorRsp"})
    DCCancelOperator,

    /**取消作为协作方响应*/
    @Response(clz = MsgBeans.CommonResult.class)
    DCCancelOperatorRsp,

    /**成员（向主席）申请协作权通知*/
    @Notification(clz = MsgBeans.DCMember[].class)
    DCApplyOperatorNtf,

    /**（主席）拒绝成员申请作为协作方的请求*/
    @Request(para=MsgBeans.DCMember[].class)
    DCRejectApplyOperator,

    /**获取数据协作会议中的所有成员（包括协作方普通方）*/
    @Request(para=MsgBeans.DCConfId.class,
            rspSeq = {"DCQueryAllMembersRsp"})
    DCQueryAllMembers,

    /**获取数据协作会议中的所有成员响应*/
    @Response(clz = MsgBeans.DCQueryAllMembersResult.class)  // 需单独定义的响应结构体名加Result后缀，需单独定义的参数名加para后缀
    DCQueryAllMembersRsp,


    // 数据协作画板相关

    /**新建画板*/
    @Request(para=MsgBeans.DCBoard.class,
            rspSeq = {"DCNewBoardRsp"})
    DCNewBoard,

    /**新建画板响应*/
    @Response(clz=MsgBeans.CommonResult.class)
    DCNewBoardRsp,

    /**删除画板*/
    @Request(para=MsgBeans.DCBoardId.class,
            rspSeq = {"DCDelBoardRsp"})
    DCDelBoard,

    /**删除画板响应*/
    @Response(clz=MsgBeans.CommonResult.class)
    DCDelBoardRsp,

    /**查询画板*/
    @Request(para=MsgBeans.DCBoardId.class,
            rspSeq = {"DCQueryBoardRsp"})
    DCQueryBoard,

    /**查询画板响应*/
    @Response(clz=MsgBeans.DCQueryBoardResult.class)
    DCQueryBoardRsp,

    /**查询所有画板*/
    @Request(para= MsgBeans.DCConfId.class,
            rspSeq = {"DCQueryAllBoardsRsp"})
    DCQueryAllBoards,

    /**查询所有白板响应*/
    @Response(clz=MsgBeans.DCQueryAllBoardsResult.class)
    DCQueryAllBoardsRsp,

    /**添加子页*/
    @Request(para=MsgBeans.DCSOperAddSubPageOper.class)
    DCAddSubPage,   // TODO 确认


    /** 当前画板通知*/
    @Notification(clz = MsgBeans.DCBoard.class)
    DCCurrentBoardNtf,

    /**新建画板通知*/
    @Notification(clz = MsgBeans.DCBoard.class)
    DCBoardCreatedNtf,

    /**切换画板通知*/
    @Notification(clz = MsgBeans.DCBoard.class)
    DCBoardSwitchedNtf,

    /**删除画板通知*/
    @Notification(clz = MsgBeans.DCBoard.class)
    DCBoardDeletedNtf,


    // 数据协作图元操作

    /**画线*/
    @Request(para=MsgBeans.DCLineOp.class)
    DCDrawLine,

    /**画圆/椭圆*/
    @Request(para=MsgBeans.DCOvalOp.class)
    DCDrawOval,

    /**画矩形*/
    @Request(para=MsgBeans.DCRectOp.class)
    DCDrawRect,

    /**画路径（铅笔操作）*/
    @Request(para=MsgBeans.DCPathOp.class)
    DCDrawPath,

//    /**彩笔操作*/
//    @Deprecated
//    @Request(para=MsgBeans.DCSOperColorPenOper.class)
//    DCColorPenOp,

//    /**图片操作*/
//    @Deprecated
//    @Request(para=MsgBeans.DCSOperImageOper.class)
//    DCSOperImageOperInfoCmd,

    /**矩形擦除*/
    @Request(para=MsgBeans.DCRectEraseOp.class)
    DCRectErase,

    /**清屏*/
    @Request(para=MsgBeans.DCPaintOp.class)
    DCClearScreen,


    // 数据协作矩阵操作

    /**放缩*/
    @Request(para=MsgBeans.DCZoomOp.class)
    DCZoom,

    /**左旋转*/
    @Request(para=MsgBeans.DCPaintOp.class)
    DCRotateLeft,

    /**右旋转*/
    @Request(para=MsgBeans.DCPaintOp.class)
    DCRotateRight,

    /**滚屏*/
    @Request(para=MsgBeans.DCScrollOp.class)
    DCScrollScreen,


    // 数据协作图元控制操作

    /**撤销*/
    @Request(para=MsgBeans.DCPaintOp.class)
    DCUndo,

    /**恢复（恢复被撤销的操作）*/
    @Request(para=MsgBeans.DCPaintOp.class)
    DCRedo,


    // 数据协作文件操作

    /**上传文件*/
    @Request(para=MsgBeans.DCSTransferFile.class,
            rspSeq = {"DcsUploadFile_Ntf"})
    DCSUploadFileCmd, // TODO 待定

    /**上传文件响应*/
    @Response(clz= MsgBeans.BaseTypeBool.class)
    DcsUploadFile_Ntf, // TODO 待定


    /**上传图片地址*/
    @Request(para=MsgBeans.DCQueryPicUrlPara.class,
            rspSeq = {"DcsUploadImage_Rsp"})
    DCSUploadImageReq, // TODO 待定

    /**上传图片地址响应*/
    @Response(clz= MsgBeans.DCQueryPicUrlResult.class)
    DcsUploadImage_Rsp, // TODO 待定


    /**发布图片信息*/
    @Request
    DCSSendImgFileInfoCmd, // TODO 待定



    /**下载（图元、图片等）*/
    @Request(para = MsgBeans.DownloadPara.class,
            rspSeq = {"DCDownloadRsp"})
    DCDownload,

    /**下载响应*/
    @Response(clz = MsgBeans.DownloadResult.class)
    DCDownloadRsp,

    /**获取图片下载地址*/
    @Request(para=MsgBeans.DCQueryPicUrlPara.class,
            rspSeq = {"DCQueryPicUrlRsp"})
    DCQueryPicUrl,

    /**获取下载图片地址响应*/
    @Response(clz = MsgBeans.DCQueryPicUrlResult.class)
    DCQueryPicUrlRsp,

    /**下载图片通知*/ //??? 干嘛的
    @Notification(clz = MsgBeans.DCQueryPicUrlPara.class)
    DownloadImage_Ntf, // TODO 待定



    // 数据协作图元操作通知

    /**图元序列开始通知。
     * 注：新加入数据协作会议后，服务器会将当前数据协作会议中已存在的图元序列同步到新加入的与会方。*/
    @Notification
    DCElementBeginNtf,

    /**画直线通知*/
    @Notification(clz = MsgBeans.DCLineOp.class)
    DCLineDrawnNtf,

    /**画圆/椭圆通知*/
    @Notification(clz = MsgBeans.DCOvalOp.class)
    DCOvalDrawnNtf,

    /**画矩形通知*/
    @Notification(clz = MsgBeans.DCRectOp.class)
    DCRectDrawnNtf,

    /**画路径（铅笔操作）通知*/
    @Notification(clz = MsgBeans.DCPathOp.class)
    DCPathDrawnNtf,

//    /**彩笔操作通知*/
//    @Deprecated
//    @Notification(clz = MsgBeans.DcsOperColorPenOperInfo_Ntf.class)
//    DcsOperColorPenOperInfo_Ntf,

    /**图片插入通知*/
    @Notification(clz = MsgBeans.DCInertPicOp.class)
    DCPicInsertedNtf,

    /**图片拖动通知*/
    @Notification(clz = MsgBeans.DCDragPicOp.class)
    DCPicDraggedNtf,

    /**图片删除通知*/
    @Notification(clz = MsgBeans.DCDelPicOp.class)
    DCPicDeletedNtf,

    /**矩形擦除通知*/
    @Notification(clz = MsgBeans.DCRectEraseOp.class)
    DCRectErasedNtf,

    /**全屏matrix操作通知（缩放、移动、旋转）*/
    @Notification(clz = MsgBeans.DCFullScreenMatrixOp.class)
    DCFullScreenMatrixOpNtf,

    /**撤销操作通知*/
    @Notification(clz = MsgBeans.DCPaintOp.class)
    DCUndoneNtf,

    /**恢复（恢复被撤销的操作）通知*/
    @Notification(clz = MsgBeans.DCPaintOp.class)
    DCRedoneNtf,

    /**清屏通知*/
    @Notification(clz = MsgBeans.DCPaintOp.class)
    DCScreenClearedNtf,

    /**图元序列结束通知*/
    @Notification
    DCElementEndNtf,


    //<<<<<<<<<<<<<<<<<< 数据协作

}
