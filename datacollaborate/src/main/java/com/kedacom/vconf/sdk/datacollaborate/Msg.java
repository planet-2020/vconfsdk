package com.kedacom.vconf.sdk.datacollaborate;


import androidx.annotation.RestrictTo;

import com.kedacom.vconf.sdk.annotation.Message;
import com.kedacom.vconf.sdk.annotation.Request;
import com.kedacom.vconf.sdk.annotation.Response;
import com.kedacom.vconf.sdk.common.type.BaseTypeString;
import com.kedacom.vconf.sdk.datacollaborate.bean.transfer.*;


/**
 * Created by Sissi on 2018/9/3.
 * 数据协作消息定义。
 */

@RestrictTo(RestrictTo.Scope.LIBRARY)
@Message(
        module = "DC" // abbreviation for DataCollaborate
)
public enum Msg {
    // 数据协作基础

    /**获取数据协作服务器地址*/
    @Request(method = "GetDCSCfg",
            owner = MethodOwner.ConfigCtrl,
            paras = StringBuffer.class,
            userParas = TDCSSvrAddr.class,
            type = Request.GET)
    GetServerAddr,

    /**获取数据协作相关状态*/
    @Request(method = "GetDCSServerStateRt",
            owner = MethodOwner.ConfigCtrl,
            paras = StringBuffer.class,
            userParas = TDCSSrvState.class,
            type = Request.GET)
    GetState,

    /**
     * 登录链路状态变化
     */
    @Response(clz = TDCSConnectResult.class,
            id = "DcsLoginResult_Ntf")
    LoginLinkStateChanged,

    /**
     * 登录数据协作服务器
     */
    @Request(method = "LoginSrvReq",
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSRegInfo.class,
            rspSeq = {"LoginLinkStateChanged",
                    "LoginRsp"})
    Login,

    /**
     * 登录数据协作服务器响应
     */
    @Response(clz = TDCSResult.class,
            id = "DcsLoginSrv_Rsp")
    LoginRsp,

    /**
     * 注销数据协作服务器
     */
    @Request(method = "DCSLogoutReq",
            owner = MethodOwner.DcsCtrl,
            rspSeq = {"LogoutRsp",
                    "LoginLinkStateChanged"
    })
    Logout,

    /**
     * 注销数据协作服务器响应
     */
    @Response(clz = TDCSResult.class,
            id = "DcsLogout_Rsp")
    LogoutRsp,


    /**查询数据协作地址*/
    @Request(method = "DCSGetConfAddrReq",
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = String.class, // 数据协作所在会议e164号
            rspSeq = "QueryAddrRsp"
    )
    QueryAddr,

    /**
     * 查询数据协作地址响应
     */
    @Response(clz = DcsGetConfAddrRsp.class,
            id = "DcsGetConfAddr_Rsp")
    QueryAddrRsp,

    /**
     * 数据协作链路状态变化
     */
    @Response(clz = TDCSConnectResult.class,
            id = "DcsConfResult_Ntf")
    LinkStateChanged,

    /**
     * 开启数据协作
     */
    @Request(method = "DCSCreateConfReq",
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSCreateConf.class,
            rspSeq = {"LinkStateChanged",  // NOTE: 若该响应bSuccess字段为false则不会收到DCConfCreated。
                    "CollaborateStarted"},
            rspSeq2 = "CollaborateStarted")
    StartCollaborate,

    /**
     * 数据协作已开启
     */
    @Response(clz = TDCSCreateConfResult.class,
            id = "DcsCreateConf_Rsp")
    CollaborateStarted,


    /**
     * 退出数据协作。
     * 注：仅自己退出，协作仍存在，不影响其他人继续
     */
    @Request(method = "DCSQuitConfReq",
            owner = MethodOwner.DcsCtrl,
            paras = {StringBuffer.class, int.class},
            userParas = {String.class, // 会议e164
                    Integer.class // 是否同时退出会议。0表示退出协作的同时退出会议，1表示仅退出协作。
            },
            rspSeq = {"QuitCollaborateRsp",
                    "LinkStateChanged"})
    QuitCollaborate,

    /**
     * 退出数据协作响应
     */
    @Response(clz = TDCSResult.class,
            id = "DcsQuitConf_Rsp")
    QuitCollaborateRsp,

    /**
     * 结束数据协作
     */
    @Request(method = "DCSReleaseConfReq",
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = String.class, // 会议e164
            rspSeq = {"FinishCollaborateRsp","LinkStateChanged"},
            rspSeq2 = {"FinishCollaborateRsp","CollaborateFinished","LinkStateChanged"})
    FinishCollaborate,

    /**
     * 结束数据协作响应
     */
    @Response(clz = TDCSResult.class,
            id = "DcsReleaseConf_Rsp")
    FinishCollaborateRsp,

    /**
     * 数据协作已结束
     */
    @Response(clz = BaseTypeString.class,
            id = "DcsReleaseConf_Ntf")
    CollaborateFinished,

    /** 查询数据协作配置*/
    @Request(method = "DCSGetConfInfoReq",
            owner = MethodOwner.DcsCtrl,
            rspSeq = "QueryConfigRsp")
    QueryConfig,

    /** 查询数据协作配置响应*/
    @Response(clz = TDCSCreateConfResult.class,
            id = "DcsGetConfInfo_Rsp")
    QueryConfigRsp,

    /**修改数据协作配置*/
    @Request(method = "DCSSetConfInfoReq",
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSConfInfo.class,
            rspSeq = {"ModifyConfigRsp", "ConfigModified"})
    ModifyConfig,

    /** 修改数据协作配置响应*/
    @Response(clz = DcsSetConfInfoRsp.class,
            id = "DcsSetConfInfo_Rsp")
    ModifyConfigRsp,

    /**
     * 数据协作配置已变更，如协作模式被修改。
     * */
    @Response(clz = TDCSConfInfo.class,
            id = "DcsUpdateConfInfo_Ntf")
    ConfigModified,


    // 数据协作权限控制相关

    /**
     * （主席）添加协作方
     */
    @Request(method = "DCSAddOperatorReq",
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSOperator.class,
            rspSeq = {"AddOperatorRsp",
                    "OperatorAdded"})
    AddOperator,

    /**
     * 添加协作方响应
     */
    @Response(clz = TDCSResult.class,
            id = "DcsAddOperator_Rsp")
    AddOperatorRsp,

    /**
     * （主席）删除协作方
     */
    @Request(method = "DCSDelOperatorReq",
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSOperator.class,
            rspSeq = {"DelOperatorRsp",
                    "OperatorDeleted"})
    DelOperator,

    /**
     * 删除协作方响应
     */
    @Response(clz = TDCSResult.class,
            id = "DcsDelOperator_Rsp")
    DelOperatorRsp,

    /**
     * （自己）申请作为协作方
     */
    @Request(method = "DCSApplyOperReq",
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = String.class, // 申请者的e164
            rspSeq = {"ApplyOperatorRsp", "ApplyOperatorRejected"},
            rspSeq2 = {"ApplyOperatorRsp", "OperatorAdded"},
            timeout = 30)
    ApplyOperator,

    /**
     * 申请协作方响应
     */
    @Response(clz = TDCSResult.class,
            id = "DcsApplyOper_Rsp")
    ApplyOperatorRsp,

    /**
     * （自己）取消作为协作方
     */
    @Request(method = "DCSCancelOperReq",
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = String.class, // 申请者的e164
            rspSeq = {"CancelOperatorRsp"})
    CancelOperator, // TODO 待调

    /**
     * 取消作为协作方响应
     */
    @Response(clz = TDCSResult.class,
            id = "DcsCancelOper_Rsp")
    CancelOperatorRsp,

    /**
     * （主席）拒绝成员申请作为协作方的请求
     */
    @Request(method = "DCSRejectOperatorCmd",
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSOperator.class)
    RejectApplyOperator,

    /**
     * 用户加入数据协作通知
     */
    @Response(clz = TDCSUserInfo.class,
            id = "DcsUserJoinConf_Ntf")
    UserJoined,

    /**
     * 成员（向主席）申请协作权通知
     */
    @Response(clz = TDCSUserInfo.class,
            id = "DcsUserApplyOper_Ntf")
    ApplyOperatorNtf,

    /**
     * 协作方被添加通知
     */
    @Response(clz = TDCSUserInfos.class,
            id = "DcsAddOperator_Ntf")
    OperatorAdded,
    /**
     * 协作方被删除通知
     */
    @Response(clz = TDCSUserInfos.class,
            id = "DcsDelOperator_Ntf")
    OperatorDeleted,
    /**
     * 申请协作权被拒绝通知
     */
    @Response(clz = TDCSUserInfo.class,
            id = "DcsRejectOper_Ntf")
    ApplyOperatorRejected,


    /**
     * 获取数据协作会议中的所有成员（包括协作方普通方）
     */
    @Request(method = "DCSGetUserListReq",
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = String.class, // 会议e164
            rspSeq = {"QueryAllMembersRsp"})
    QueryAllMembers,

    /**
     * 获取数据协作会议中的所有成员响应
     */
    @Response(clz = DcsGetUserListRsp.class,
            id = "DcsGetUserList_Rsp")
    QueryAllMembersRsp,


    // 数据协作画板相关

    /**
     * 新建画板
     */
    @Request(method = "DCSNewWhiteBoardReq",
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSNewWhiteBoard.class,
            rspSeq = {"NewBoardRsp",
                    "BoardCreated"})
    NewBoard,

    /**
     * 新建画板响应
     */
    @Response(clz = DcsNewWhiteBoardRsp.class,
            id = "DcsNewWhiteBoard_Rsp")
    NewBoardRsp,

    /**
     * 删除画板
     */
    @Request(method = "DCSDelWhiteBoardReq",
            owner = MethodOwner.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {String.class, // 当前会议e164
                    String.class}, // 画板Id
            rspSeq = {"DelBoardRsp",
                    "BoardDeleted"})
    DelBoard,

    /**
     * 删除画板响应
     */
    @Response(clz = TDCSBoardResult.class,
            id = "DcsDelWhiteBoard_Rsp")
    DelBoardRsp,

    /**
     * 删除所有画板
     */
    @Request(method = "DCSDelAllWhiteBoardReq",
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = String.class, // 当前会议e164
            rspSeq = {"DelAllBoardsRsp",
                    "AllBoardDeleted"})
    DelAllBoards,

    /**
     * 删除所有画板响应
     */
    @Response(clz = TDCSBoardResult.class,
            id = "DcsDelAllWhiteBoard_Rsp")
    DelAllBoardsRsp,

    /**
     * 切换画板
     */
    @Request(method = "DCSSwitchReq",
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSSwitchReq.class,
            rspSeq = {"SwitchBoardRsp",
                    "BoardSwitched"})
    SwitchBoard,

    /**
     * 切换画板响应
     */
    @Response(clz = DcsSwitchRsp.class,
            id = "DcsSwitch_Rsp")
    SwitchBoardRsp,

    /**
     * 查询当前画板
     */
    @Request(method = "DCSGetCurWhiteBoardReq",
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = String.class, // 会议e164
            rspSeq = {"QueryCurBoardRsp"})
    QueryCurBoard,

    /**
     * 查询当前画板响应
     */
    @Response(clz = DcsGetWhiteBoardRsp.class,
            id = "DcsGetCurWhiteBoard_Rsp")
    QueryCurBoardRsp,


    /**
     * 查询画板
     */
    @Request(method = "DCSGetWhiteBoardReq",
            owner = MethodOwner.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {String.class, // 会议e164
                    String.class}, // 画板id
            rspSeq = {"QueryBoardRsp"})
    QueryBoard,

    /**
     * 查询画板响应
     */
    @Response(clz = DcsGetWhiteBoardRsp.class,
            id = "DcsGetWhiteBoard_Rsp")
    QueryBoardRsp,

    /**
     * 查询所有画板
     */
    @Request(method = "DCSGetAllWhiteBoardReq", //参数：StringBuffer类型 e164
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = String.class,
            rspSeq = {"QueryAllBoardsRsp"})
    QueryAllBoards,

    /**
     * 查询所有白板响应
     */
    @Response(clz = DcsGetAllWhiteBoardRsp.class,
            id = "DcsGetAllWhiteBoard_Rsp")
    QueryAllBoardsRsp,

    /**
     * 添加子页
     */
    @Request(method = "DCSOperAddSubPageInfoCmd",
            owner = MethodOwner.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbAddSubPageInfo.class})
    AddSubPage, // TODO 待调


    /**
     * 当前画板通知。
     * 该通知仅用于加入数据协作时通知入会方当前画板信息，其他场景下不会上报。
     */
    @Response(clz = TDCSBoardInfo.class,
            id = "DcsCurrentWhiteBoard_Ntf")
    CurrentBoardNtf,

    /**
     * 画板已创建
     */
    @Response(id = "DcsNewWhiteBoard_Ntf", clz = TDCSBoardInfo.class)
    BoardCreated,

    /**
     * 画板已切换
     */
    @Response(id = "DcsSwitch_Ntf", clz = TDCSBoardInfo.class)
    BoardSwitched,

    /**
     * 画板已删除
     */
    @Response(id = "DcsDelWhiteBoard_Ntf", clz = TDCSDelWhiteBoardInfo.class)
    BoardDeleted,

    /**
     * 所有画板已删除
     */
    @Response(id = "DcsDelAllWhiteBoard_Ntf", clz = TDCSDelWhiteBoardInfo.class)
    AllBoardDeleted,


    // 数据协作图元操作

    /**
     * 画线
     */
    @Request(method = "DCSOperLineOperInfoCmd",
            owner = MethodOwner.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbLineOperInfo.class},
            rspSeq = "LineDrawn")
    DrawLine,

    /**
     * 画圆/椭圆
     */
    @Request(method = "DCSOperCircleOperInfoCmd",
            owner = MethodOwner.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbCircleOperInfo.class},
            rspSeq = "OvalDrawn")
    DrawOval,

    /**
     * 画矩形
     */
    @Request(method = "DCSOperRectangleOperInfoCmd",
            owner = MethodOwner.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbRectangleOperInfo.class},
            rspSeq = "RectDrawn")
    DrawRect,

    /**
     * 画路径
     */
    @Request(method = "DCSOperPencilOperInfoCmd",
            owner = MethodOwner.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbPencilOperInfo.class},
            rspSeq = "PathDrawn")
    DrawPath,

    /**
     * 插入图片
     * */
    @Request(method = "DCSOperInsertPicCmd",
            owner = MethodOwner.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbInsertPicOperInfo.class},
            rspSeq = "PicInserted")
    InsertPic,
    /**
     * 删除图片
     * */
    @Request(method = "DCSOperPitchPicDelCmd",
            owner = MethodOwner.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbDelPicOperInfo.class},
            rspSeq = "PicDeleted")
    DelPic,
    /**
     * 拖动/放缩图片
     * */
    @Request(method = "DCSOperPitchPicDragCmd",
            owner = MethodOwner.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbPitchPicOperInfo.class},
            rspSeq = "PicDragged")
    DragPic,
//    @Request(method = "DCSOperPitchPicZoomCmd",
//            owner = MethodOwner.DcsCtrl,
//            paras = {StringBuffer.class, StringBuffer.class},
//            userParas = {TDCSOperReq.class, TDCSWbPitchPicOperInfo.class})
//    DCZoomPic, // TODO 待调

    /**
     * 黑板擦擦除
     */
    @Request(method = "DCSOperReginEraseCmd",
            owner = MethodOwner.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbReginEraseOperInfo.class},
            rspSeq = "Erased")
    Erase,

    /**
     * 矩形擦除
     */
    @Request(method = "DCSOperEraseOperInfoCmd",
            owner = MethodOwner.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbEraseOperInfo.class},
            rspSeq = "RectErased")
    RectErase,

    /**
     * 清屏
     */
    @Request(method = "DCSOperClearScreenCmd",
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSOperReq.class,
            rspSeq = "ScreenCleared")
    ClearScreen,


    // 数据协作矩阵操作
    /**
     * 矩阵变换（放缩、位移等）
     */
    @Request(method = "DCSOperFullScreenCmd",
            owner = MethodOwner.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbDisPlayInfo.class},
            rspSeq = "Matrixed")
    Matrix,

    /**
     * 左旋转
     */
    @Request(method = "DCSOperRotateLeftCmd",
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSOperReq.class)
    RotateLeft, // TODO 待调

    /**
     * 右旋转
     */
    @Request(method = "DCSOperRotateRightCmd",
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSOperReq.class)
    RotateRight, // TODO 待调


    // 数据协作图元控制操作

    /**
     * 撤销
     */
    @Request(method = "DCSOperUndoCmd",
            owner = MethodOwner.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbTabPageIdInfo.class},
            rspSeq = "Undone")
    Undo,

    /**
     * 恢复（恢复被撤销的操作）
     */
    @Request(method = "DCSOperRedoCmd",
            owner = MethodOwner.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbTabPageIdInfo.class},
            rspSeq = "Redone")
    Redo,


    // 数据协作文件操作

    /**
     * 上传文件
     */
    @Request(method = "DCSUploadFileCmd",
            owner = MethodOwner.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {BaseTypeString.class, // 下载url。NOTE: 下层规定先将url包装到该类里面转成json然后传下，下层将json解析出来进而萃取出url。
                    TDCSFileInfo.class},
            rspSeq = {"UploadRsp", "PicDownloadable"},
            rspSeq2 = "PicDownloadable",
            timeout = 15)
    Upload,

    /**
     * 上传文件响应
     */
    @Response(clz = TDCSFileLoadResult.class,
            id = "DcsUploadFile_Ntf")
    UploadRsp,


    /**
     * 获取图片上传地址
     */
    @Request(method = "DCSUploadImageReq",
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSImageUrl.class,
            rspSeq = {"QueryPicUploadUrlRsp"})
    QueryPicUploadUrl,

    /**
     * 获取图片上传地址响应
     */
    @Response(clz = DcsUploadImageRsp.class,
            id = "DcsUploadImage_Rsp")
    QueryPicUploadUrlRsp,


    /**
     * 下载（图元、图片等）
     */
    @Request(method = "DCSDownloadFileReq",
            owner = MethodOwner.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {BaseTypeString.class,
                    TDCSFileInfo.class},
            rspSeq = {"DownloadRsp"},
            timeout = 15)
    Download,

    /**
     * 下载响应
     */
    @Response(clz = TDCSFileLoadResult.class,
            id = "DcsDownloadFile_Rsp")
    DownloadRsp,

    /**
     * 获取图片下载地址
     */
    @Request(method = "DCSDownloadImageReq",
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSImageUrl.class,
            rspSeq = {"QueryPicUrlRsp"})
    QueryPicUrl,

    /**
     * 获取下载图片地址响应
     */
    @Response(clz = DcsDownloadImageRsp.class,
            id = "DcsDownloadImage_Rsp")
    QueryPicUrlRsp,

    /**
     * 图片可下载通知
     */
    @Response(clz = TDCSImageUrl.class,
            id = "DcsDownloadImage_Ntf")
    PicDownloadable,


    // 数据协作图元操作通知

//    /**图元序列开始通知。
//     * 注：新加入数据协作会议后，服务器会将当前数据协作会议中已存在的图元序列同步到新加入的与会方。*/
//    @Deprecated
//    @Response(clz = MsgBeans.DCBoardId.class,
//					name ="DcsElementOperBegin_Ntf") //注意：组件没有处理此条消息
//    DCElementBeginNtf,

    /**
     * 直线已绘制
     */
    @Response(clz = DcsOperLineOperInfoNtf.class,
            id = "DcsOperLineOperInfo_Ntf")
    LineDrawn,

    /**
     * 圆/椭圆已绘制
     */
    @Response(clz = DcsOperCircleOperInfoNtf.class,
            id = "DcsOperCircleOperInfo_Ntf")
    OvalDrawn,

    /**
     * 矩形已绘制
     */
    @Response(clz = DcsOperRectangleOperInfoNtf.class,
            id = "DcsOperRectangleOperInfo_Ntf")
    RectDrawn,

    /**
     * 路径已绘制
     */
    @Response(clz = DcsOperPencilOperInfoNtf.class,
            id = "DcsOperPencilOperInfo_Ntf")
    PathDrawn,


    /**
     * 图片插入通知
     */
    @Response(clz = DcsOperInsertPicNtf.class,
            id = "DcsOperInsertPic_Ntf")
    PicInserted,

    /**
     * 图片拖动通知
     */
    @Response(clz = DcsOperPitchPicDragNtf.class,
            id = "DcsOperPitchPicDrag_Ntf")
    PicDragged,

    /**
     * 图片删除通知
     */
    @Response(clz = DcsOperPitchPicDelNtf.class,
            id = "DcsOperPitchPicDel_Ntf")
    PicDeleted,

    /**
     * 黑板擦擦除通知
     */
    @Response(clz = DcsOperReginEraseNtf.class,
            id = "DcsOperReginErase_Ntf")
    Erased,

    /**
     * 矩形擦除通知
     */
    @Response(clz = DcsOperEraseOperInfoNtf.class,
            id = "DcsOperEraseOperInfo_Ntf")
    RectErased,

    /**
     * 全屏matrix操作通知（缩放、移动、旋转）
     */
    @Response(clz = DcsOperFullScreenNtf.class,
            id = "DcsOperFullScreen_Ntf")
    Matrixed,

    /**
     * 撤销操作通知
     */
    @Response(clz = DcsOperUndoNtf.class,
            id = "DcsOperUndo_Ntf")
    Undone,

    /**
     * 恢复（恢复被撤销的操作）通知
     */
    @Response(clz = DcsOperRedoNtf.class,
            id = "DcsOperRedo_Ntf")
    Redone,

    /**
     * 清屏通知
     */
    @Response(clz = TDCSOperContent.class,
            id = "DcsOperClearScreen_Ntf")
    ScreenCleared;

//    /**
//     * 图元序列结束通知
//     */ //NOTE: 下层“开始——结束”通知不可靠，时序数量均有问题，故废弃不用。
//    @Response(clz = TDcsCacheElementParseResult.class,
//            id = "DcsElementOperFinal_Ntf")
//    DCElementEndNtf,


    //<<<<<<<<<<<<<<<<<< 数据协作


    private static class MethodOwner {
        private static final String DcsCtrl = "com.kedacom.kdv.mt.mtapi.DcsCtrl";
        private static final String ConfigCtrl = "com.kedacom.kdv.mt.mtapi.ConfigCtrl";
    }
}
