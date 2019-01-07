package com.kedacom.vconf.sdk.base;


import androidx.annotation.RestrictTo;

import com.kedacom.vconf.sdk.annotation.Message;
import com.kedacom.vconf.sdk.annotation.Notification;
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
 *
 */

@SuppressWarnings("unused")
@RestrictTo(RestrictTo.Scope.LIBRARY)
@Message
public enum Msg {
    @Response(clz = Void.class)
    Timeout,

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
    @Response(clz = TDCSConnectResult.class,
				name="DcsLoginResult_Ntf")
    DCBuildLink4LoginRsp,

    /**登录数据协作服务器。
     * 注：登录前需先建链。*/
    @Request(name = "LoginSrvReq",
            methodOwner = DcsCtrl,
            paras = TDCSRegInfo.class,
            rspSeq = {"DcsLoginResult_Ntf",  // 登录时下层自动建链，然后就抛了这条消息上来。NOTE: 对于失败的情形只会收到DCBuildLink4LoginRsp而没有DCLoginRsp。
					  "DcsLoginSrv_Rsp"})
    DCLogin,

    /**登录数据协作服务器响应*/
    @Response(clz = TDCSResult.class,
			  name = "DcsLoginSrv_Rsp")
    DCLoginRsp,

    /**注销数据协作服务器*/
    @Request(name = "DCSLogoutReq",
            methodOwner = DcsCtrl,
			rspSeq = {"DcsLogout_Rsp"}, timeout = 5)
    DCLogout, // TODO 收不到响应，跟下层确认

    /**注销数据协作服务器响应*/
    @Response(clz = TDCSResult.class,
			  name = "DcsLogout_Rsp")
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
    @Response(clz = TDCSConnectResult.class,
			  name = "DcsConfResult_Ntf")
    DCBuildLink4ConfRsp,

    /**创建数据协作。
     * 注：创建数据协作前需先建链。*/
    @Request(name = "DCSCreateConfReq",
            methodOwner = DcsCtrl,
			 paras = TDCSCreateConf.class,
             rspSeq = {"DcsConfResult_Ntf",  // 创建数据协作时下层自动建链，然后就抛了这条消息上来。NOTE: 对于失败的情形只会收到DCBuildLink4ConfRsp而没有DCCreateConfRsp。
						"DcsCreateConf_Rsp"})
    DCCreateConf,

    /**己端创建数据协作时的响应；
     * 其他终端创建数据协作时的通知；
     *
     * 当会议中有人创建数据协作时，平台会发送一个邀请通知给各个与会方，
     * 下层（组件层）收到邀请会主动加入该数据协作，然后再上报该条消息给界面，
     * 所以该消息既是响应也是通知。*/
    @Notification(clz = TDCSCreateConfResult.class,
				  name = "DcsCreateConf_Rsp")
    @Response(clz = TDCSCreateConfResult.class,
				  name = "DcsCreateConf_Rsp")
    DCConfCreated,


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
    @Request( name = "DCSQuitConfReq",
            methodOwner = DcsCtrl,
              paras = {String.class, // 会议e164
                      Integer.class // 是否同时退出会议。0表示退出协作的同时退出会议，1表示仅退出协作。
              },
              rspSeq = {"DcsQuitConf_Rsp"})
    DCQuitConf, // TODO 待调

    /**退出数据协作响应*/
    @Response(clz = TDCSResult.class,
			  name = "DcsQuitConf_Rsp")
    DCQuitConfRsp,

    /**结束数据协作*/
    @Request( name = "DCSReleaseConfReq",
            methodOwner = DcsCtrl,
            paras = String.class, // 会议e164
             rspSeq = {"DcsReleaseConf_Rsp"})
    DCReleaseConf, // TODO 待调

    /**结束数据协作响应*/
    @Response(clz = TDCSResult.class,
				name = "DcsReleaseConf_Rsp")
    DCReleaseConfRsp,

    /**结束数据协作响应。
     * 注：该响应也会作为通知广播给其他与会者。*/
    @Notification(clz = BaseTypeString.class,
					name = "DcsReleaseConf_Ntf")
    @Response(clz = BaseTypeString.class,
				name = "DcsReleaseConf_Ntf")
    DCReleaseConfNtf, // TODO 待调

//    /**当前终端拒绝入会*/
//    @Deprecated // 下层已经做掉了（目前是始终同意入数据协作）。入会后若当前会议存在数据协作，平台会通知各终端入数据协作，各终端可选择加入或拒绝。
//    @Request
//    DCSRejectJoinConfCmd,


    // 数据协作权限控制相关

    /**（主席）添加协作方*/
    @Request( name ="DCSAddOperatorReq",
            methodOwner = DcsCtrl,
			paras = TDCSOperator.class,
            rspSeq = {"DcsAddOperator_Rsp"})
    DCAddOperator, // TODO 待调

    /**添加协作方响应*/
    @Response(clz = TDCSResult.class,
				name = "DcsAddOperator_Rsp")
    DCAddOperatorRsp,

    /**（主席）删除协作方*/
    @Request( name = "DCSDelOperatorReq",
            methodOwner = DcsCtrl,
			paras = TDCSOperator.class,
            rspSeq = {"DcsDelOperator_Rsp"})
    DCDelOperator, // TODO 待调

    /**删除协作方响应*/
    @Response(clz = TDCSResult.class,
				name = "DcsDelOperator_Rsp")
    DCDelOperatorRsp,

    /**（自己）申请作为协作方*/
    @Request( name = "DCSApplyOperReq",
            methodOwner = DcsCtrl,
            paras = String.class, // 申请者的e164
            rspSeq = {"DcsApplyOper_Rsp"})
    DCApplyOperator,

    /**申请作为协作方响应*/
    @Response(clz = TDCSResult.class,
				name = "DcsApplyOper_Rsp")
    DCApplyOperatorRsp,

    /**（自己）取消作为协作方*/
    @Request( name = "DCSCancelOperReq",
            methodOwner = DcsCtrl,
            paras = String.class, // 申请者的e164
            rspSeq = {"DcsCancelOper_Rsp"})
    DCCancelOperator, // TODO 待调

    /**取消作为协作方响应*/
    @Response(clz = TDCSResult.class,
				name = "DcsCancelOper_Rsp")
    DCCancelOperatorRsp,

    /**（主席）拒绝成员申请作为协作方的请求*/
    @Request(name = "DCSRejectOperatorCmd",
            methodOwner = DcsCtrl,
				paras=TDCSOperator.class)
    DCRejectApplyOperator, // TODO 待调

    /**用户加入数据协作通知*/
    @Notification(clz = TDCSUserInfo.class,
            name = "DcsUserJoinConf_Ntf")
    DCUserJoinedNtf, // TODO 待调

    /**成员（向主席）申请协作权通知*/
    @Notification(clz = TDCSUserInfo.class,
            name = "DcsUserApplyOper_Ntf")
    DCApplyOperatorNtf, // TODO 待调

    /**（主席）添加协作方通知*/
    @Notification(clz = TDCSUserInfo.class,
            name = "DcsAddOperator_Ntf")
    DCOperatorAddedNtf, // TODO 待调
    /**（主席）删除协作方通知*/
    @Notification(clz = TDCSUserInfo.class,
            name = "DcsDelOperator_Ntf")
    DCOperatorDeletedNtf, // TODO 待调
    /**申请协作权被拒绝通知*/
    @Notification(clz = TDCSUserInfo.class,
            name = "DcsRejectOper_Ntf")
    DCApplyOperatorRejectedNtf, // TODO 待调


    /**获取数据协作会议中的所有成员（包括协作方普通方）*/
    @Request( name = "DCSGetUserListReq",   //参数：StringBuffer类型 e164
            methodOwner = DcsCtrl,
            paras = String.class,
            rspSeq = {"DcsGetUserList_Rsp"})
    DCQueryAllMembers, // TODO 待调

    /**获取数据协作会议中的所有成员响应*/
    @Response(	clz = DcsGetUserListRsp.class,
				name = "DcsGetUserList_Rsp")  // 需单独定义的响应结构体名加Result后缀，需单独定义的参数名加para后缀
    DCQueryAllMembersRsp,


    // 数据协作画板相关

    /**新建画板*/
    @Request( name = "DCSNewWhiteBoardReq",
            methodOwner = DcsCtrl,
			paras= TDCSNewWhiteBoard.class,
            rspSeq = {"DcsNewWhiteBoard_Rsp"},
            timeout = 5)
    DCNewBoard,

    /**新建画板响应*/
    @Response(	clz = DcsNewWhiteBoardRsp.class,
				name = "DcsNewWhiteBoard_Rsp")
    DCNewBoardRsp,

    /**删除画板*/
    @Request(	name = "DCSDelWhiteBoardReq",
            methodOwner = DcsCtrl,
            paras = {String.class, // 当前会议e164
                    String.class}, // 画板Id
            rspSeq = {"DcsDelWhiteBoard_Rsp"},
            timeout = 5)
    DCDelBoard,

    /**删除画板响应*/
    @Response(clz=TDCSBoardResult.class,
				name="DcsDelWhiteBoard_Rsp")
    DCDelBoardRsp,

    /**删除所有画板*/
    @Request(	name = "DCSDelAllWhiteBoardReq",
            methodOwner = DcsCtrl,
            paras = String.class, // 当前会议e164
            rspSeq = {"DcsDelAllWhiteBoard_Rsp"})
    DCDelAllBoard,

    /**删除所有画板响应*/
    @Response(clz=TDCSBoardResult.class,
            name="DcsDelAllWhiteBoard_Rsp")
    DCDelAllBoardRsp,

    /**切换画板*/
    @Request(	name = "DCSSwitchReq",
            methodOwner = DcsCtrl,
            paras = TDCSSwitchReq.class,
            rspSeq = {"DcsSwitch_Rsp"},
            timeout = 5)
    DCSwitchBoard,

    /**切换画板响应*/
    @Response(clz=DcsSwitchRsp.class,
            name="DcsSwitch_Rsp")
    DCSwitchBoardRsp,


    /**查询画板*/
    @Request(	name = "DCSGetWhiteBoardReq",
            methodOwner = DcsCtrl,
            paras = {String.class, // 会议e164
                    String.class}, // 画板id
            rspSeq = {"DcsGetWhiteBoard_Rsp"})
    DCQueryBoard, // TODO 待调

    /**查询画板响应*/
    @Response(	clz = DcsGetWhiteBoardRsp.class,
				name="DcsGetWhiteBoard_Rsp")
    DCQueryBoardRsp,

    /**查询所有画板*/
    @Request(	name = "DCSGetAllWhiteBoardReq", //参数：StringBuffer类型 e164
            methodOwner = DcsCtrl,
            paras = String.class,
            rspSeq = {"DcsGetAllWhiteBoard_Rsp"})
    DCQueryAllBoards,

    /**查询所有白板响应*/
    @Response(	clz = DcsGetAllWhiteBoardRsp.class,
				name="DcsGetAllWhiteBoard_Rsp")
    DCQueryAllBoardsRsp,

    /**添加子页*/
    @Request(	name = "DCSOperAddSubPageInfoCmd",
            methodOwner = DcsCtrl,
				paras = {TDCSOperReq.class, TDCSWbAddSubPageInfo.class})
    DCAddSubPage, // TODO 待调


    /** 当前画板通知。
     * 该通知仅用于加入数据协作时通知入会方当前画板信息，其他场景下不会上报。
     * 收到该通知后会从服务器拉取当前画板已有图元。
     * */
    @Notification(clz = TDCSBoardInfo.class,
					name = "DcsCurrentWhiteBoard_Ntf")
    DCCurrentBoardNtf,

    /**新建画板通知*/
    @Notification(clz = TDCSBoardInfo.class,
					name="DcsNewWhiteBoard_Ntf")
    DCBoardCreatedNtf,

    /**切换画板通知*/
    @Notification(clz = TDCSBoardInfo.class,
					name="DcsSwitch_Ntf")
    DCBoardSwitchedNtf,

    /**删除画板通知*/
    @Notification(clz = TDCSDelWhiteBoardInfo.class,
					name="DcsDelWhiteBoard_Ntf")
    DCBoardDeletedNtf,


    // 数据协作图元操作

    /**画线*/
    @Request(	name="DCSOperLineOperInfoCmd",
            methodOwner = DcsCtrl,
				paras={TDCSOperReq.class, TDCSWbLineOperInfo.class})
    DCDrawLine,

    /**画圆/椭圆*/
    @Request(	name="DCSOperCircleOperInfoCmd",
            methodOwner = DcsCtrl,
            paras={TDCSOperReq.class, TDCSWbCircleOperInfo.class})
    DCDrawOval,

    /**画矩形*/
    @Request(	name="DCSOperRectangleOperInfoCmd",
            methodOwner = DcsCtrl,
            paras={TDCSOperReq.class, TDCSWbRectangleOperInfo.class})
    DCDrawRect,

    /**画路径（铅笔操作）*/
    @Request(	name="DCSOperPencilOperInfoCmd",
            methodOwner = DcsCtrl,
            paras={TDCSOperReq.class, TDCSWbPencilOperInfo.class})
    DCDrawPath,

//    /**彩笔操作*/
//    @Deprecated
//    @Request(para=MsgBeans.DCSOperColorPenOper.class)
//    DCColorPenOp,

//    /**图片操作*/
//    @Deprecated
//    @Request(para=MsgBeans.DCSOperImageOper.class)
//    DCSOperImageOperInfoCmd,

    @Request(name = "DCSOperInsertPicCmd",
            methodOwner = DcsCtrl,
            paras = {TDCSOperReq.class, TDCSWbInsertPicOperInfo.class})
    DCInsertPic,
    @Request(name = "DCSOperPitchPicDelCmd",
            methodOwner = DcsCtrl,
            paras = {TDCSOperReq.class, TDCSWbDelPicOperInfo.class})
    DCDeletePic, // TODO 待调
    @Request(name = "DCSOperPitchPicDragCmd",
            methodOwner = DcsCtrl,
            paras = {TDCSOperReq.class, TDCSWbPitchPicOperInfo.class})
    DCDragPic, // TODO 待调

    /**黑板擦擦除*/
    @Request(	name="DCSOperReginEraseCmd",
            methodOwner = DcsCtrl,
            paras={TDCSOperReq.class, TDCSWbReginEraseOperInfo.class})
    DCErase,

    /**矩形擦除*/
    @Request(	name="DCSOperEraseOperInfoCmd",
            methodOwner = DcsCtrl,
            paras={TDCSOperReq.class, TDCSWbEraseOperInfo.class})
    DCRectErase,

    /**清屏*/
    @Request(	name="DCSOperClearScreenCmd",
            methodOwner = DcsCtrl,
				paras=TDCSOperReq.class)
    DCClearScreen,


    // 数据协作矩阵操作
    /**矩阵变换（放缩、位移等）*/
    @Request(	name="DCSOperFullScreenCmd",
            methodOwner = DcsCtrl,
            paras={TDCSOperReq.class, TDCSWbDisPlayInfo.class})
    DCMatrix,

    /**左旋转*/
    @Request(	name="DCSOperRotateLeftCmd",
            methodOwner = DcsCtrl,
				paras=TDCSOperReq.class)
    DCRotateLeft, // TODO 待调

    /**右旋转*/
    @Request(	name="DCSOperRotateRightCmd",
            methodOwner = DcsCtrl,
				paras=TDCSOperReq.class)
    DCRotateRight, // TODO 待调


    // 数据协作图元控制操作

    /**撤销*/
    @Request(	name="DCSOperUndoCmd",
            methodOwner = DcsCtrl,
            paras = {TDCSOperReq.class, TDCSWbTabPageIdInfo.class})
    DCUndo,

    /**恢复（恢复被撤销的操作）*/
    @Request(	name="DCSOperRedoCmd",
            methodOwner = DcsCtrl,
            paras = {TDCSOperReq.class, TDCSWbTabPageIdInfo.class})
    DCRedo,


    // 数据协作文件操作

    /**上传文件*/
    @Request(name = "DCSUploadFileCmd",
            methodOwner = DcsCtrl,
            paras = {BaseTypeString.class, // 下载url。XXX 下层龟腚上层先将url包装到该类里面转成json然后传给它，它再将json解析出来进而萃取出url。
                    TDCSFileInfo.class},
            rspSeq = {"DcsUploadFile_Ntf"},
            timeout = 30)
    DCUpload,

    /**上传文件响应*/
    @Response(	clz= TDCSFileLoadResult.class,
				name="DcsUploadFile_Ntf")
    DCUploadNtf,


    /**获取图片上传地址*/
    @Request(name = "DCSUploadImageReq",
            methodOwner = DcsCtrl,
            paras=TDCSImageUrl.class,
            rspSeq = {"DcsUploadImage_Rsp"})
    DCQueryPicUploadUrl,

    /**获取图片上传地址响应*/
    @Response(	clz = DcsUploadImageRsp.class,
				name="DcsUploadImage_Rsp")
    DCQueryPicUploadUrlRsp,


//    /**发布图片信息*/
//    @Deprecated
//    @Request
//    DCSSendImgFileInfoCmd, // 这个接口组件注销了



    /**下载（图元、图片等）*/
    @Request(	name ="DCSDownloadFileReq",
            methodOwner = DcsCtrl,
            paras = {BaseTypeString.class, // 下载url。XXX 下层龟腚上层先将url包装到该类里面转成json然后传给它，它再将json解析出来进而萃取出url。
                    TDCSFileInfo.class},
            rspSeq = {"DcsDownloadFile_Rsp"},
            timeout = 30)
    DCDownload,

    /**下载响应*/
    @Response(clz = TDCSFileLoadResult.class,
				name="DcsDownloadFile_Rsp")
    DCDownloadRsp,

    /**获取图片下载地址*/
    @Request(	name="DCSDownloadImageReq",
            methodOwner = DcsCtrl,
				paras=TDCSImageUrl.class,
            rspSeq = {"DcsDownloadImage_Rsp"})
    DCQueryPicUrl,

    /**获取下载图片地址响应*/
    @Response(	clz = DcsDownloadImageRsp.class,
				name ="DcsDownloadImage_Rsp")
    DCQueryPicUrlRsp,

    /**图片可下载通知*/
    @Notification(clz = TDCSImageUrl.class,
					name ="DcsDownloadImage_Ntf")
    DCPicDownloadableNtf,



    // 数据协作图元操作通知

//    /**图元序列开始通知。
//     * 注：新加入数据协作会议后，服务器会将当前数据协作会议中已存在的图元序列同步到新加入的与会方。*/
//    @Deprecated
//    @Notification(clz = MsgBeans.DCBoardId.class,
//					name ="DcsElementOperBegin_Ntf") //注意：组件没有处理此条消息
//    DCElementBeginNtf,

    /**画直线通知*/
    @Notification(clz = DcsOperLineOperInfoNtf.class,
					name="DcsOperLineOperInfo_Ntf")
    DCLineDrawnNtf,

    /**画圆/椭圆通知*/
    @Notification(clz = DcsOperCircleOperInfoNtf.class,
					name="DcsOperCircleOperInfo_Ntf")
    DCOvalDrawnNtf,

    /**画矩形通知*/
    @Notification(clz = DcsOperRectangleOperInfoNtf.class,
					name="DcsOperRectangleOperInfo_Ntf")
    DCRectDrawnNtf,

    /**画路径（铅笔操作）通知*/
    @Notification(clz = DcsOperPencilOperInfoNtf.class,
					name="DcsOperPencilOperInfo_Ntf")
    DCPathDrawnNtf,

//    /**彩笔操作通知*/
//    @Deprecated
//    @Notification(clz = MsgBeans.DcsOperColorPenOperInfo_Ntf.class)
//    DcsOperColorPenOperInfo_Ntf,

    /**图片插入通知*/
    @Notification(clz = DcsOperInsertPicNtf.class,
					name="DcsOperInsertPic_Ntf")
    DCPicInsertedNtf,

    /**图片拖动通知*/
    @Notification(clz = DcsOperPitchPicDragNtf.class,
					name="DcsOperPitchPicDrag_Ntf")
    DCPicDraggedNtf,

    /**图片删除通知*/
    @Notification(clz = DcsOperPitchPicDelNtf.class,
					name="DcsOperPitchPicDel_Ntf")
    DCPicDeletedNtf,

    /**黑板擦擦除通知*/
    @Notification(clz =DcsOperReginEraseNtf.class,
            name = "DcsOperReginErase_Ntf")
    DCErasedNtf,

    /**矩形擦除通知*/
    @Notification(clz = DcsOperEraseOperInfoNtf.class,
					name="DcsOperEraseOperInfo_Ntf")
    DCRectErasedNtf,

    /**全屏matrix操作通知（缩放、移动、旋转）*/
    @Notification(clz = DcsOperFullScreenNtf.class,
					name="DcsOperFullScreen_Ntf")
    DCFullScreenMatrixOpNtf,

    /**撤销操作通知*/
    @Notification(clz = DcsOperUndoNtf.class,
					name="DcsOperUndo_Ntf")
    DCUndoneNtf,

    /**恢复（恢复被撤销的操作）通知*/
    @Notification(clz = DcsOperRedoNtf.class,
					name="DcsOperRedo_Ntf")
    DCRedoneNtf,

    /**清屏通知*/
    @Notification(clz = TDCSOperContent.class,
					name="DcsOperClearScreen_Ntf")
    DCScreenClearedNtf,

    /**图元序列结束通知*/
    @Notification(clz = TDcsCacheElementParseResult.class,
					name="DcsElementOperFinal_Ntf")
    DCElementEndNtf,


    //<<<<<<<<<<<<<<<<<< 数据协作

}
