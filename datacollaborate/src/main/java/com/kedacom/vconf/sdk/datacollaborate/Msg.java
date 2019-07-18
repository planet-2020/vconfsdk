package com.kedacom.vconf.sdk.datacollaborate;


import androidx.annotation.RestrictTo;

import com.kedacom.vconf.sdk.annotation.Message;
import com.kedacom.vconf.sdk.annotation.Request;
import com.kedacom.vconf.sdk.annotation.Response;
import com.kedacom.vconf.sdk.datacollaborate.bean.transfer.*;


/**
 * Created by Sissi on 2018/9/3.
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
    DCGetServerAddr,

    /**获取数据协作相关状态*/
    @Request(method = "GetDCSServerStateRt",
            owner = MethodOwner.ConfigCtrl,
            paras = StringBuffer.class,
            userParas = TDCSSrvState.class,
            type = Request.GET)
    DCGetState,


    /**
     * 登录数据协作建链响应
     */
    @Response(clz = TDCSConnectResult.class,
            id = "DcsLoginResult_Ntf")
    DCBuildLink4LoginRsp,

    /**
     * 登录数据协作服务器
     */
    @Request(method = "LoginSrvReq",
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSRegInfo.class,
            rspSeq = {"DCBuildLink4LoginRsp",  // NOTE: 若该响应bSuccess字段为false则不会收到DCLoginRsp。
                    "DCLoginRsp"})
    DCLogin,

    /**
     * 登录数据协作服务器响应
     */
    @Response(clz = TDCSResult.class,
            id = "DcsLoginSrv_Rsp")
    DCLoginRsp,

    /**
     * 注销数据协作服务器
     */
    @Request(method = "DCSLogoutReq",
            owner = MethodOwner.DcsCtrl,
            rspSeq = {"DCLogoutRsp", // NOTE: 若该响应bSuccess字段为false则不会收到DCBuildLink4LoginRsp
                    "DCBuildLink4LoginRsp"
    })
    DCLogout,

    /**
     * 注销数据协作服务器响应
     */
    @Response(clz = TDCSResult.class,
            id = "DcsLogout_Rsp")
    DCLogoutRsp,


    /**查询数据协作地址*/
    @Request(method = "DCSGetConfAddrReq",
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = String.class, // 数据协作所在会议e164号
            rspSeq = "DCQueryAddrRsp"
    )
    DCQueryAddr,

    /**
     * 查询数据协作地址响应
     */
    @Response(clz = DcsGetConfAddrRsp.class,
            id = "DcsGetConfAddr_Rsp")
    DCQueryAddrRsp,

    /**
     * 创建数据协作建链响应/通知
     */
    @Response(clz = TDCSConnectResult.class,
            id = "DcsConfResult_Ntf")
    DCBuildLink4ConfRsp,

    /**
     * 创建数据协作
     */
    @Request(method = "DCSCreateConfReq",
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSCreateConf.class,
            rspSeq = {"DCBuildLink4ConfRsp",  // NOTE: 若该响应bSuccess字段为false则不会收到DCConfCreated。
                    "DCConfCreated"},
            rspSeq2 = "DCConfCreated")
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
            id = "DcsCreateConf_Rsp")
    DCConfCreated,


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
            rspSeq = {"DCQuitConfRsp",
                    "DCBuildLink4ConfRsp"})
    DCQuitConf,

    /**
     * 退出数据协作响应
     */
    @Response(clz = TDCSResult.class,
            id = "DcsQuitConf_Rsp")
    DCQuitConfRsp,

    /**
     * 结束数据协作
     */
    @Request(method = "DCSReleaseConfReq",
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = String.class, // 会议e164
            rspSeq = {"DCReleaseConfRsp","DCBuildLink4ConfRsp"},
            rspSeq2 = {"DCReleaseConfRsp","DCReleaseConfNtf","DCBuildLink4ConfRsp"})
    DCReleaseConf,

    /**
     * 结束数据协作响应
     */
    @Response(clz = TDCSResult.class,
            id = "DcsReleaseConf_Rsp")
    DCReleaseConfRsp,

    /**
     * 结束数据协作响应。
     * 注：该响应也会作为通知广播给其他与会者。
     */
    @Response(clz = BaseTypeString.class,
            id = "DcsReleaseConf_Ntf")
    DCReleaseConfNtf,

    /** 获取数据协作配置*/
    @Request(method = "DCSGetConfInfoReq",
            owner = MethodOwner.DcsCtrl,
            rspSeq = "DCQueryConfigRsp")
    DCQueryConfig,

    /** 获取数据协作配置响应*/
    @Response(clz = TDCSCreateConfResult.class,
            id = "DcsGetConfInfo_Rsp")
    DCQueryConfigRsp,

    /**修改数据协作配置*/
    @Request(method = "DCSSetConfInfoReq",
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSConfInfo.class,
            rspSeq = {"DCModifyConfigRsp", "DCConfigModified"})
    DCModifyConfig,

    /** 修改数据协作配置响应*/
    @Response(clz = DcsSetConfInfoRsp.class,
            id = "DcsSetConfInfo_Rsp")
    DCModifyConfigRsp,

    /**
     * 数据协作相关参数设置变更，如协作模式被修改。
     * */
    @Response(clz = TDCSConfInfo.class,
            id = "DcsUpdateConfInfo_Ntf")
    DCConfigModified,


    // 数据协作权限控制相关

    /**
     * （主席）添加协作方
     */
    @Request(method = "DCSAddOperatorReq",
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSOperator.class,
            rspSeq = {"DCAddOperatorRsp",
                    "DCOperatorAddedNtf"})
    DCAddOperator,

    /**
     * 添加协作方响应
     */
    @Response(clz = TDCSResult.class,
            id = "DcsAddOperator_Rsp")
    DCAddOperatorRsp,

    /**
     * （主席）删除协作方
     */
    @Request(method = "DCSDelOperatorReq",
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSOperator.class,
            rspSeq = {"DCDelOperatorRsp",
                    "DCOperatorDeletedNtf"})
    DCDelOperator,

    /**
     * 删除协作方响应
     */
    @Response(clz = TDCSResult.class,
            id = "DcsDelOperator_Rsp")
    DCDelOperatorRsp,

    /**
     * （自己）申请作为协作方
     */
    @Request(method = "DCSApplyOperReq",
            owner = MethodOwner.DcsCtrl,
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
            id = "DcsApplyOper_Rsp")
    DCApplyOperatorRsp,

    /**
     * （自己）取消作为协作方
     */
    @Request(method = "DCSCancelOperReq",
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = String.class, // 申请者的e164
            rspSeq = {"DCCancelOperatorRsp"})
    DCCancelOperator, // TODO 待调

    /**
     * 取消作为协作方响应
     */
    @Response(clz = TDCSResult.class,
            id = "DcsCancelOper_Rsp")
    DCCancelOperatorRsp,

    /**
     * （主席）拒绝成员申请作为协作方的请求
     */
    @Request(method = "DCSRejectOperatorCmd",
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSOperator.class)
    DCRejectApplyOperator,

    /**
     * 用户加入数据协作通知
     */
    @Response(clz = TDCSUserInfo.class,
            id = "DcsUserJoinConf_Ntf")
    DCUserJoinedNtf,

    /**
     * 成员（向主席）申请协作权通知
     */
    @Response(clz = TDCSUserInfo.class,
            id = "DcsUserApplyOper_Ntf")
    DCApplyOperatorNtf,

    /**
     * 协作方被添加通知
     */
    @Response(clz = TDCSUserInfos.class,
            id = "DcsAddOperator_Ntf")
    DCOperatorAddedNtf,
    /**
     * 协作方被删除通知
     */
    @Response(clz = TDCSUserInfos.class,
            id = "DcsDelOperator_Ntf")
    DCOperatorDeletedNtf,
    /**
     * 申请协作权被拒绝通知
     */
    @Response(clz = TDCSUserInfo.class,
            id = "DcsRejectOper_Ntf")
    DCApplyOperatorRejectedNtf,


    /**
     * 获取数据协作会议中的所有成员（包括协作方普通方）
     */
    @Request(method = "DCSGetUserListReq",
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = String.class, // 会议e164
            rspSeq = {"DCQueryAllMembersRsp"})
    DCQueryAllMembers,

    /**
     * 获取数据协作会议中的所有成员响应
     */
    @Response(clz = DcsGetUserListRsp.class,
            id = "DcsGetUserList_Rsp")  // 需单独定义的响应结构体名加Result后缀，需单独定义的参数名加para后缀
            DCQueryAllMembersRsp,


    // 数据协作画板相关

    /**
     * 新建画板
     */
    @Request(method = "DCSNewWhiteBoardReq",
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSNewWhiteBoard.class,
            rspSeq = {"DCNewBoardRsp",
                    "DCBoardCreatedNtf"})
    DCNewBoard,

    /**
     * 新建画板响应
     */
    @Response(clz = DcsNewWhiteBoardRsp.class,
            id = "DcsNewWhiteBoard_Rsp")
    DCNewBoardRsp,

    /**
     * 删除画板
     */
    @Request(method = "DCSDelWhiteBoardReq",
            owner = MethodOwner.DcsCtrl,
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
            id = "DcsDelWhiteBoard_Rsp")
    DCDelBoardRsp,

    /**
     * 删除所有画板
     */
    @Request(method = "DCSDelAllWhiteBoardReq",
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = String.class, // 当前会议e164
            rspSeq = {"DCDelAllBoardRsp",
                    "DCAllBoardDeletedNtf"})
    DCDelAllBoard,

    /**
     * 删除所有画板响应
     */
    @Response(clz = TDCSBoardResult.class,
            id = "DcsDelAllWhiteBoard_Rsp")
    DCDelAllBoardRsp,

    /**
     * 切换画板
     */
    @Request(method = "DCSSwitchReq",
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSSwitchReq.class,
            rspSeq = {"DCSwitchBoardRsp",
                    "DCBoardSwitchedNtf"})
    DCSwitchBoard,

    /**
     * 切换画板响应
     */
    @Response(clz = DcsSwitchRsp.class,
            id = "DcsSwitch_Rsp")
    DCSwitchBoardRsp,

    /**
     * 查询当前画板
     */
    @Request(method = "DCSGetCurWhiteBoardReq",
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = String.class, // 会议e164
            rspSeq = {"DCQueryCurBoardRsp"})
    DCQueryCurBoard,

    /**
     * 查询当前画板响应
     */
    @Response(clz = DcsGetWhiteBoardRsp.class,
            id = "DcsGetCurWhiteBoard_Rsp")
    DCQueryCurBoardRsp,


    /**
     * 查询画板
     */
    @Request(method = "DCSGetWhiteBoardReq",
            owner = MethodOwner.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {String.class, // 会议e164
                    String.class}, // 画板id
            rspSeq = {"DCQueryBoardRsp"})
    DCQueryBoard,

    /**
     * 查询画板响应
     */
    @Response(clz = DcsGetWhiteBoardRsp.class,
            id = "DcsGetWhiteBoard_Rsp")
    DCQueryBoardRsp,

    /**
     * 查询所有画板
     */
    @Request(method = "DCSGetAllWhiteBoardReq", //参数：StringBuffer类型 e164
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = String.class,
            rspSeq = {"DCQueryAllBoardsRsp"})
    DCQueryAllBoards,

    /**
     * 查询所有白板响应
     */
    @Response(clz = DcsGetAllWhiteBoardRsp.class,
            id = "DcsGetAllWhiteBoard_Rsp")
    DCQueryAllBoardsRsp,

    /**
     * 添加子页
     */
    @Request(method = "DCSOperAddSubPageInfoCmd",
            owner = MethodOwner.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbAddSubPageInfo.class})
    DCAddSubPage, // TODO 待调


    /**
     * 当前画板通知。
     * 该通知仅用于加入数据协作时通知入会方当前画板信息，其他场景下不会上报。
     * 收到该通知后会从服务器拉取当前画板已有图元。
     */
    @Response(clz = TDCSBoardInfo.class,
            id = "DcsCurrentWhiteBoard_Ntf")
    DCCurrentBoardNtf,

    /**
     * 新建画板通知
     */
    @Response(id = "DcsNewWhiteBoard_Ntf", clz = TDCSBoardInfo.class)
    DCBoardCreatedNtf,

    /**
     * 切换画板通知
     */
    @Response(id = "DcsSwitch_Ntf", clz = TDCSBoardInfo.class)
    DCBoardSwitchedNtf,

    /**
     * 删除画板通知
     */
    @Response(id = "DcsDelWhiteBoard_Ntf", clz = TDCSDelWhiteBoardInfo.class)
    DCBoardDeletedNtf,

    /**
     * 删除所有画板通知
     */
    @Response(id = "DcsDelAllWhiteBoard_Ntf", clz = TDCSDelWhiteBoardInfo.class)
    DCAllBoardDeletedNtf,


    // 数据协作图元操作

    /**
     * 画线
     */
    @Request(method = "DCSOperLineOperInfoCmd",
            owner = MethodOwner.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbLineOperInfo.class},
            rspSeq = "DCLineDrawnNtf")
    DCDrawLine,

    /**
     * 画圆/椭圆
     */
    @Request(method = "DCSOperCircleOperInfoCmd",
            owner = MethodOwner.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbCircleOperInfo.class},
            rspSeq = "DCOvalDrawnNtf")
    DCDrawOval,

    /**
     * 画矩形
     */
    @Request(method = "DCSOperRectangleOperInfoCmd",
            owner = MethodOwner.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbRectangleOperInfo.class},
            rspSeq = "DCRectDrawnNtf")
    DCDrawRect,

    /**
     * 画路径（铅笔操作）
     */
    @Request(method = "DCSOperPencilOperInfoCmd",
            owner = MethodOwner.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbPencilOperInfo.class},
            rspSeq = "DCPathDrawnNtf")
    DCDrawPath,

    /**
     * 插入图片
     * */
    @Request(method = "DCSOperInsertPicCmd",
            owner = MethodOwner.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbInsertPicOperInfo.class},
            rspSeq = "DCPicInsertedNtf")
    DCInsertPic,
    /**
     * 删除图片
     * */
    @Request(method = "DCSOperPitchPicDelCmd",
            owner = MethodOwner.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbDelPicOperInfo.class},
            rspSeq = "DCPicDeletedNtf")
    DCDeletePic,
    /**
     * 拖动/放缩图片
     * */
    @Request(method = "DCSOperPitchPicDragCmd",
            owner = MethodOwner.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbPitchPicOperInfo.class},
            rspSeq = "DCPicDraggedNtf")
    DCDragPic,
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
            rspSeq = "DCErasedNtf")
    DCErase,

    /**
     * 矩形擦除
     */
    @Request(method = "DCSOperEraseOperInfoCmd",
            owner = MethodOwner.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbEraseOperInfo.class},
            rspSeq = "DCRectErasedNtf")
    DCRectErase,

    /**
     * 清屏
     */
    @Request(method = "DCSOperClearScreenCmd",
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSOperReq.class,
            rspSeq = "DCScreenClearedNtf")
    DCClearScreen,


    // 数据协作矩阵操作
    /**
     * 矩阵变换（放缩、位移等）
     */
    @Request(method = "DCSOperFullScreenCmd",
            owner = MethodOwner.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbDisPlayInfo.class},
            rspSeq = "DCFullScreenMatrixOpNtf")
    DCMatrix,

    /**
     * 左旋转
     */
    @Request(method = "DCSOperRotateLeftCmd",
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSOperReq.class)
    DCRotateLeft, // TODO 待调

    /**
     * 右旋转
     */
    @Request(method = "DCSOperRotateRightCmd",
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSOperReq.class)
    DCRotateRight, // TODO 待调


    // 数据协作图元控制操作

    /**
     * 撤销
     */
    @Request(method = "DCSOperUndoCmd",
            owner = MethodOwner.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbTabPageIdInfo.class},
            rspSeq = "DCUndoneNtf")
    DCUndo,

    /**
     * 恢复（恢复被撤销的操作）
     */
    @Request(method = "DCSOperRedoCmd",
            owner = MethodOwner.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {TDCSOperReq.class, TDCSWbTabPageIdInfo.class},
            rspSeq = "DCRedoneNtf")
    DCRedo,


    // 数据协作文件操作

    /**
     * 上传文件
     */
    @Request(method = "DCSUploadFileCmd",
            owner = MethodOwner.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {BaseTypeString.class, // 下载url。NOTE: 下层规定先将url包装到该类里面转成json然后传下，下层将json解析出来进而萃取出url。
                    TDCSFileInfo.class},
            rspSeq = {"DCUploadNtf", "DCPicDownloadableNtf"},
            rspSeq2 = "DCPicDownloadableNtf",
            timeout = 15)
    DCUpload,

    /**
     * 上传文件响应
     */
    @Response(clz = TDCSFileLoadResult.class,
            id = "DcsUploadFile_Ntf")
    DCUploadNtf,


    /**
     * 获取图片上传地址
     */
    @Request(method = "DCSUploadImageReq",
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSImageUrl.class,
            rspSeq = {"DCQueryPicUploadUrlRsp"})
    DCQueryPicUploadUrl,

    /**
     * 获取图片上传地址响应
     */
    @Response(clz = DcsUploadImageRsp.class,
            id = "DcsUploadImage_Rsp")
    DCQueryPicUploadUrlRsp,


    /**
     * 下载（图元、图片等）
     */
    @Request(method = "DCSDownloadFileReq",
            owner = MethodOwner.DcsCtrl,
            paras = {StringBuffer.class, StringBuffer.class},
            userParas = {BaseTypeString.class,
                    TDCSFileInfo.class},
            rspSeq = {"DCDownloadRsp"},
            timeout = 15)
    DCDownload,

    /**
     * 下载响应
     */
    @Response(clz = TDCSFileLoadResult.class,
            id = "DcsDownloadFile_Rsp")
    DCDownloadRsp,

    /**
     * 获取图片下载地址
     */
    @Request(method = "DCSDownloadImageReq",
            owner = MethodOwner.DcsCtrl,
            paras = StringBuffer.class,
            userParas = TDCSImageUrl.class,
            rspSeq = {"DCQueryPicUrlRsp"})
    DCQueryPicUrl,

    /**
     * 获取下载图片地址响应
     */
    @Response(clz = DcsDownloadImageRsp.class,
            id = "DcsDownloadImage_Rsp")
    DCQueryPicUrlRsp,

    /**
     * 图片可下载通知
     */
    @Response(clz = TDCSImageUrl.class,
            id = "DcsDownloadImage_Ntf")
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
            id = "DcsOperLineOperInfo_Ntf")
    DCLineDrawnNtf,

    /**
     * 画圆/椭圆通知
     */
    @Response(clz = DcsOperCircleOperInfoNtf.class,
            id = "DcsOperCircleOperInfo_Ntf")
    DCOvalDrawnNtf,

    /**
     * 画矩形通知
     */
    @Response(clz = DcsOperRectangleOperInfoNtf.class,
            id = "DcsOperRectangleOperInfo_Ntf")
    DCRectDrawnNtf,

    /**
     * 画路径（铅笔操作）通知
     */
    @Response(clz = DcsOperPencilOperInfoNtf.class,
            id = "DcsOperPencilOperInfo_Ntf")
    DCPathDrawnNtf,

//    /**彩笔操作通知*/
//    @Deprecated
//    @Response(clz = MsgBeans.DcsOperColorPenOperInfo_Ntf.class)
//    DcsOperColorPenOperInfo_Ntf,

    /**
     * 图片插入通知
     */
    @Response(clz = DcsOperInsertPicNtf.class,
            id = "DcsOperInsertPic_Ntf")
    DCPicInsertedNtf,

    /**
     * 图片拖动通知
     */
    @Response(clz = DcsOperPitchPicDragNtf.class,
            id = "DcsOperPitchPicDrag_Ntf")
    DCPicDraggedNtf,

    /**
     * 图片删除通知
     */
    @Response(clz = DcsOperPitchPicDelNtf.class,
            id = "DcsOperPitchPicDel_Ntf")
    DCPicDeletedNtf,

    /**
     * 黑板擦擦除通知
     */
    @Response(clz = DcsOperReginEraseNtf.class,
            id = "DcsOperReginErase_Ntf")
    DCErasedNtf,

    /**
     * 矩形擦除通知
     */
    @Response(clz = DcsOperEraseOperInfoNtf.class,
            id = "DcsOperEraseOperInfo_Ntf")
    DCRectErasedNtf,

    /**
     * 全屏matrix操作通知（缩放、移动、旋转）
     */
    @Response(clz = DcsOperFullScreenNtf.class,
            id = "DcsOperFullScreen_Ntf")
    DCFullScreenMatrixOpNtf,

    /**
     * 撤销操作通知
     */
    @Response(clz = DcsOperUndoNtf.class,
            id = "DcsOperUndo_Ntf")
    DCUndoneNtf,

    /**
     * 恢复（恢复被撤销的操作）通知
     */
    @Response(clz = DcsOperRedoNtf.class,
            id = "DcsOperRedo_Ntf")
    DCRedoneNtf,

    /**
     * 清屏通知
     */
    @Response(clz = TDCSOperContent.class,
            id = "DcsOperClearScreen_Ntf")
    DCScreenClearedNtf;

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
