package com.kedacom.vconf.sdk.base;


import androidx.annotation.RestrictTo;

import com.kedacom.vconf.sdk.annotation.Message;
import com.kedacom.vconf.sdk.annotation.Request;
import com.kedacom.vconf.sdk.annotation.Response;
import com.kedacom.vconf.sdk.base.bean.dc.BaseTypeString;
import com.kedacom.vconf.sdk.base.bean.dc.DcsDownloadImageRsp;
import com.kedacom.vconf.sdk.base.bean.dc.DcsGetAllWhiteBoardRsp;
import com.kedacom.vconf.sdk.base.bean.dc.DcsGetUserListRsp;
import com.kedacom.vconf.sdk.base.bean.dc.DcsGetWhiteBoardRsp;
import com.kedacom.vconf.sdk.base.bean.dc.DcsNewWhiteBoardRsp;
import com.kedacom.vconf.sdk.base.bean.dc.DcsOperCircleOperInfoNtf;
import com.kedacom.vconf.sdk.base.bean.dc.DcsOperEraseOperInfoNtf;
import com.kedacom.vconf.sdk.base.bean.dc.DcsOperFullScreenNtf;
import com.kedacom.vconf.sdk.base.bean.dc.DcsOperInsertPicNtf;
import com.kedacom.vconf.sdk.base.bean.dc.DcsOperLineOperInfoNtf;
import com.kedacom.vconf.sdk.base.bean.dc.DcsOperPencilOperInfoNtf;
import com.kedacom.vconf.sdk.base.bean.dc.DcsOperPitchPicDelNtf;
import com.kedacom.vconf.sdk.base.bean.dc.DcsOperPitchPicDragNtf;
import com.kedacom.vconf.sdk.base.bean.dc.DcsOperRectangleOperInfoNtf;
import com.kedacom.vconf.sdk.base.bean.dc.DcsOperRedoNtf;
import com.kedacom.vconf.sdk.base.bean.dc.DcsOperReginEraseNtf;
import com.kedacom.vconf.sdk.base.bean.dc.DcsOperUndoNtf;
import com.kedacom.vconf.sdk.base.bean.dc.DcsSwitchRsp;
import com.kedacom.vconf.sdk.base.bean.dc.DcsUploadImageRsp;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSBoardInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSBoardResult;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSConnectResult;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSCreateConf;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSCreateConfResult;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSDelWhiteBoardInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSFileInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSFileLoadResult;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSImageUrl;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSNewWhiteBoard;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSOperContent;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSOperReq;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSOperator;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSRegInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSResult;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSSwitchReq;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSUserInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSUserInfos;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbAddSubPageInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbCircleOperInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbDelPicOperInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbDisPlayInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbEraseOperInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbInsertPicOperInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbLineOperInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbPencilOperInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbPitchPicOperInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbRectangleOperInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbReginEraseOperInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDCSWbTabPageIdInfo;
import com.kedacom.vconf.sdk.base.bean.dc.TDcsCacheElementParseResult;

import static com.kedacom.vconf.sdk.base.NativeMethodOwners.DcsCtrl;

/**
 * Created by Sissi on 2018/9/3.
 * 消息。
 */

@SuppressWarnings("unused")
@RestrictTo(RestrictTo.Scope.LIBRARY)
@Message
public enum Msg {
    @Response(name = "Timeout",
            clz = Void.class)
    Timeout,

    //>>>>>>>>>>>>>>>>>>> 数据协作

    // 数据协作基础

//    /**获取数据协作服务器地址*/
//    @Get(result = MsgBeans.DCServerAddr.class)
//    DCGetServerAddr,


    /**
     * 登录数据协作建链响应
     */
    @Response(clz = TDCSConnectResult.class,
            name = "DcsLoginResult_Ntf")
    DCBuildLink4LoginRsp,

    /**
     * 登录数据协作服务器
     */
    @Request(method = "LoginSrvReq",
            owner = DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSRegInfo.class,
            rspSeq = {"DCBuildLink4LoginRsp",  // NOTE: 若该响应bSuccess字段为false则不会收到DCLoginRsp。
                    "DCLoginRsp"})
    DCLogin,

    /**
     * 登录数据协作服务器响应
     */
    @Response(clz = TDCSResult.class,
            name = "DcsLoginSrv_Rsp")
    DCLoginRsp,

    /**
     * 注销数据协作服务器
     */
    @Request(method = "DCSLogoutReq",
            owner = DcsCtrl,
            rspSeq = {"DCLogoutRsp", // NOTE: 若该响应bSuccess字段为false则不会收到DCBuildLink4LoginRsp
                    "DCBuildLink4LoginRsp"
    })
    DCLogout,

    /**
     * 注销数据协作服务器响应
     */
    @Response(clz = TDCSResult.class,
            name = "DcsLogout_Rsp")
    DCLogoutRsp,


    /**
     * 创建数据协作建链响应/通知
     */
    @Response(clz = TDCSConnectResult.class,
            name = "DcsConfResult_Ntf")
    DCBuildLink4ConfRsp,

    /**
     * 创建数据协作
     */
    @Request(method = "DCSCreateConfReq",
            owner = DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSCreateConf.class,
            rspSeq = {"DCBuildLink4ConfRsp",  // NOTE: 若该响应bSuccess字段为false则不会收到DCConfCreated。
                    "DCConfCreated"})
    DCCreateConf,

    /**
     * 己端创建数据协作时的响应；
     * 其他终端创建数据协作时的通知；
     * <p>
     * 当会议中有人创建数据协作时，平台会发送一个邀请通知给各个与会方，
     * 下层（组件层）收到邀请会主动加入该数据协作，然后再上报该条消息给界面，
     * 所以该消息既是响应也是通知。
     */
    @Response(clz = TDCSCreateConfResult.class,
            name = "DcsCreateConf_Rsp")
    DCConfCreated,


    /**
     * 退出数据协作。
     * 注：仅自己退出，协作仍存在，不影响其他人继续
     */
    @Request(method = "DCSQuitConfReq",
            owner = DcsCtrl,
            paras = {StringBuffer.class, int.class},
            userParas = {String.class, // 会议e164
                    Integer.class // 是否同时退出会议。0表示退出协作的同时退出会议，1表示仅退出协作。
            },
            rspSeq = {"DCQuitConfRsp",
                    "DCBuildLink4ConfRsp"})
    DCQuitConf,

    /**
     * 退出数据协作响应
     */
    @Response(clz = TDCSResult.class,
            name = "DcsQuitConf_Rsp")
    DCQuitConfRsp,

    /**
     * 结束数据协作
     */
    @Request(method = "DCSReleaseConfReq",
            owner = DcsCtrl,
            paras = StringBuffer.class,
            userParas = String.class, // 会议e164
            rspSeq = {"DCReleaseConfRsp","DCBuildLink4ConfRsp"},
            rspSeq2 = {"DCReleaseConfRsp","DCReleaseConfNtf","DCBuildLink4ConfRsp"})
    DCReleaseConf,

    /**
     * 结束数据协作响应
     */
    @Response(clz = TDCSResult.class,
            name = "DcsReleaseConf_Rsp")
    DCReleaseConfRsp,

    /**
     * 结束数据协作响应。
     * 注：该响应也会作为通知广播给其他与会者。
     */
    @Response(clz = BaseTypeString.class,
            name = "DcsReleaseConf_Ntf")
    DCReleaseConfNtf,


    // 数据协作权限控制相关

    /**
     * （主席）添加协作方
     */
    @Request(method = "DCSAddOperatorReq",
            owner = DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSOperator.class,
            rspSeq = {"DCAddOperatorRsp",
                    "DCOperatorAddedNtf"})
    DCAddOperator,

    /**
     * 添加协作方响应
     */
    @Response(clz = TDCSResult.class,
            name = "DcsAddOperator_Rsp")
    DCAddOperatorRsp,

    /**
     * （主席）删除协作方
     */
    @Request(method = "DCSDelOperatorReq",
            owner = DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSOperator.class,
            rspSeq = {"DCDelOperatorRsp",
                    "DCOperatorDeletedNtf"})
    DCDelOperator,

    /**
     * 删除协作方响应
     */
    @Response(clz = TDCSResult.class,
            name = "DcsDelOperator_Rsp")
    DCDelOperatorRsp,

    /**
     * （自己）申请作为协作方
     */
    @Request(method = "DCSApplyOperReq",
            owner = DcsCtrl,
            paras = StringBuffer.class,
            userParas = String.class, // 申请者的e164
            rspSeq = {"DCApplyOperatorRsp", "DCApplyOperatorRejectedNtf"},
            rspSeq2 = {"DCApplyOperatorRsp", "DCOperatorAddedNtf"},
            timeout = 30)
    DCApplyOperator,

    /**
     * 申请作为协作方响应
     */
    @Response(clz = TDCSResult.class,
            name = "DcsApplyOper_Rsp")
    DCApplyOperatorRsp,

    /**
     * （自己）取消作为协作方
     */
    @Request(method = "DCSCancelOperReq",
            owner = DcsCtrl,
            paras = StringBuffer.class,
            userParas = String.class, // 申请者的e164
            rspSeq = {"DCCancelOperatorRsp"})
    DCCancelOperator, // TODO 待调

    /**
     * 取消作为协作方响应
     */
    @Response(clz = TDCSResult.class,
            name = "DcsCancelOper_Rsp")
    DCCancelOperatorRsp,

    /**
     * （主席）拒绝成员申请作为协作方的请求
     */
    @Request(method = "DCSRejectOperatorCmd",
            owner = DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSOperator.class)
    DCRejectApplyOperator,

    /**
     * 用户加入数据协作通知
     */
    @Response(clz = TDCSUserInfo.class,
            name = "DcsUserJoinConf_Ntf")
    DCUserJoinedNtf,

    /**
     * 成员（向主席）申请协作权通知
     */
    @Response(clz = TDCSUserInfo.class,
            name = "DcsUserApplyOper_Ntf")
    DCApplyOperatorNtf,

    /**
     * 协作方被添加通知
     */
    @Response(clz = TDCSUserInfos.class,
            name = "DcsAddOperator_Ntf")
    DCOperatorAddedNtf,
    /**
     * 协作方被删除通知
     */
    @Response(clz = TDCSUserInfos.class,
            name = "DcsDelOperator_Ntf")
    DCOperatorDeletedNtf,
    /**
     * 申请协作权被拒绝通知
     */
    @Response(clz = TDCSUserInfo.class,
            name = "DcsRejectOper_Ntf")
    DCApplyOperatorRejectedNtf,


    /**
     * 获取数据协作会议中的所有成员（包括协作方普通方）
     */
    @Request(method = "DCSGetUserListReq",
            owner = DcsCtrl,
            paras = StringBuffer.class,
            userParas = String.class, // 会议e164
            rspSeq = {"DCQueryAllMembersRsp"})
    DCQueryAllMembers, // TODO 待调

    /**
     * 获取数据协作会议中的所有成员响应
     */
    @Response(clz = DcsGetUserListRsp.class,
            name = "DcsGetUserList_Rsp")  // 需单独定义的响应结构体名加Result后缀，需单独定义的参数名加para后缀
            DCQueryAllMembersRsp,


    // 数据协作画板相关

    /**
     * 新建画板
     */
    @Request(method = "DCSNewWhiteBoardReq",
            owner = DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSNewWhiteBoard.class,
            rspSeq = {"DCNewBoardRsp",
                    "DCBoardCreatedNtf"})
    DCNewBoard,

    /**
     * 新建画板响应
     */
    @Response(clz = DcsNewWhiteBoardRsp.class,
            name = "DcsNewWhiteBoard_Rsp")
    DCNewBoardRsp,

    /**
     * 删除画板
     */
    @Request(method = "DCSDelWhiteBoardReq",
            owner = DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {String.class, // 当前会议e164
                    String.class}, // 画板Id
            rspSeq = {"DCDelBoardRsp",
                    "DCBoardDeletedNtf"})
    DCDelBoard,

    /**
     * 删除画板响应
     */
    @Response(clz = TDCSBoardResult.class,
            name = "DcsDelWhiteBoard_Rsp")
    DCDelBoardRsp,

    /**
     * 删除所有画板
     */
    @Request(method = "DCSDelAllWhiteBoardReq",
            owner = DcsCtrl,
            paras = StringBuffer.class,
            userParas = String.class, // 当前会议e164
            rspSeq = {"DCDelAllBoardRsp",
                    "DCAllBoardDeletedNtf"})
    DCDelAllBoard,

    /**
     * 删除所有画板响应
     */
    @Response(clz = TDCSBoardResult.class,
            name = "DcsDelAllWhiteBoard_Rsp")
    DCDelAllBoardRsp,

    /**
     * 切换画板
     */
    @Request(method = "DCSSwitchReq",
            owner = DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSSwitchReq.class,
            rspSeq = {"DCSwitchBoardRsp",
                    "DCBoardSwitchedNtf"})
    DCSwitchBoard,

    /**
     * 切换画板响应
     */
    @Response(clz = DcsSwitchRsp.class,
            name = "DcsSwitch_Rsp")
    DCSwitchBoardRsp,

    /**
     * 查询当前画板
     */
    @Request(method = "DCSGetCurWhiteBoardReq",
            owner = DcsCtrl,
            paras = StringBuffer.class,
            userParas = String.class, // 会议e164
            rspSeq = {"DCQueryCurBoardRsp"})
    DCQueryCurBoard,

    /**
     * 查询当前画板响应
     */
    @Response(clz = DcsGetWhiteBoardRsp.class,
            name = "DcsGetCurWhiteBoard_Rsp")
    DCQueryCurBoardRsp,


    /**
     * 查询画板
     */
    @Request(method = "DCSGetWhiteBoardReq",
            owner = DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {String.class, // 会议e164
                    String.class}, // 画板id
            rspSeq = {"DCQueryBoardRsp"})
    DCQueryBoard,

    /**
     * 查询画板响应
     */
    @Response(clz = DcsGetWhiteBoardRsp.class,
            name = "DcsGetWhiteBoard_Rsp")
    DCQueryBoardRsp,

    /**
     * 查询所有画板
     */
    @Request(method = "DCSGetAllWhiteBoardReq", //参数：StringBuffer类型 e164
            owner = DcsCtrl,
            paras = StringBuffer.class,
            userParas = String.class,
            rspSeq = {"DCQueryAllBoardsRsp"})
    DCQueryAllBoards,

    /**
     * 查询所有白板响应
     */
    @Response(clz = DcsGetAllWhiteBoardRsp.class,
            name = "DcsGetAllWhiteBoard_Rsp")
    DCQueryAllBoardsRsp,

    /**
     * 添加子页
     */
    @Request(method = "DCSOperAddSubPageInfoCmd",
            owner = DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbAddSubPageInfo.class})
    DCAddSubPage, // TODO 待调


    /**
     * 当前画板通知。
     * 该通知仅用于加入数据协作时通知入会方当前画板信息，其他场景下不会上报。
     * 收到该通知后会从服务器拉取当前画板已有图元。
     */
    @Response(clz = TDCSBoardInfo.class,
            name = "DcsCurrentWhiteBoard_Ntf")
    DCCurrentBoardNtf,

    /**
     * 新建画板通知
     */
    @Response(name = "DcsNewWhiteBoard_Ntf", clz = TDCSBoardInfo.class)
    DCBoardCreatedNtf,

    /**
     * 切换画板通知
     */
    @Response(name = "DcsSwitch_Ntf", clz = TDCSBoardInfo.class)
    DCBoardSwitchedNtf,

    /**
     * 删除画板通知
     */
    @Response(name = "DcsDelWhiteBoard_Ntf", clz = TDCSDelWhiteBoardInfo.class)
    DCBoardDeletedNtf,

    /**
     * 删除所有画板通知
     */
    @Response(name = "DcsDelAllWhiteBoard_Ntf", clz = TDCSDelWhiteBoardInfo.class)
    DCAllBoardDeletedNtf,


    // 数据协作图元操作

    /**
     * 画线
     */
    @Request(method = "DCSOperLineOperInfoCmd",
            owner = DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbLineOperInfo.class})
    DCDrawLine,

    /**
     * 画圆/椭圆
     */
    @Request(method = "DCSOperCircleOperInfoCmd",
            owner = DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbCircleOperInfo.class})
    DCDrawOval,

    /**
     * 画矩形
     */
    @Request(method = "DCSOperRectangleOperInfoCmd",
            owner = DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbRectangleOperInfo.class})
    DCDrawRect,

    /**
     * 画路径（铅笔操作）
     */
    @Request(method = "DCSOperPencilOperInfoCmd",
            owner = DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbPencilOperInfo.class})
    DCDrawPath,

    @Request(method = "DCSOperInsertPicCmd",
            owner = DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbInsertPicOperInfo.class})
    DCInsertPic,
    @Request(method = "DCSOperPitchPicDelCmd",
            owner = DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbDelPicOperInfo.class})
    DCDeletePic,
    @Request(method = "DCSOperPitchPicDragCmd",
            owner = DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbPitchPicOperInfo.class})
    DCDragPic,
    @Request(method = "DCSOperPitchPicZoomCmd",
            owner = DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbPitchPicOperInfo.class})
    DCZoomPic, // TODO 待调

    /**
     * 黑板擦擦除
     */
    @Request(method = "DCSOperReginEraseCmd",
            owner = DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbReginEraseOperInfo.class})
    DCErase,

    /**
     * 矩形擦除
     */
    @Request(method = "DCSOperEraseOperInfoCmd",
            owner = DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbEraseOperInfo.class})
    DCRectErase,

    /**
     * 清屏
     */
    @Request(method = "DCSOperClearScreenCmd",
            owner = DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSOperReq.class)
    DCClearScreen,


    // 数据协作矩阵操作
    /**
     * 矩阵变换（放缩、位移等）
     */
    @Request(method = "DCSOperFullScreenCmd",
            owner = DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbDisPlayInfo.class})
    DCMatrix,

    /**
     * 左旋转
     */
    @Request(method = "DCSOperRotateLeftCmd",
            owner = DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSOperReq.class)
    DCRotateLeft, // TODO 待调

    /**
     * 右旋转
     */
    @Request(method = "DCSOperRotateRightCmd",
            owner = DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSOperReq.class)
    DCRotateRight, // TODO 待调


    // 数据协作图元控制操作

    /**
     * 撤销
     */
    @Request(method = "DCSOperUndoCmd",
            owner = DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbTabPageIdInfo.class})
    DCUndo,

    /**
     * 恢复（恢复被撤销的操作）
     */
    @Request(method = "DCSOperRedoCmd",
            owner = DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbTabPageIdInfo.class})
    DCRedo,


    // 数据协作文件操作

    /**
     * 上传文件
     */
    @Request(method = "DCSUploadFileCmd",
            owner = DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {BaseTypeString.class, // 下载url。XXX 下层规定先将url包装到该类里面转成json然后传下，下层将json解析出来进而萃取出url。
                    TDCSFileInfo.class},
            rspSeq = {"DCUploadNtf"},
            timeout = 30)
    DCUpload,

    /**
     * 上传文件响应
     */
    @Response(clz = TDCSFileLoadResult.class,
            name = "DcsUploadFile_Ntf")
    DCUploadNtf,


    /**
     * 获取图片上传地址
     */
    @Request(method = "DCSUploadImageReq",
            owner = DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSImageUrl.class,
            rspSeq = {"DCQueryPicUploadUrlRsp"})
    DCQueryPicUploadUrl,

    /**
     * 获取图片上传地址响应
     */
    @Response(clz = DcsUploadImageRsp.class,
            name = "DcsUploadImage_Rsp")
    DCQueryPicUploadUrlRsp,


    /**
     * 下载（图元、图片等）
     */
    @Request(method = "DCSDownloadFileReq",
            owner = DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {BaseTypeString.class,
                    TDCSFileInfo.class},
            rspSeq = {"DCDownloadRsp"},
            timeout = 30)
    DCDownload,

    /**
     * 下载响应
     */
    @Response(clz = TDCSFileLoadResult.class,
            name = "DcsDownloadFile_Rsp")
    DCDownloadRsp,

    /**
     * 获取图片下载地址
     */
    @Request(method = "DCSDownloadImageReq",
            owner = DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSImageUrl.class,
            rspSeq = {"DCQueryPicUrlRsp"})
    DCQueryPicUrl,

    /**
     * 获取下载图片地址响应
     */
    @Response(clz = DcsDownloadImageRsp.class,
            name = "DcsDownloadImage_Rsp")
    DCQueryPicUrlRsp,

    /**
     * 图片可下载通知
     */
    @Response(clz = TDCSImageUrl.class,
            name = "DcsDownloadImage_Ntf")
    DCPicDownloadableNtf,


    // 数据协作图元操作通知

//    /**图元序列开始通知。
//     * 注：新加入数据协作会议后，服务器会将当前数据协作会议中已存在的图元序列同步到新加入的与会方。*/
//    @Deprecated
//    @Response(clz = MsgBeans.DCBoardId.class,
//					name ="DcsElementOperBegin_Ntf") //注意：组件没有处理此条消息
//    DCElementBeginNtf,

    /**
     * 画直线通知
     */
    @Response(clz = DcsOperLineOperInfoNtf.class,
            name = "DcsOperLineOperInfo_Ntf")
    DCLineDrawnNtf,

    /**
     * 画圆/椭圆通知
     */
    @Response(clz = DcsOperCircleOperInfoNtf.class,
            name = "DcsOperCircleOperInfo_Ntf")
    DCOvalDrawnNtf,

    /**
     * 画矩形通知
     */
    @Response(clz = DcsOperRectangleOperInfoNtf.class,
            name = "DcsOperRectangleOperInfo_Ntf")
    DCRectDrawnNtf,

    /**
     * 画路径（铅笔操作）通知
     */
    @Response(clz = DcsOperPencilOperInfoNtf.class,
            name = "DcsOperPencilOperInfo_Ntf")
    DCPathDrawnNtf,

//    /**彩笔操作通知*/
//    @Deprecated
//    @Response(clz = MsgBeans.DcsOperColorPenOperInfo_Ntf.class)
//    DcsOperColorPenOperInfo_Ntf,

    /**
     * 图片插入通知
     */
    @Response(clz = DcsOperInsertPicNtf.class,
            name = "DcsOperInsertPic_Ntf")
    DCPicInsertedNtf,

    /**
     * 图片拖动通知
     */
    @Response(clz = DcsOperPitchPicDragNtf.class,
            name = "DcsOperPitchPicDrag_Ntf")
    DCPicDraggedNtf,

    /**
     * 图片删除通知
     */
    @Response(clz = DcsOperPitchPicDelNtf.class,
            name = "DcsOperPitchPicDel_Ntf")
    DCPicDeletedNtf,

    /**
     * 黑板擦擦除通知
     */
    @Response(clz = DcsOperReginEraseNtf.class,
            name = "DcsOperReginErase_Ntf")
    DCErasedNtf,

    /**
     * 矩形擦除通知
     */
    @Response(clz = DcsOperEraseOperInfoNtf.class,
            name = "DcsOperEraseOperInfo_Ntf")
    DCRectErasedNtf,

    /**
     * 全屏matrix操作通知（缩放、移动、旋转）
     */
    @Response(clz = DcsOperFullScreenNtf.class,
            name = "DcsOperFullScreen_Ntf")
    DCFullScreenMatrixOpNtf,

    /**
     * 撤销操作通知
     */
    @Response(clz = DcsOperUndoNtf.class,
            name = "DcsOperUndo_Ntf")
    DCUndoneNtf,

    /**
     * 恢复（恢复被撤销的操作）通知
     */
    @Response(clz = DcsOperRedoNtf.class,
            name = "DcsOperRedo_Ntf")
    DCRedoneNtf,

    /**
     * 清屏通知
     */
    @Response(clz = TDCSOperContent.class,
            name = "DcsOperClearScreen_Ntf")
    DCScreenClearedNtf,

    /**
     * 图元序列结束通知
     */
    @Response(clz = TDcsCacheElementParseResult.class,
            name = "DcsElementOperFinal_Ntf")
    DCElementEndNtf,


    //<<<<<<<<<<<<<<<<<< 数据协作

}
