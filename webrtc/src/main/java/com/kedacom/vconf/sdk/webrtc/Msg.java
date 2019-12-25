package com.kedacom.vconf.sdk.webrtc;

import com.kedacom.vconf.sdk.annotation.Message;
import com.kedacom.vconf.sdk.annotation.Request;
import com.kedacom.vconf.sdk.annotation.Response;
import com.kedacom.vconf.sdk.common.constant.EmConfProtocol;
import com.kedacom.vconf.sdk.common.constant.EmMtCallDisReason;
import com.kedacom.vconf.sdk.common.type.BaseTypeInt;
import com.kedacom.vconf.sdk.common.type.vconf.TMTInstanceCreateConference;
import com.kedacom.vconf.sdk.common.type.vconf.TMtAssVidStatusList;
import com.kedacom.vconf.sdk.common.type.vconf.TMtCallLinkSate;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TCreateConfResult;
import com.kedacom.vconf.sdk.webrtc.bean.trans.*;

/**
 * Created by Sissi on 2019/10/24
 */
@Message(
        module = "RTC"
)
enum Msg {
    /**获取Rtc服务器地址*/
    @Request(method = "GetRtcSvrCfg",
            owner = MethodOwner.ConfigCtrl,
            paras = StringBuffer.class,
            userParas = TMtRtcSvrAddr.class,
            type = Request.GET)
    GetSvrAddr,

    /**
     * 登录/登出Rtc服务器
     */
    @Request(method = "SetRtcSvrCfgCmd",
            owner = MethodOwner.ConfigCtrl,
            paras = StringBuffer.class,
            userParas = TMtRtcSvrAddr.class, // 登录：TMtRtcSvrAddr.bUsedRtc==true，登出：=false
            rspSeq = "RegisterRsp")
    Register,

    /**
     * 登录/登出Rtc服务器响应
     */
    @Response(clz = TLoginResult.class,
            id = "RegResultNtf")
    RegisterRsp,


    /**
     * 呼出
     * */
    @Request(method = "ConfMakeCallCmd",
            owner = MethodOwner.ConfCtrl,
            paras = {StringBuffer.class, int.class, int.class},
            userParas = {
                    String.class, // 对端e164（点对点）/ 会议号（多点）
                    int.class,  // 呼叫码率
                    EmConfProtocol.class   // 协议类型
            },
            timeout = 60,
            rspSeq = {"Calling", "MultipartyConfStarted"},
            rspSeq2 = {"Calling", "ConfCanceled"},
            rspSeq3 = {"Calling", "MultipartyConfEnded"}
            )
    Call,

    /**
     * 正在呼出中
     * */
    @Response(clz = TMtCallLinkSate.class,
            id = "ConfCallingNtf")
    Calling,

    /**
     * 多方会议已开始
     * */
    @Response(clz = TMtCallLinkSate.class,
            id = "MulConfStartedNtf")
    MultipartyConfStarted,

    /**
     * 多方会议已结束
     * */
    @Response(clz = BaseTypeInt.class,
            id = "MulConfEndedNtf")
    MultipartyConfEnded,

    /**
     * 会议取消
     * */
    @Response(clz = BaseTypeInt.class,
            id = "ConfCanceledNtf")
    ConfCanceled,

    /**
     * 呼入通知
     * */
    @Response(clz = TMtCallLinkSate.class,
            id = "ConfInComingNtf")
    CallIncoming,


    /**
     * 创建会议
     * */
    @Request(method = "MGRestCreateConferenceReq",
            owner = MethodOwner.MeetingCtrl,
            paras = StringBuffer.class,
            userParas = TMTInstanceCreateConference.class,
            timeout = 60,
            rspSeq = {"CreateConfRsp", // 创会成功与否。创会成功后平台会拉终端入会
                    "MultipartyConfStarted",  // 终端（己端）被成功拉入会议
            }
    )
    CreateConf,

    /**
     * 创建会议响应
     * */
    @Response(clz = TCreateConfResult.class,
            id = "RestCreateConference_Rsp")
    CreateConfRsp,

    /**
     * 退出会议
     * */
    @Request(method = "ConfHangupConfCmd",
            owner = MethodOwner.ConfCtrl,
            paras = int.class,
            userParas = EmMtCallDisReason.class,
            rspSeq = "MultipartyConfEnded"
    )
    QuitConf,

    /**
     * 结束会议
     * */
    @Request(method = "ConfEndConfCmd",
            owner = MethodOwner.ConfCtrl,
            rspSeq = "MultipartyConfEnded"
    )
    EndConf,

    /**
     * 接受入会邀请
     * */
    @Request(method = "ConfAcceptCmd",
            owner = MethodOwner.ConfCtrl,
            timeout = 60,
            rspSeq = "MultipartyConfStarted"
    )
    AcceptInvitation,

    /**
     * 拒绝入会邀请
     * */
    @Request(method = "ConfRejectConfCmd",
            owner = MethodOwner.ConfCtrl,
            rspSeq = {} //TODO
    )
    DeclineInvitation,



    /**
     * 当前会议中已有与会成员列表通知
     * 入会后会收到一次该通知
     * */
    @Response(clz = TMTEntityInfoList.class,
            id = "OnLineTerListNtf")
    CurrentConfereeList,

    /**
     * 与会方加入通知
     * 入会以后，会议中有新的与会方加入则会收到该通知。
     * */
    @Response(clz = TMTEntityInfo.class,
            id = "TerJoin_Ntf")
    ConfereeJoined,

    /**
     * 与会方退出通知
     * 入会以后，会议中有与会方离会则会收到该通知
     * */
    @Response(clz = TMtId.class,
            id = "TerLeft_Ntf")
    ConfereeLeft,



    /**
     * 当前会议中已有音视频流列表通知
     * 入会后会收到一次该通知
     * NOTE: 平台过来的Stream概念上对应的是WebRTC里面的Track
     * */
    @Response(clz = TRtcStreamInfoList.class,
            id = "RtcStreamList_Ntf")
    CurrentStreamList,

    /**
     * 流加入通知
     * 入会以后，会议中有新的流加入则会收到该通知。
     * */
    @Response(clz = TRtcStreamInfoList.class,
            id = "RtcStreamAdd_Ntf")
    StreamJoined,

    /**
     * 流退出通知
     * 入会以后，会议中有流退出则会收到该通知
     * NOTE: 对比{@link #ConfereeLeft}，StreamLeft只表示音/视频离会了，而ConfereeLeft表示与会方退会了（当然相应的音视频也退出了）。
     * 比如某个与会方关闭了摄像头停止了视频发布，则其他与会方会收到StreamLeft，但不会收到ConfereeLeft。
     * 如果某个与会方退会了则其他与会方会收到ConfereeLeft和StreamLeft。
     * */
    @Response(clz = TRtcStreamInfoList.class,
            id = "RtcStreamLeft_Ntf")
    StreamLeft,

    /**
     * 选择想要订阅的码流
     */
    @Request(method = "SetRtcPlayCmd",
            owner = MethodOwner.MonitorCtrl,
            paras = StringBuffer.class,
            userParas = TRtcPlayParam.class,
            type = Request.SET)
    SelectStream,



    /**获取流列表*/
    @Request(method = "GetRtcStreamList",
            owner = MethodOwner.MonitorCtrl,
            paras = StringBuffer.class,
            userParas = TRtcStreamInfoList.class,
            type = Request.GET)
    GetStreamList,

    /**
     * 获取流数量
     */
    @Request(method = "GetRtcStreamListNum",
            owner = MethodOwner.MonitorCtrl,
            paras = StringBuffer.class,
            userParas = int.class,
            type = Request.GET)
    GetStreamCount,



    /**
     * 静音
     */
    @Request(method = "AudQuiteLocalSpeakerCmd",
            owner = MethodOwner.AudioCtrl,
            paras = boolean.class)
    SetSilence,


    /**
     * 哑音
     */
    @Request(method = "AudMuteLocalMicCmd",
            owner = MethodOwner.AudioCtrl,
            paras = boolean.class)
    SetMute,



    /**
     * 开启/关闭桌面共享（双流）
     * */
    @Request(method = "VideoAssStreamCmd",
            paras = boolean.class,
            owner = MethodOwner.MonitorCtrl,
            rspSeq = "ToggleScreenShareRsp"
    )
    ToggleScreenShare,

    /**
     * 开启/关闭桌面共享（双流）响应
     * */
    @Response(clz = TMtAssVidStatusList.class,
            id = "AssSndSreamStatusNtf")
    ToggleScreenShareRsp,



    END;

    private static class MethodOwner {
        private static final String PKG = "com.kedacom.kdv.mt.mtapi.";

        private static final String MonitorCtrl = PKG + "MonitorCtrl";
        private static final String ConfigCtrl = PKG + "ConfigCtrl";
        private static final String ConfCtrl = PKG + "ConfCtrl";
        private static final String MeetingCtrl = PKG + "MeetingCtrl";
        private static final String AudioCtrl = PKG + "AudioCtrl";
    }

}
