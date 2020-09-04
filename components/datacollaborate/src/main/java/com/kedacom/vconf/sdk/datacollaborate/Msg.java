package com.kedacom.vconf.sdk.datacollaborate;


import com.kedacom.vconf.sdk.annotation.Module;
import com.kedacom.vconf.sdk.annotation.Notification;
import com.kedacom.vconf.sdk.annotation.Request;
import com.kedacom.vconf.sdk.annotation.Response;
import com.kedacom.vconf.sdk.common.type.BaseTypeString;
import com.kedacom.vconf.sdk.datacollaborate.bean.transfer.*;

import static com.kedacom.vconf.sdk.annotation.Request.*;


/**
 * Created by Sissi on 2018/9/3.
 * 数据协作消息定义。
 */

@Module(
        name = "DC" // abbreviation for DataCollaborate
)
enum Msg {
    // 数据协作基础

    /**
     * 获取数据协作服务器地址
     */
    @Request(name = "GetDCSCfg",
            owner = ConfigCtrl,
            paras = StringBuffer.class,
            userParas = TDCSSvrAddr.class,
            outputParaIndex = LastIndex
    )
    GetServerAddr,

    /**
     * 获取数据协作相关状态
     */
    @Request(name = "GetDCSServerStateRt",
            owner = ConfigCtrl,
            paras = StringBuffer.class,
            userParas = TDCSSrvState.class,
            outputParaIndex = LastIndex
    )
    GetState,

    /**
     * 登录链路状态变化
     */
    @Response(clz = TDCSConnectResult.class,
            name = "DcsLoginResult_Ntf")
    LoginLinkStateChanged,

    /**
     * 登录数据协作服务器
     */
    @Request(name = "LoginSrvReq",
            owner = DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSRegInfo.class,
            rspSeq = {"LoginLinkStateChanged",
                    "LoginRsp"})
    Login,

    @Response(clz = TDCSResult.class,
            name = "DcsLoginSrv_Rsp")
    LoginRsp,

    /**
     * 注销数据协作服务器
     */
    @Request(name = "DCSLogoutReq",
            owner = DcsCtrl,
            rspSeq = {"LogoutRsp",
                    "LoginLinkStateChanged"
            })
    Logout,

    @Response(clz = TDCSResult.class,
            name = "DcsLogout_Rsp")
    LogoutRsp,


    /**
     * 查询数据协作地址
     */
    @Request(name = "DCSGetConfAddrReq",
            owner = DcsCtrl,
            paras = StringBuffer.class,
            userParas = String.class, // 数据协作所在会议e164号
            rspSeq = "QueryAddrRsp"
    )
    QueryAddr,

    @Response(clz = DcsGetConfAddrRsp.class,
            name = "DcsGetConfAddr_Rsp")
    QueryAddrRsp,

    /**
     * 数据协作链路状态变化
     */
    @Notification
    @Response(clz = TDCSConnectResult.class,
            name = "DcsConfResult_Ntf")
    LinkStateChanged,

    /**
     * 开启数据协作
     */
    @Request(name = "DCSCreateConfReq",
            owner = DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSCreateConf.class,
            rspSeq = {"LinkStateChanged",  // NOTE: 若该响应bSuccess字段为false则不会收到DCConfCreated。
                    "CollaborateStarted"},
            rspSeq2 = "CollaborateStarted")
    StartCollaborate,

    /**
     * 数据协作已开启
     */
    @Notification
    @Response(clz = TDCSCreateConfResult.class,
            name = "DcsCreateConf_Rsp")
    CollaborateStarted,


    /**
     * 退出数据协作。
     * 注：仅自己退出，协作仍存在，不影响其他人继续
     */
    @Request(name = "DCSQuitConfReq",
            owner = DcsCtrl,
            paras = {StringBuffer.class, int.class},
            userParas = {String.class, // 会议e164
                    Integer.class // 是否同时退出会议。0表示退出协作的同时退出会议，1表示仅退出协作。
            },
            rspSeq = {"QuitCollaborateRsp",
                    "LinkStateChanged"})
    QuitCollaborate,

    @Response(clz = TDCSResult.class,
            name = "DcsQuitConf_Rsp")
    QuitCollaborateRsp,

    /**
     * 结束数据协作
     */
    @Request(name = "DCSReleaseConfReq",
            owner = DcsCtrl,
            paras = StringBuffer.class,
            userParas = String.class, // 会议e164
            rspSeq = {"FinishCollaborateRsp", "LinkStateChanged"},
            rspSeq2 = {"FinishCollaborateRsp", "CollaborateFinished", "LinkStateChanged"})
    FinishCollaborate,

    @Response(clz = TDCSResult.class,
            name = "DcsReleaseConf_Rsp")
    FinishCollaborateRsp,

    /**
     * 数据协作已结束
     */
    @Notification
    @Response(clz = BaseTypeString.class,
            name = "DcsReleaseConf_Ntf")
    CollaborateFinished,

    /**
     * 查询数据协作配置
     */
    @Request(name = "DCSGetConfInfoReq",
            owner = DcsCtrl,
            rspSeq = "QueryConfigRsp")
    QueryConfig,

    @Response(clz = TDCSCreateConfResult.class,
            name = "DcsGetConfInfo_Rsp")
    QueryConfigRsp,

    /**
     * 修改数据协作配置
     */
    @Request(name = "DCSSetConfInfoReq",
            owner = DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSConfInfo.class,
            rspSeq = {"ModifyConfigRsp", "ConfigModified"})
    ModifyConfig,

    @Response(clz = DcsSetConfInfoRsp.class,
            name = "DcsSetConfInfo_Rsp")
    ModifyConfigRsp,

    /**
     * 数据协作配置已变更，如协作模式被修改。
     */
    @Notification
    @Response(clz = TDCSConfInfo.class,
            name = "DcsUpdateConfInfo_Ntf")
    ConfigModified,


    // 数据协作权限控制相关

    /**
     * （主席）添加协作方
     */
    @Request(name = "DCSAddOperatorReq",
            owner = DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSOperator.class,
            rspSeq = {"AddOperatorRsp",
                    "OperatorAdded"})
    AddOperator,

    @Response(clz = TDCSResult.class,
            name = "DcsAddOperator_Rsp")
    AddOperatorRsp,

    /**
     * （主席）删除协作方
     */
    @Request(name = "DCSDelOperatorReq",
            owner = DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSOperator.class,
            rspSeq = {"DelOperatorRsp",
                    "OperatorDeleted"})
    DelOperator,

    @Response(clz = TDCSResult.class,
            name = "DcsDelOperator_Rsp")
    DelOperatorRsp,

    /**
     * （自己）申请作为协作方
     */
    @Request(name = "DCSApplyOperReq",
            owner = DcsCtrl,
            paras = StringBuffer.class,
            userParas = String.class, // 申请者的e164
            rspSeq = {"ApplyOperatorRsp", "ApplyOperatorRejected"},
            rspSeq2 = {"ApplyOperatorRsp", "OperatorAdded"},
            timeout = 30)
    ApplyOperator,

    @Response(clz = TDCSResult.class,
            name = "DcsApplyOper_Rsp")
    ApplyOperatorRsp,

    /**
     * （自己）取消作为协作方
     */
    @Request(name = "DCSCancelOperReq",
            owner = DcsCtrl,
            paras = StringBuffer.class,
            userParas = String.class, // 申请者的e164
            rspSeq = {"CancelOperatorRsp"})
    CancelOperator, // TODO 待调

    @Response(clz = TDCSResult.class,
            name = "DcsCancelOper_Rsp")
    CancelOperatorRsp,

    /**
     * （主席）拒绝协作权申请
     */
    @Request(name = "DCSRejectOperatorCmd",
            owner = DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSOperator.class)
    RejectApplyOperator,

    /**
     * 有用户加入数据协作
     */
    @Notification(clz = TDCSUserInfo.class,
            name = "DcsUserJoinConf_Ntf")
    UserJoined,

    /**
     * 协作权申请通知
     */
    @Notification(clz = TDCSUserInfo.class,
            name = "DcsUserApplyOper_Ntf")
    ApplyOperatorNtf,

    /**
     * 协作方被添加
     */
    @Notification
    @Response(clz = TDCSUserInfos.class,
            name = "DcsAddOperator_Ntf")
    OperatorAdded,
    /**
     * 协作方被删除
     */
    @Notification
    @Response(clz = TDCSUserInfos.class,
            name = "DcsDelOperator_Ntf")
    OperatorDeleted,
    /**
     * 申请协作权被拒绝
     */
    @Response(clz = TDCSUserInfo.class,
            name = "DcsRejectOper_Ntf")
    ApplyOperatorRejected,


    /**
     * 获取数据协作会议中的所有成员（包括协作方普通方）
     */
    @Request(name = "DCSGetUserListReq",
            owner = DcsCtrl,
            paras = StringBuffer.class,
            userParas = String.class, // 会议e164
            rspSeq = {"QueryAllMembersRsp"})
    QueryAllMembers,

    @Response(clz = DcsGetUserListRsp.class,
            name = "DcsGetUserList_Rsp")
    QueryAllMembersRsp,


    // 数据协作画板相关

    /**
     * 新建画板
     */
    @Request(name = "DCSNewWhiteBoardReq",
            owner = DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSNewWhiteBoard.class,
            rspSeq = {"NewBoardRsp",
                    "BoardCreated"})
    NewBoard,

    @Response(clz = DcsNewWhiteBoardRsp.class,
            name = "DcsNewWhiteBoard_Rsp")
    NewBoardRsp,

    /**
     * 删除画板
     */
    @Request(name = "DCSDelWhiteBoardReq",
            owner = DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {String.class, // 当前会议e164
                    String.class}, // 画板Id
            rspSeq = {"DelBoardRsp",
                    "BoardDeleted"})
    DelBoard,

    @Response(clz = TDCSBoardResult.class,
            name = "DcsDelWhiteBoard_Rsp")
    DelBoardRsp,

    /**
     * 删除所有画板
     */
    @Request(name = "DCSDelAllWhiteBoardReq",
            owner = DcsCtrl,
            paras = StringBuffer.class,
            userParas = String.class, // 当前会议e164
            rspSeq = {"DelAllBoardsRsp",
                    "AllBoardDeleted"})
    DelAllBoards,

    @Response(clz = TDCSBoardResult.class,
            name = "DcsDelAllWhiteBoard_Rsp")
    DelAllBoardsRsp,

    /**
     * 切换画板
     */
    @Request(name = "DCSSwitchReq",
            owner = DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSSwitchReq.class,
            rspSeq = {"SwitchBoardRsp",
                    "BoardSwitched"})
    SwitchBoard,

    @Response(clz = DcsSwitchRsp.class,
            name = "DcsSwitch_Rsp")
    SwitchBoardRsp,

    /**
     * 查询当前画板
     */
    @Request(name = "DCSGetCurWhiteBoardReq",
            owner = DcsCtrl,
            paras = StringBuffer.class,
            userParas = String.class, // 会议e164
            rspSeq = {"QueryCurBoardRsp"})
    QueryCurBoard,

    @Response(clz = DcsGetWhiteBoardRsp.class,
            name = "DcsGetCurWhiteBoard_Rsp")
    QueryCurBoardRsp,


    /**
     * 查询画板
     */
    @Request(name = "DCSGetWhiteBoardReq",
            owner = DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {String.class, // 会议e164
                    String.class}, // 画板id
            rspSeq = {"QueryBoardRsp"})
    QueryBoard,

    @Response(clz = DcsGetWhiteBoardRsp.class,
            name = "DcsGetWhiteBoard_Rsp")
    QueryBoardRsp,

    /**
     * 查询所有画板
     */
    @Request(name = "DCSGetAllWhiteBoardReq", //参数：StringBuffer类型 e164
            owner = DcsCtrl,
            paras = StringBuffer.class,
            userParas = String.class,
            rspSeq = {"QueryAllBoardsRsp"})
    QueryAllBoards,

    @Response(clz = DcsGetAllWhiteBoardRsp.class,
            name = "DcsGetAllWhiteBoard_Rsp")
    QueryAllBoardsRsp,

    /**
     * 添加子页
     */
    @Request(name = "DCSOperAddSubPageInfoCmd",
            owner = DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbAddSubPageInfo.class})
    AddSubPage, // TODO 待调


    /**
     * 当前画板
     * 该通知仅用于加入数据协作时通知入会方当前画板信息，其他场景下不会上报。
     */
    @Notification(clz = TDCSBoardInfo.class, name = "DcsCurrentWhiteBoard_Ntf")
    CurrentBoardNtf,

    /**
     * 画板已创建
     */
    @Notification
    @Response(name = "DcsNewWhiteBoard_Ntf", clz = TDCSBoardInfo.class)
    BoardCreated,

    /**
     * 画板已切换
     */
    @Notification
    @Response(name = "DcsSwitch_Ntf", clz = TDCSBoardInfo.class)
    BoardSwitched,

    /**
     * 画板已删除
     */
    @Notification
    @Response(name = "DcsDelWhiteBoard_Ntf", clz = TDCSDelWhiteBoardInfo.class)
    BoardDeleted,

    /**
     * 所有画板已删除
     */
    @Notification
    @Response(name = "DcsDelAllWhiteBoard_Ntf", clz = TDCSDelWhiteBoardInfo.class)
    AllBoardDeleted,


    // 数据协作图元操作

    /**
     * 画线
     */
    @Request(name = "DCSOperLineOperInfoCmd",
            owner = DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbLineOperInfo.class},
            rspSeq = "LineDrawn")
    DrawLine,

    /**
     * 画圆/椭圆
     */
    @Request(name = "DCSOperCircleOperInfoCmd",
            owner = DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbCircleOperInfo.class},
            rspSeq = "OvalDrawn")
    DrawOval,

    /**
     * 画矩形
     */
    @Request(name = "DCSOperRectangleOperInfoCmd",
            owner = DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbRectangleOperInfo.class},
            rspSeq = "RectDrawn")
    DrawRect,

    /**
     * 画路径
     */
    @Request(name = "DCSOperPencilOperInfoCmd",
            owner = DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbPencilOperInfo.class},
            rspSeq = "PathDrawn")
    DrawPath,

    /**
     * 插入图片
     */
    @Request(name = "DCSOperInsertPicCmd",
            owner = DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbInsertPicOperInfo.class},
            rspSeq = "PicInserted")
    InsertPic,
    /**
     * 删除图片
     */
    @Request(name = "DCSOperPitchPicDelCmd",
            owner = DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbDelPicOperInfo.class},
            rspSeq = "PicDeleted")
    DelPic,
    /**
     * 拖动/放缩图片
     */
    @Request(name = "DCSOperPitchPicDragCmd",
            owner = DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbPitchPicOperInfo.class},
            rspSeq = "PicDragged")
    DragPic,
//    @Request(method = "DCSOperPitchPicZoomCmd",
//            owner = Atlas.DcsCtrl,
//            paras = {StringBuffer.class, StringBuffer.class},
//            userParas = {TDCSOperReq.class, TDCSWbPitchPicOperInfo.class})
//    DCZoomPic, // TODO 待调

    /**
     * 黑板擦擦除
     */
    @Request(name = "DCSOperReginEraseCmd",
            owner = DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbReginEraseOperInfo.class},
            rspSeq = "Erased")
    Erase,

    /**
     * 矩形擦除
     */
    @Request(name = "DCSOperEraseOperInfoCmd",
            owner = DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbEraseOperInfo.class},
            rspSeq = "RectErased")
    RectErase,

    /**
     * 清屏
     */
    @Request(name = "DCSOperClearScreenCmd",
            owner = DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSOperReq.class,
            rspSeq = "ScreenCleared")
    ClearScreen,


    // 数据协作矩阵操作
    /**
     * 矩阵变换（放缩、位移等）
     */
    @Request(name = "DCSOperFullScreenCmd",
            owner = DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbDisPlayInfo.class},
            rspSeq = "Matrixed")
    Matrix,

    /**
     * 左旋转
     */
    @Request(name = "DCSOperRotateLeftCmd",
            owner = DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSOperReq.class)
    RotateLeft, // TODO 待调

    /**
     * 右旋转
     */
    @Request(name = "DCSOperRotateRightCmd",
            owner = DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSOperReq.class)
    RotateRight, // TODO 待调


    // 数据协作图元控制操作

    /**
     * 撤销
     */
    @Request(name = "DCSOperUndoCmd",
            owner = DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbTabPageIdInfo.class},
            rspSeq = "Undone")
    Undo,

    /**
     * 恢复（恢复被撤销的操作）
     */
    @Request(name = "DCSOperRedoCmd",
            owner = DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbTabPageIdInfo.class},
            rspSeq = "Redone")
    Redo,


    // 数据协作文件操作

    /**
     * 上传文件
     */
    @Request(name = "DCSUploadFileCmd",
            owner = DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {BaseTypeString.class, // 下载url。NOTE: 下层规定先将url包装到该类里面转成json然后传下，下层将json解析出来进而萃取出url。
                    TDCSFileInfo.class},
            rspSeq = {"UploadRsp", "PicDownloadable"},
            rspSeq2 = "PicDownloadable",
            timeout = 15)
    Upload,

    @Response(clz = TDCSFileLoadResult.class,
            name = "DcsUploadFile_Ntf")
    UploadRsp,


    /**
     * 获取图片上传地址
     */
    @Request(name = "DCSUploadImageReq",
            owner = DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSImageUrl.class,
            rspSeq = {"QueryPicUploadUrlRsp"})
    QueryPicUploadUrl,

    @Response(clz = DcsUploadImageRsp.class,
            name = "DcsUploadImage_Rsp")
    QueryPicUploadUrlRsp,


    /**
     * 下载（图元、图片等）
     */
    @Request(name = "DCSDownloadFileReq",
            owner = DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {BaseTypeString.class,
                    TDCSFileInfo.class},
            rspSeq = {"DownloadRsp"},
            timeout = 15)
    Download,

    @Response(clz = TDCSFileLoadResult.class,
            name = "DcsDownloadFile_Rsp")
    DownloadRsp,

    /**
     * 获取图片下载地址
     */
    @Request(name = "DCSDownloadImageReq",
            owner = DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSImageUrl.class,
            rspSeq = {"QueryPicUrlRsp"})
    QueryPicUrl,

    @Response(clz = DcsDownloadImageRsp.class,
            name = "DcsDownloadImage_Rsp")
    QueryPicUrlRsp,

    /**
     * 图片可下载
     */
    @Notification
    @Response(clz = TDCSImageUrl.class,
            name = "DcsDownloadImage_Ntf")
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
    @Notification
    @Response(clz = DcsOperLineOperInfoNtf.class,
            name = "DcsOperLineOperInfo_Ntf")
    LineDrawn,

    /**
     * 圆/椭圆已绘制
     */
    @Notification
    @Response(clz = DcsOperCircleOperInfoNtf.class,
            name = "DcsOperCircleOperInfo_Ntf")
    OvalDrawn,

    /**
     * 矩形已绘制
     */
    @Notification
    @Response(clz = DcsOperRectangleOperInfoNtf.class,
            name = "DcsOperRectangleOperInfo_Ntf")
    RectDrawn,

    /**
     * 路径已绘制
     */
    @Notification
    @Response(clz = DcsOperPencilOperInfoNtf.class,
            name = "DcsOperPencilOperInfo_Ntf")
    PathDrawn,


    /**
     * 图片已插入
     */
    @Notification
    @Response(clz = DcsOperInsertPicNtf.class,
            name = "DcsOperInsertPic_Ntf")
    PicInserted,

    /**
     * 图片已拖动
     */
    @Notification
    @Response(clz = DcsOperPitchPicDragNtf.class,
            name = "DcsOperPitchPicDrag_Ntf")
    PicDragged,

    /**
     * 图片已删除
     */
    @Notification
    @Response(clz = DcsOperPitchPicDelNtf.class,
            name = "DcsOperPitchPicDel_Ntf")
    PicDeleted,

    /**
     * 已黑板擦擦除
     */
    @Notification
    @Response(clz = DcsOperReginEraseNtf.class,
            name = "DcsOperReginErase_Ntf")
    Erased,

    /**
     * 已矩形擦除
     */
    @Notification
    @Response(clz = DcsOperEraseOperInfoNtf.class,
            name = "DcsOperEraseOperInfo_Ntf")
    RectErased,

    /**
     * 已全屏matrix（缩放、移动、旋转）
     */
    @Notification
    @Response(clz = DcsOperFullScreenNtf.class,
            name = "DcsOperFullScreen_Ntf")
    Matrixed,

    /**
     * 已撤销
     */
    @Notification
    @Response(clz = DcsOperUndoNtf.class,
            name = "DcsOperUndo_Ntf")
    Undone,

    /**
     * 已恢复（被撤销的操作）
     */
    @Notification
    @Response(clz = DcsOperRedoNtf.class,
            name = "DcsOperRedo_Ntf")
    Redone,

    /**
     * 已清屏
     */
    @Notification
    @Response(clz = TDCSOperContent.class,
            name = "DcsOperClearScreen_Ntf")
    ScreenCleared;

//    /**
//     * 图元序列结束通知
//     */ //NOTE: 下层“开始——结束”通知不可靠，时序数量均有问题，故废弃不用。
//    @Response(clz = TDcsCacheElementParseResult.class,
//            id = "DcsElementOperFinal_Ntf")
//    DCElementEndNtf,

}
