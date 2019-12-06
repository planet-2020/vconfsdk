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
            rspSeq3 = {"Calling", "MultipartyConfStarted"},
            rspSeq4 = {"Calling", "MultipartyConfEnded"}
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
     * 设置播放参数
     */
    @Request(method = "SetRtcPlayCmd",
            owner = MethodOwner.MonitorCtrl,
            paras = StringBuffer.class,
            userParas = TRtcPlayParam.class,
            type = Request.SET)
    SetPlayPara,


    @Response(clz = TRtcStreamInfoList.class,
            id = "RtcStreamList_Ntf")
    StreamListReady,

    @Response(clz = TRtcStreamInfoList.class,
            id = "RtcStreamAdd_Ntf")
    StreamJoined,

    @Response(clz = TRtcStreamInfoList.class,
            id = "RtcStreamLeft_Ntf")
    StreamLeft,

    /**
     * 与会成员列表通知
     * */
    @Response(clz = TMTEntityInfoList.class,
            id = "OnLineTerListNtf")
    ConfMembersInfoNtf,


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
