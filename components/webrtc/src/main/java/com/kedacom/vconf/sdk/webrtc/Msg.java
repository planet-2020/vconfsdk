package com.kedacom.vconf.sdk.webrtc;

import com.kedacom.vconf.sdk.annotation.Module;
import com.kedacom.vconf.sdk.annotation.Notification;
import com.kedacom.vconf.sdk.annotation.Request;
import com.kedacom.vconf.sdk.annotation.Response;
import com.kedacom.vconf.sdk.common.bean.transfer.TMtEntityStatus;
import com.kedacom.vconf.sdk.common.bean.transfer.TRegResultNtf;
import com.kedacom.vconf.sdk.common.bean.transfer.TSrvStartResult;
import com.kedacom.vconf.sdk.common.constant.EmConfProtocol;
import com.kedacom.vconf.sdk.common.constant.EmMtCallDisReason;
import com.kedacom.vconf.sdk.common.type.BaseTypeBool;
import com.kedacom.vconf.sdk.common.type.BaseTypeInt;
import com.kedacom.vconf.sdk.common.type.EmAPIVersionType;
import com.kedacom.vconf.sdk.common.type.vconf.TMTEntityInfo;
import com.kedacom.vconf.sdk.common.type.vconf.TMTEntityInfoList;
import com.kedacom.vconf.sdk.common.type.vconf.TMTInstanceCreateConference;
import com.kedacom.vconf.sdk.common.type.vconf.TMtAssVidStatusList;
import com.kedacom.vconf.sdk.common.type.vconf.TMtCallLinkSate;
import com.kedacom.vconf.sdk.common.type.vconf.TMtId;
import com.kedacom.vconf.sdk.common.type.vconf.TMtIdList;
import com.kedacom.vconf.sdk.common.type.vconf.TMtSimpConfInfo;
import com.kedacom.vconf.sdk.common.type.vconf.TShortMsg;
import com.kedacom.vconf.sdk.webrtc.bean.trans.*;

import static com.kedacom.vconf.sdk.annotation.Request.*;

/**
 * Created by Sissi on 2019/10/24
 */
@Module(
        name = "RTC"
)
enum Msg {

    /**
     * 启动业务组件服务
     * */
    @Request(name = "SYSStartService",
            owner = MtServiceCfgCtrl,
            paras = StringBuffer.class,
            userParas = String.class,
            rspSeq = "StartMtServiceRsp"
    )
    StartMtService,

    @Response(name = "SrvStartResultNtf",
            clz = TSrvStartResult.class)
    StartMtServiceRsp,

    /**获取Rtc服务器地址*/
    @Request(name = "GetRtcSvrCfg",
            owner = ConfigCtrl,
            paras = StringBuffer.class,
            userParas = TMtRtcSvrAddr.class,
            outputParaIndex = LastIndex
    )
    GetSvrAddr,

    /**
     * 登录Rtc服务器
     */
    @Request(name = "SetRtcSvrCfgCmd",
            owner = ConfigCtrl,
            paras = StringBuffer.class,
            userParas = TMtRtcSvrAddr.class, // 登录：TMtRtcSvrAddr.bUsedRtc==true，登出：=false
            timeout = 30,
            rspSeq = "LoginStateChanged"
    )
    Login,

    /**
     * 注销Rtc服务器
     */
    @Request(name = "SetRtcSvrCfgCmd",
            owner = ConfigCtrl,
            paras = StringBuffer.class,
            userParas = TMtRtcSvrAddr.class, // 登录：TMtRtcSvrAddr.bUsedRtc==true，登出：=false
            rspSeq = "LoginStateChanged"
    )
    Logout,

    /**
     * 登录状态变更
     */
    @Notification
    @Response(name = "RegResultNtf", clz = TRegResultNtf.class)
    LoginStateChanged,


    /**
     * 呼出
     * */
    @Request(name = "ConfMakeCallCmd",
            owner = ConfCtrl,
            paras = {StringBuffer.class, int.class, int.class},
            userParas = {
                    String.class, // 对端e164（点对点）/ 会议号（多点）
                    int.class,  // 呼叫码率
                    EmConfProtocol.class   // 协议类型
            },
            timeout = 60,
//            rspSeq = {"Calling", "MultipartyConfStarted"}, // 并非所有会议均需要密码，但是如果定义此消息序列会对下面的消息序列造成“短路”，
                                                             // 进而使得需要密码的情形无法正常处理。所以我们注掉此消息序列转而使用如下“全量“的消息序列
                                                             // 对于没有密码的情形，用户可能需要手动处理，否则会话会一直等待ConfPasswordNeeded直到超时。
            rspSeq2 = {"Calling", "MultipartyConfStarted", "ConfPasswordNeeded"},
            rspSeq3 = {"Calling", "ConfCanceled"}
            )
    Call,

    /**
     * 正在呼出中
     * */
    @Response(clz = TMtCallLinkSate.class,
            name = "ConfCallingNtf")
    Calling,

    /**
     * 多方会议已开始
     * */
    @Response(clz = TMtCallLinkSate.class, name = "MulConfStartedNtf")
    MultipartyConfStarted,

    /**
     * 多方会议已结束
     * */
    @Notification
    @Response(clz = BaseTypeInt.class, // EmMtCallDisReason
            name = "MulConfEndedNtf")
    MultipartyConfEnded,

    /**
     * 会议已取消
     * */
    @Notification
    @Response(clz = BaseTypeInt.class, // EmMtCallDisReason
            name = "ConfCanceledNtf")
    ConfCanceled,

    /**
     * 呼入通知
     * */
    @Notification(clz = TMtCallLinkSate.class, name = "ConfInComingNtf")
    CallIncoming,

    /**
     * 获取平台接口版本
     * */
    @Request(name = "MGGetAPIVersionReq",
            owner = MeetingCtrl,
            paras = int.class,
            userParas = EmAPIVersionType.class,
            rspSeq = "GetAPIVersionRsp"
    )
    GetAPIVersion,

    @Response(name = "GetAPIVersion_Rsp",
            clz = TAPIVersion.class)
    GetAPIVersionRsp,

    /**
     * 创建会议
     * */
    @Request(name = "MGRestCreateConferenceReq",
            owner = MeetingCtrl,
            paras = StringBuffer.class,
            userParas = TMTInstanceCreateConference.class,
            timeout = 60,
            rspSeq = {"CreateConfRsp", // 创会成功与否。创会成功后平台会拉终端入会
                    "MultipartyConfStarted",  // 终端（己端）被成功拉入会议
            },
            rspSeq2 = {"CreateConfRsp",
                    "ConfCanceled",
            }
    )
    CreateConf,

    /**
     * 创建会议响应
     * */
    @Response(clz = TCreateConfResult.class,
            name = "RestCreateConference_Rsp")
    CreateConfRsp,

    /**
     * 退出会议
     * */
    @Request(name = "ConfHangupConfCmd",
            owner = ConfCtrl,
            paras = int.class,
            userParas = EmMtCallDisReason.class,
            rspSeq = "MultipartyConfEnded"
    )
    QuitConf,

    /**
     * 结束会议
     * */
    @Request(name = "ConfEndConfCmd",
            owner = ConfCtrl,
            rspSeq = "MultipartyConfEnded"
    )
    EndConf,

    /**
     * 接受入会邀请
     * */
    @Request(name = "ConfAcceptCmd",
            owner = ConfCtrl,
            timeout = 60,
            rspSeq = "MultipartyConfStarted"
    )
    AcceptInvitation,

    /**
     * 拒绝入会邀请
     * */
    @Request(name = "ConfRejectConfCmd",
            owner = ConfCtrl,
            rspSeq = {} //TODO
    )
    DeclineInvitation,


    @Notification(name = "SimpleConfInfo_Ntf",
            clz = TMtSimpConfInfo.class)
    BriefConfInfoArrived,


    /**
     * 此与会方标识已分配
     * 入会后平台会为每个与会方分配标识。
     * */
    @Response(clz = TMtId.class,
            name = "TerLabelNtf")
    MyLabelAssigned,


    /**
     * 当前会议中已有与会成员列表通知
     * NOTE: 入会后会收到一次该通知，创会者也会收到这条消息。
     * */
    @Notification(clz = TMTEntityInfoList.class,
            name = "OnLineTerListNtf")
    CurrentConfereeList,

    /**
     * 与会方加入通知
     * 入会以后，会议中有其他与会方加入则会收到该通知。
     * */
    @Notification(clz = TMTEntityInfo.class,
            name = "TerJoin_Ntf")
    ConfereeJoined,

    /**
     * 与会方退出通知
     * 入会以后，会议中有其他与会方离会则会收到该通知
     * */
    @Notification(clz = TMtId.class,
            name = "TerLeft_Ntf")
    ConfereeLeft,



    /**
     * 当前会议中已有音视频流列表通知
     * NOTE:
     * 加入会议后会收到一次该通知
     * 创会者不会收到这条消息。
     * 平台过来的Stream概念上对应的是WebRTC里面的Track
     * */
    @Notification(clz = TRtcStreamInfoList.class,
            name = "RtcStreamList_Ntf")
    CurrentStreamList,

    /**
     * 流加入通知
     * 入会以后，会议中有其他与会方的流加入则会收到该通知。
     * NOTE: 己端不会收到自己的流joined的消息。
     * */
    @Notification(clz = TRtcStreamInfoList.class,
            name = "RtcStreamAdd_Ntf")
    StreamJoined,

    /**
     * 流退出通知
     * 入会以后，会议中有其他与会方的流退出则会收到该通知
     * NOTE:
     * 己端不会收到自己的流left的消息。
     * 对比{@link #ConfereeLeft}，StreamLeft只表示音/视频离会了，而ConfereeLeft表示与会方退会了（当然相应的音视频也退出了）。
     * 比如某个与会方关闭了摄像头停止了视频发布，则其他与会方会收到StreamLeft，但不会收到ConfereeLeft。
     * 如果某个与会方退会了则其他与会方会收到ConfereeLeft和StreamLeft。
     * */
    @Notification(clz = TRtcStreamInfoList.class,
            name = "RtcStreamLeft_Ntf")
    StreamLeft,

    /**
     * 选择想要订阅的视频码流
     */
    @Request(name = "SetRtcPlayCmd",
            owner = MonitorCtrl,
            paras = StringBuffer.class,
            userParas = TRtcPlayParam.class)
    SelectStream,



    /**获取流列表*/
    @Request(name = "GetRtcStreamList",
            owner = MonitorCtrl,
            paras = StringBuffer.class,
            userParas = TRtcStreamInfoList.class,
            outputParaIndex = LastIndex
    )
    GetStreamList,

    /**
     * 获取流数量
     */
    @Request(name = "GetRtcStreamListNum",
            owner = MonitorCtrl,
            paras = StringBuffer.class,
            userParas = int.class,
            outputParaIndex = LastIndex
    )
    GetStreamCount,



    /**
     * 静音
     */
    @Request(name = "AudQuiteLocalSpeakerCmd",
            owner = AudioCtrl,
            paras = boolean.class,
            userParas = boolean.class,
            rspSeq = "SelfSilenceStateChanged"
    )
    SetSilence,


    /**
     * 哑音
     */
    @Request(name = "AudMuteLocalMicCmd",
            owner = AudioCtrl,
            paras = boolean.class,
            userParas = boolean.class,
            rspSeq = "SelfMuteStateChanged"
    )
    SetMute,

    /**
     * （管理员）哑音其他与会方
     */
    @Request(name = "ConfSetTerMuteCmd",
            owner = ConfCtrl,
            paras = {StringBuffer.class, boolean.class},
            userParas = {TMtId.class, boolean.class},
            rspSeq = "OtherConfereeStateChanged"
    )
    SetMuteOther,


    /**
     * 设置全场哑音
     * */
    @Request(name = "ConfSetConfDumb",
            owner = ConfCtrl,
            paras = boolean.class,  // 是否哑音。true哑音，false取消哑音
            userParas = boolean.class,
            rspSeq = "ConfSettingsModified"
    )
    SetMuteMeeting,


    /**
     * 开启/关闭桌面共享（双流）
     * */
    @Request(name = "VideoAssStreamCmd",
            paras = boolean.class,
            userParas = boolean.class,
            owner = MonitorCtrl,
            rspSeq = "ToggleScreenShareRsp"
    )
    ToggleScreenShare,

    /**
     * 开启/关闭桌面共享（双流）响应
     * */
    @Response(clz = TMtAssVidStatusList.class,
            name = "AssSndSreamStatusNtf")
    ToggleScreenShareRsp,



    /**
     * 查询会议详情
     * */
    @Request(name = "MGRestGetInstantConfInfoByIDReq",
            owner = MeetingCtrl,
            paras = StringBuffer.class,  // 会议e164号
            userParas = String.class,
            timeout = 10,
            rspSeq = "QueryConfInfoRsp"
    )
    QueryConfInfo,

    /**
     * 查询会议详情响应
     * */
    @Response(clz = TQueryConfInfoResult.class,
            name = "RestGetInstantConfInfoByID_Rsp")
    QueryConfInfoRsp,

    /**
     * 此会议需要密码
     * */
    @Notification
    @Response(clz = Void.class, name = "McReqTerPwdNtf")
    ConfPasswordNeeded,

    /**
     * 验证会议密码
     * */
    @Request(name = "ConfVerifyConfPwdCmd",
            owner = ConfCtrl,
            paras = StringBuffer.class,  // 会议密码
            userParas = String.class,
            timeout = 10,
            rspSeq = "MyLabelAssigned",     // 验证通过
            rspSeq2 = "ConfPasswordNeeded"  // 验证失败
    )
    VerifyConfPassword,

    /**
     * 关闭己端主流
     * */
    @Request(name = "MainVideoOff",
            owner = ConfCtrl
    )
    CloseMyMainVideoChannel,

    /**
     * 己端静音状态变更
     * */
    @Notification
    @Response(name = "CodecQuietNtf",
            clz = BaseTypeBool.class // true已静音
    )
    SelfSilenceStateChanged,

    /**
     * 己端哑音状态变更
     * */
    @Notification
    @Response(name = "CodecMuteNtf",
            clz = BaseTypeBool.class // true已哑音
    )
    SelfMuteStateChanged,

    /**
     * 其他与会方状态变更
     * */
    @Response(name = "GetTerStatusNtf", clz = TMtEntityStatus.class)
    @Notification
    OtherConfereeStateChanged,

    /**
     * 会议即将结束通知
     * */
    @Notification(name = "ConfWillEndNtf",
            clz = BaseTypeInt.class // 剩余时长。单位：分钟
    )
    ConfAboutToEnd,

    /**
     * 延长会议
     * */
    @Request(name = "ConfProlongConfTimeCmd",
            owner = ConfCtrl,
            paras = int.class,  // 需要延长的时长。单位：分钟
            userParas = int.class,
            rspSeq = "ConfProlonged"
    )
    ProlongConf,

//    @Response(name = "ProlongResultNtf",
//            clz = BaseTypeBool.class)
//    ProlongConfRsp,


    /**
     * 会议已被延长
     * */
    @Response(name = "ConfDelayNtf",
            clz = BaseTypeInt.class // 延长后的会议时长。单位：分钟
    )
    @Notification
    ConfProlonged,

    /**
     * 会议设置变更通知
     * */
    @Response(name = "ModifyConfResultNtf",
            clz = TConfSettingsModified.class
    )
    @Notification
    ConfSettingsModified,

    /**
     * 主持人变更通知
     * */
    @Notification(name = "ChairPosNtf",
            clz = TMtId.class
    )
    PresenterChanged,

    /**
     * 主讲人变更通知
     * */
    @Notification(name = "SpeakerPosNtf",
            clz = TMtId.class
    )
    KeynoteSpeakerChanged,

    /**
     * VIP列表变更通知
     * */
    @Notification(name = "VipList_Ntf",
            clz = TMtIdList.class
    )
    VIPsChanged,

    /**
     * 会管短消息到达
     * */
    @Notification(name = "SMSNtf",
            clz = TShortMsg.class
    )
    ConfManSMSArrived,

}
