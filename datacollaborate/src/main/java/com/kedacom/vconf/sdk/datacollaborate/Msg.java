package com.kedacom.vconf.sdk.datacollaborate;


import com.kedacom.vconf.sdk.amulet.Atlas;
import com.kedacom.vconf.sdk.annotation.Module;
import com.kedacom.vconf.sdk.annotation.Notification;
import com.kedacom.vconf.sdk.annotation.Request;
import com.kedacom.vconf.sdk.annotation.Response;
import com.kedacom.vconf.sdk.common.type.BaseTypeString;
import com.kedacom.vconf.sdk.datacollaborate.bean.transfer.*;


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
            owner = Atlas.ConfigCtrl,
            paras = StringBuffer.class,
            userParas = TDCSSvrAddr.class,
            isGet = true)
    GetServerAddr,

    /**
     * 获取数据协作相关状态
     */
    @Request(name = "GetDCSServerStateRt",
            owner = Atlas.ConfigCtrl,
            paras = StringBuffer.class,
            userParas = TDCSSrvState.class,
            isGet = true)
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
            owner = Atlas.DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSRegInfo.class,
            rspSeq = {"LoginLinkStateChanged",
                    "LoginRsp"})
    Login,

    /**
     * 登录数据协作服务器响应
     */
    @Response(clz = TDCSResult.class,
            name = "DcsLoginSrv_Rsp")
    LoginRsp,

    /**
     * 注销数据协作服务器
     */
    @Request(name = "DCSLogoutReq",
            owner = Atlas.DcsCtrl,
            rspSeq = {"LogoutRsp",
                    "LoginLinkStateChanged"
            })
    Logout,

    /**
     * 注销数据协作服务器响应
     */
    @Response(clz = TDCSResult.class,
            name = "DcsLogout_Rsp")
    LogoutRsp,


    /**
     * 查询数据协作地址
     */
    @Request(name = "DCSGetConfAddrReq",
            owner = Atlas.DcsCtrl,
            paras = StringBuffer.class,
            userParas = String.class, // 数据协作所在会议e164号
            rspSeq = "QueryAddrRsp"
    )
    QueryAddr,

    /**
     * 查询数据协作地址响应
     */
    @Response(clz = DcsGetConfAddrRsp.class,
            name = "DcsGetConfAddr_Rsp")
    QueryAddrRsp,

    /**
     * 数据协作链路状态变化
     */
    @Response(clz = TDCSConnectResult.class,
            name = "DcsConfResult_Ntf")
    @Notification(clz = TDCSConnectResult.class,
            name = "DcsConfResult_Ntf")
    LinkStateChanged,

    /**
     * 开启数据协作
     */
    @Request(name = "DCSCreateConfReq",
            owner = Atlas.DcsCtrl,
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
            name = "DcsCreateConf_Rsp")
    @Notification(clz = TDCSCreateConfResult.class,
            name = "DcsCreateConf_Rsp")
    CollaborateStarted,


    /**
     * 退出数据协作。
     * 注：仅自己退出，协作仍存在，不影响其他人继续
     */
    @Request(name = "DCSQuitConfReq",
            owner = Atlas.DcsCtrl,
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
            name = "DcsQuitConf_Rsp")
    QuitCollaborateRsp,

    /**
     * 结束数据协作
     */
    @Request(name = "DCSReleaseConfReq",
            owner = Atlas.DcsCtrl,
            paras = StringBuffer.class,
            userParas = String.class, // 会议e164
            rspSeq = {"FinishCollaborateRsp", "LinkStateChanged"},
            rspSeq2 = {"FinishCollaborateRsp", "CollaborateFinished", "LinkStateChanged"})
    FinishCollaborate,

    /**
     * 结束数据协作响应
     */
    @Response(clz = TDCSResult.class,
            name = "DcsReleaseConf_Rsp")
    FinishCollaborateRsp,

    /**
     * 数据协作已结束
     */
    @Response(clz = BaseTypeString.class,
            name = "DcsReleaseConf_Ntf")
    @Notification(clz = BaseTypeString.class,
            name = "DcsReleaseConf_Ntf")
    CollaborateFinished,

    /**
     * 查询数据协作配置
     */
    @Request(name = "DCSGetConfInfoReq",
            owner = Atlas.DcsCtrl,
            rspSeq = "QueryConfigRsp")
    QueryConfig,

    /**
     * 查询数据协作配置响应
     */
    @Response(clz = TDCSCreateConfResult.class,
            name = "DcsGetConfInfo_Rsp")
    QueryConfigRsp,

    /**
     * 修改数据协作配置
     */
    @Request(name = "DCSSetConfInfoReq",
            owner = Atlas.DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSConfInfo.class,
            rspSeq = {"ModifyConfigRsp", "ConfigModified"})
    ModifyConfig,

    /**
     * 修改数据协作配置响应
     */
    @Response(clz = DcsSetConfInfoRsp.class,
            name = "DcsSetConfInfo_Rsp")
    ModifyConfigRsp,

    /**
     * 数据协作配置已变更，如协作模式被修改。
     */
    @Response(clz = TDCSConfInfo.class,
            name = "DcsUpdateConfInfo_Ntf")
    @Notification(clz = TDCSConfInfo.class,
            name = "DcsUpdateConfInfo_Ntf")
    ConfigModified,


    // 数据协作权限控制相关

    /**
     * （主席）添加协作方
     */
    @Request(name = "DCSAddOperatorReq",
            owner = Atlas.DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSOperator.class,
            rspSeq = {"AddOperatorRsp",
                    "OperatorAdded"})
    AddOperator,

    /**
     * 添加协作方响应
     */
    @Response(clz = TDCSResult.class,
            name = "DcsAddOperator_Rsp")
    AddOperatorRsp,

    /**
     * （主席）删除协作方
     */
    @Request(name = "DCSDelOperatorReq",
            owner = Atlas.DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSOperator.class,
            rspSeq = {"DelOperatorRsp",
                    "OperatorDeleted"})
    DelOperator,

    /**
     * 删除协作方响应
     */
    @Response(clz = TDCSResult.class,
            name = "DcsDelOperator_Rsp")
    DelOperatorRsp,

    /**
     * （自己）申请作为协作方
     */
    @Request(name = "DCSApplyOperReq",
            owner = Atlas.DcsCtrl,
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
            name = "DcsApplyOper_Rsp")
    ApplyOperatorRsp,

    /**
     * （自己）取消作为协作方
     */
    @Request(name = "DCSCancelOperReq",
            owner = Atlas.DcsCtrl,
            paras = StringBuffer.class,
            userParas = String.class, // 申请者的e164
            rspSeq = {"CancelOperatorRsp"})
    CancelOperator, // TODO 待调

    /**
     * 取消作为协作方响应
     */
    @Response(clz = TDCSResult.class,
            name = "DcsCancelOper_Rsp")
    CancelOperatorRsp,

    /**
     * （主席）拒绝成员申请作为协作方的请求
     */
    @Request(name = "DCSRejectOperatorCmd",
            owner = Atlas.DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSOperator.class)
    RejectApplyOperator,

    /**
     * 用户加入数据协作通知
     */
    @Notification(clz = TDCSUserInfo.class,
            name = "DcsUserJoinConf_Ntf")
    UserJoined,

    /**
     * 成员（向主席）申请协作权通知
     */
    @Notification(clz = TDCSUserInfo.class,
            name = "DcsUserApplyOper_Ntf")
    ApplyOperatorNtf,

    /**
     * 协作方被添加通知
     */
    @Response(clz = TDCSUserInfos.class,
            name = "DcsAddOperator_Ntf")
    @Notification(clz = TDCSUserInfos.class,
            name = "DcsAddOperator_Ntf")
    OperatorAdded,
    /**
     * 协作方被删除通知
     */
    @Response(clz = TDCSUserInfos.class,
            name = "DcsDelOperator_Ntf")
    @Notification(clz = TDCSUserInfos.class,
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
            owner = Atlas.DcsCtrl,
            paras = StringBuffer.class,
            userParas = String.class, // 会议e164
            rspSeq = {"QueryAllMembersRsp"})
    QueryAllMembers,

    /**
     * 获取数据协作会议中的所有成员响应
     */
    @Response(clz = DcsGetUserListRsp.class,
            name = "DcsGetUserList_Rsp")
    QueryAllMembersRsp,


    // 数据协作画板相关

    /**
     * 新建画板
     */
    @Request(name = "DCSNewWhiteBoardReq",
            owner = Atlas.DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSNewWhiteBoard.class,
            rspSeq = {"NewBoardRsp",
                    "BoardCreated"})
    NewBoard,

    /**
     * 新建画板响应
     */
    @Response(clz = DcsNewWhiteBoardRsp.class,
            name = "DcsNewWhiteBoard_Rsp")
    NewBoardRsp,

    /**
     * 删除画板
     */
    @Request(name = "DCSDelWhiteBoardReq",
            owner = Atlas.DcsCtrl,
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
            name = "DcsDelWhiteBoard_Rsp")
    DelBoardRsp,

    /**
     * 删除所有画板
     */
    @Request(name = "DCSDelAllWhiteBoardReq",
            owner = Atlas.DcsCtrl,
            paras = StringBuffer.class,
            userParas = String.class, // 当前会议e164
            rspSeq = {"DelAllBoardsRsp",
                    "AllBoardDeleted"})
    DelAllBoards,

    /**
     * 删除所有画板响应
     */
    @Response(clz = TDCSBoardResult.class,
            name = "DcsDelAllWhiteBoard_Rsp")
    DelAllBoardsRsp,

    /**
     * 切换画板
     */
    @Request(name = "DCSSwitchReq",
            owner = Atlas.DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSSwitchReq.class,
            rspSeq = {"SwitchBoardRsp",
                    "BoardSwitched"})
    SwitchBoard,

    /**
     * 切换画板响应
     */
    @Response(clz = DcsSwitchRsp.class,
            name = "DcsSwitch_Rsp")
    SwitchBoardRsp,

    /**
     * 查询当前画板
     */
    @Request(name = "DCSGetCurWhiteBoardReq",
            owner = Atlas.DcsCtrl,
            paras = StringBuffer.class,
            userParas = String.class, // 会议e164
            rspSeq = {"QueryCurBoardRsp"})
    QueryCurBoard,

    /**
     * 查询当前画板响应
     */
    @Response(clz = DcsGetWhiteBoardRsp.class,
            name = "DcsGetCurWhiteBoard_Rsp")
    QueryCurBoardRsp,


    /**
     * 查询画板
     */
    @Request(name = "DCSGetWhiteBoardReq",
            owner = Atlas.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {String.class, // 会议e164
                    String.class}, // 画板id
            rspSeq = {"QueryBoardRsp"})
    QueryBoard,

    /**
     * 查询画板响应
     */
    @Response(clz = DcsGetWhiteBoardRsp.class,
            name = "DcsGetWhiteBoard_Rsp")
    QueryBoardRsp,

    /**
     * 查询所有画板
     */
    @Request(name = "DCSGetAllWhiteBoardReq", //参数：StringBuffer类型 e164
            owner = Atlas.DcsCtrl,
            paras = StringBuffer.class,
            userParas = String.class,
            rspSeq = {"QueryAllBoardsRsp"})
    QueryAllBoards,

    /**
     * 查询所有白板响应
     */
    @Response(clz = DcsGetAllWhiteBoardRsp.class,
            name = "DcsGetAllWhiteBoard_Rsp")
    QueryAllBoardsRsp,

    /**
     * 添加子页
     */
    @Request(name = "DCSOperAddSubPageInfoCmd",
            owner = Atlas.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbAddSubPageInfo.class})
    AddSubPage, // TODO 待调


    /**
     * 当前画板通知。
     * 该通知仅用于加入数据协作时通知入会方当前画板信息，其他场景下不会上报。
     */
    @Response(clz = TDCSBoardInfo.class,
            name = "DcsCurrentWhiteBoard_Ntf")
    @Notification(clz = TDCSBoardInfo.class,
            name = "DcsCurrentWhiteBoard_Ntf")
    CurrentBoardNtf,

    /**
     * 画板已创建
     */
    @Response(name = "DcsNewWhiteBoard_Ntf", clz = TDCSBoardInfo.class)
    @Notification(name = "DcsNewWhiteBoard_Ntf", clz = TDCSBoardInfo.class)
    BoardCreated,

    /**
     * 画板已切换
     */
    @Response(name = "DcsSwitch_Ntf", clz = TDCSBoardInfo.class)
    @Notification(name = "DcsSwitch_Ntf", clz = TDCSBoardInfo.class)
    BoardSwitched,

    /**
     * 画板已删除
     */
    @Response(name = "DcsDelWhiteBoard_Ntf", clz = TDCSDelWhiteBoardInfo.class)
    @Notification(name = "DcsDelWhiteBoard_Ntf", clz = TDCSDelWhiteBoardInfo.class)
    BoardDeleted,

    /**
     * 所有画板已删除
     */
    @Response(name = "DcsDelAllWhiteBoard_Ntf", clz = TDCSDelWhiteBoardInfo.class)
    @Notification(name = "DcsDelAllWhiteBoard_Ntf", clz = TDCSDelWhiteBoardInfo.class)
    AllBoardDeleted,


    // 数据协作图元操作

    /**
     * 画线
     */
    @Request(name = "DCSOperLineOperInfoCmd",
            owner = Atlas.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbLineOperInfo.class},
            rspSeq = "LineDrawn")
    DrawLine,

    /**
     * 画圆/椭圆
     */
    @Request(name = "DCSOperCircleOperInfoCmd",
            owner = Atlas.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbCircleOperInfo.class},
            rspSeq = "OvalDrawn")
    DrawOval,

    /**
     * 画矩形
     */
    @Request(name = "DCSOperRectangleOperInfoCmd",
            owner = Atlas.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbRectangleOperInfo.class},
            rspSeq = "RectDrawn")
    DrawRect,

    /**
     * 画路径
     */
    @Request(name = "DCSOperPencilOperInfoCmd",
            owner = Atlas.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbPencilOperInfo.class},
            rspSeq = "PathDrawn")
    DrawPath,

    /**
     * 插入图片
     */
    @Request(name = "DCSOperInsertPicCmd",
            owner = Atlas.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbInsertPicOperInfo.class},
            rspSeq = "PicInserted")
    InsertPic,
    /**
     * 删除图片
     */
    @Request(name = "DCSOperPitchPicDelCmd",
            owner = Atlas.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbDelPicOperInfo.class},
            rspSeq = "PicDeleted")
    DelPic,
    /**
     * 拖动/放缩图片
     */
    @Request(name = "DCSOperPitchPicDragCmd",
            owner = Atlas.DcsCtrl,
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
            owner = Atlas.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbReginEraseOperInfo.class},
            rspSeq = "Erased")
    Erase,

    /**
     * 矩形擦除
     */
    @Request(name = "DCSOperEraseOperInfoCmd",
            owner = Atlas.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbEraseOperInfo.class},
            rspSeq = "RectErased")
    RectErase,

    /**
     * 清屏
     */
    @Request(name = "DCSOperClearScreenCmd",
            owner = Atlas.DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSOperReq.class,
            rspSeq = "ScreenCleared")
    ClearScreen,


    // 数据协作矩阵操作
    /**
     * 矩阵变换（放缩、位移等）
     */
    @Request(name = "DCSOperFullScreenCmd",
            owner = Atlas.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbDisPlayInfo.class},
            rspSeq = "Matrixed")
    Matrix,

    /**
     * 左旋转
     */
    @Request(name = "DCSOperRotateLeftCmd",
            owner = Atlas.DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSOperReq.class)
    RotateLeft, // TODO 待调

    /**
     * 右旋转
     */
    @Request(name = "DCSOperRotateRightCmd",
            owner = Atlas.DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSOperReq.class)
    RotateRight, // TODO 待调


    // 数据协作图元控制操作

    /**
     * 撤销
     */
    @Request(name = "DCSOperUndoCmd",
            owner = Atlas.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbTabPageIdInfo.class},
            rspSeq = "Undone")
    Undo,

    /**
     * 恢复（恢复被撤销的操作）
     */
    @Request(name = "DCSOperRedoCmd",
            owner = Atlas.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbTabPageIdInfo.class},
            rspSeq = "Redone")
    Redo,


    // 数据协作文件操作

    /**
     * 上传文件
     */
    @Request(name = "DCSUploadFileCmd",
            owner = Atlas.DcsCtrl,
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
            name = "DcsUploadFile_Ntf")
    UploadRsp,


    /**
     * 获取图片上传地址
     */
    @Request(name = "DCSUploadImageReq",
            owner = Atlas.DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSImageUrl.class,
            rspSeq = {"QueryPicUploadUrlRsp"})
    QueryPicUploadUrl,

    /**
     * 获取图片上传地址响应
     */
    @Response(clz = DcsUploadImageRsp.class,
            name = "DcsUploadImage_Rsp")
    QueryPicUploadUrlRsp,


    /**
     * 下载（图元、图片等）
     */
    @Request(name = "DCSDownloadFileReq",
            owner = Atlas.DcsCtrl,
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
            name = "DcsDownloadFile_Rsp")
    DownloadRsp,

    /**
     * 获取图片下载地址
     */
    @Request(name = "DCSDownloadImageReq",
            owner = Atlas.DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSImageUrl.class,
            rspSeq = {"QueryPicUrlRsp"})
    QueryPicUrl,

    /**
     * 获取下载图片地址响应
     */
    @Response(clz = DcsDownloadImageRsp.class,
            name = "DcsDownloadImage_Rsp")
    QueryPicUrlRsp,

    /**
     * 图片可下载通知
     */
    @Response(clz = TDCSImageUrl.class,
            name = "DcsDownloadImage_Ntf")
    @Notification(clz = TDCSImageUrl.class,
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
    @Response(clz = DcsOperLineOperInfoNtf.class,
            name = "DcsOperLineOperInfo_Ntf")
    @Notification(clz = DcsOperLineOperInfoNtf.class,
            name = "DcsOperLineOperInfo_Ntf")
    LineDrawn,

    /**
     * 圆/椭圆已绘制
     */
    @Response(clz = DcsOperCircleOperInfoNtf.class,
            name = "DcsOperCircleOperInfo_Ntf")
    @Notification(clz = DcsOperCircleOperInfoNtf.class,
            name = "DcsOperCircleOperInfo_Ntf")
    OvalDrawn,

    /**
     * 矩形已绘制
     */
    @Response(clz = DcsOperRectangleOperInfoNtf.class,
            name = "DcsOperRectangleOperInfo_Ntf")
    @Notification(clz = DcsOperRectangleOperInfoNtf.class,
            name = "DcsOperRectangleOperInfo_Ntf")
    RectDrawn,

    /**
     * 路径已绘制
     */
    @Response(clz = DcsOperPencilOperInfoNtf.class,
            name = "DcsOperPencilOperInfo_Ntf")
    @Notification(clz = DcsOperPencilOperInfoNtf.class,
            name = "DcsOperPencilOperInfo_Ntf")
    PathDrawn,


    /**
     * 图片插入通知
     */
    @Response(clz = DcsOperInsertPicNtf.class,
            name = "DcsOperInsertPic_Ntf")
    @Notification(clz = DcsOperInsertPicNtf.class,
            name = "DcsOperInsertPic_Ntf")
    PicInserted,

    /**
     * 图片拖动通知
     */
    @Response(clz = DcsOperPitchPicDragNtf.class,
            name = "DcsOperPitchPicDrag_Ntf")
    @Notification(clz = DcsOperPitchPicDragNtf.class,
            name = "DcsOperPitchPicDrag_Ntf")
    PicDragged,

    /**
     * 图片删除通知
     */
    @Response(clz = DcsOperPitchPicDelNtf.class,
            name = "DcsOperPitchPicDel_Ntf")
    @Notification(clz = DcsOperPitchPicDelNtf.class,
            name = "DcsOperPitchPicDel_Ntf")
    PicDeleted,

    /**
     * 黑板擦擦除通知
     */
    @Response(clz = DcsOperReginEraseNtf.class,
            name = "DcsOperReginErase_Ntf")
    @Notification(clz = DcsOperReginEraseNtf.class,
            name = "DcsOperReginErase_Ntf")
    Erased,

    /**
     * 矩形擦除通知
     */
    @Response(clz = DcsOperEraseOperInfoNtf.class,
            name = "DcsOperEraseOperInfo_Ntf")
    @Notification(clz = DcsOperEraseOperInfoNtf.class,
            name = "DcsOperEraseOperInfo_Ntf")
    RectErased,

    /**
     * 全屏matrix操作通知（缩放、移动、旋转）
     */
    @Response(clz = DcsOperFullScreenNtf.class,
            name = "DcsOperFullScreen_Ntf")
    @Notification(clz = DcsOperFullScreenNtf.class,
            name = "DcsOperFullScreen_Ntf")
    Matrixed,

    /**
     * 撤销操作通知
     */
    @Response(clz = DcsOperUndoNtf.class,
            name = "DcsOperUndo_Ntf")
    @Notification(clz = DcsOperUndoNtf.class,
            name = "DcsOperUndo_Ntf")
    Undone,

    /**
     * 恢复（恢复被撤销的操作）通知
     */
    @Response(clz = DcsOperRedoNtf.class,
            name = "DcsOperRedo_Ntf")
    @Notification(clz = DcsOperRedoNtf.class,
            name = "DcsOperRedo_Ntf")
    Redone,

    /**
     * 清屏通知
     */
    @Response(clz = TDCSOperContent.class,
            name = "DcsOperClearScreen_Ntf")
    @Notification(clz = TDCSOperContent.class,
            name = "DcsOperClearScreen_Ntf")
    ScreenCleared;

//    /**
//     * 图元序列结束通知
//     */ //NOTE: 下层“开始——结束”通知不可靠，时序数量均有问题，故废弃不用。
//    @Response(clz = TDcsCacheElementParseResult.class,
//            id = "DcsElementOperFinal_Ntf")
//    DCElementEndNtf,

}
