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
     * 登录Rtc服务器
     */
    @Request(method = "SetRtcSvrCfgCmd",
            owner = MethodOwner.ConfigCtrl,
            paras = StringBuffer.class,
            userParas = TMtRtcSvrAddr.class,
            rspSeq = "LoginRsp")
    Login,

    /**
     * 登录Rtc服务器响应
     */
    @Response(clz = TLoginResult.class,
            id = "RegResultNtf")
    LoginRsp,


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
            rspSeq = {"Calling", "P2pConfStarted"},
            rspSeq2 = {"Calling", "P2pConfEnded"},
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
     * 点对点会议已开始
     * */
    @Response(clz = TMtCallLinkSate.class,
            id = "P2PStartedNtf")
    P2pConfStarted,

    /**
     * 点对点会议已结束
     * */
    @Response(clz = BaseTypeInt.class,
            id = "P2PEndedNtf")
    P2pConfEnded,

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
            rspSeq = {"CreateConfRsp", // 创会成功与否。创会成功后平台会拉终端入会
                    "P2pConfStarted",  // 终端（己端）被成功拉入会议
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
            rspSeq = {} //TODO
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
            rspSeq = "P2pConfStarted",
            rspSeq2 = "MultipartyConfStarted"
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

    END;

    private static class MethodOwner {
        private static final String PKG = "com.kedacom.kdv.mt.mtapi.";

        private static final String MonitorCtrl = PKG + "MonitorCtrl";
        private static final String ConfigCtrl = PKG + "ConfigCtrl";
        private static final String ConfCtrl = PKG + "ConfCtrl";
        private static final String MeetingCtrl = PKG + "MeetingCtrl";
    }

}
