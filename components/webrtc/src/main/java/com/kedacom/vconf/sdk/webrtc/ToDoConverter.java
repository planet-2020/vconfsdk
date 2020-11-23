package com.kedacom.vconf.sdk.webrtc;

import com.kedacom.vconf.sdk.common.type.transfer.EmConfProtocol;
import com.kedacom.vconf.sdk.common.type.transfer.EmDcsConfMode;
import com.kedacom.vconf.sdk.common.type.transfer.EmEndpointType;
import com.kedacom.vconf.sdk.common.type.transfer.EmH264Profile;
import com.kedacom.vconf.sdk.common.type.transfer.EmMeetingSafeType;
import com.kedacom.vconf.sdk.common.type.transfer.EmMtAddrType;
import com.kedacom.vconf.sdk.common.type.transfer.EmMtAliasType;
import com.kedacom.vconf.sdk.common.type.transfer.EmMtMixType;
import com.kedacom.vconf.sdk.common.type.transfer.EmMtOpenMode;
import com.kedacom.vconf.sdk.common.type.transfer.EmMtResolution;
import com.kedacom.vconf.sdk.common.type.transfer.EmVConfCreateType;
import com.kedacom.vconf.sdk.common.type.transfer.EmVidFormat;
import com.kedacom.vconf.sdk.common.type.transfer.TMTConfMixInfo;
import com.kedacom.vconf.sdk.common.type.transfer.TMTCreateConfMember;
import com.kedacom.vconf.sdk.common.type.transfer.TMTDCSAttribute;
import com.kedacom.vconf.sdk.common.type.transfer.TMTInstanceConferenceInfo;
import com.kedacom.vconf.sdk.common.type.transfer.TMTInstanceCreateConference;
import com.kedacom.vconf.sdk.common.type.transfer.TMTInviteMember;
import com.kedacom.vconf.sdk.common.type.transfer.TMTVideoFormatList;
import com.kedacom.vconf.sdk.common.type.transfer.TMtAlias;
import com.kedacom.vconf.sdk.common.type.transfer.TMtCallLinkSate;
import com.kedacom.vconf.sdk.common.type.transfer.TShortMsg;
import com.kedacom.vconf.sdk.webrtc.bean.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Sissi on 2019/11/14
 */
final class ToDoConverter {

    static MakeCallResult callLinkState2MakeCallResult(TMtCallLinkSate tMtCallLinkSate, boolean audioManner) {
        String e164=null, alias=null, email=null;
        int callBitRate = tMtCallLinkSate.dwCallRate;
        for (TMtAlias tMtAlias : tMtCallLinkSate.tPeerAlias.arrAlias){
            if (EmMtAliasType.emAliasE164 == tMtAlias.emAliasType){
                e164 = tMtAlias.achAlias;
            }else if (EmMtAliasType.emAliasH323 == tMtAlias.emAliasType){
                alias = tMtAlias.achAlias;
            }else if (EmMtAliasType.emAliasEmail == tMtAlias.emAliasType){
                email = tMtAlias.achAlias;
            }
            if (null!=e164 && null!=alias && null!=email){
                break;
            }
        }

        return new MakeCallResult(e164, alias, email, callBitRate, tMtCallLinkSate.emEndpointType == EmEndpointType.emEndpointTypeMT, audioManner);
    }

    static CreateConfResult callLinkState2CreateConfResult(TMtCallLinkSate tMtCallLinkSate, boolean isAudio, boolean selfJoinInAudioManner) {
        String e164=null, alias=null, email=null;
        int callBitRate = tMtCallLinkSate.dwCallRate;
        for (TMtAlias tMtAlias : tMtCallLinkSate.tPeerAlias.arrAlias){
            if (EmMtAliasType.emAliasE164 == tMtAlias.emAliasType){
                e164 = tMtAlias.achAlias;
            }else if (EmMtAliasType.emAliasH323 == tMtAlias.emAliasType){
                alias = tMtAlias.achAlias;
            }else if (EmMtAliasType.emAliasEmail == tMtAlias.emAliasType){
                email = tMtAlias.achAlias;
            }
            if (null!=e164 && null!=alias && null!=email){
                break;
            }
        }

        return new CreateConfResult(e164, e164, alias, email, callBitRate, isAudio, selfJoinInAudioManner);
    }

    static ConfInvitationInfo callLinkSate2ConfInvitationInfo(TMtCallLinkSate tMtCallLinkSate) {
        String e164=null, alias=null, email=null;
        int callBitRate = tMtCallLinkSate.dwCallRate;
        for (TMtAlias tMtAlias : tMtCallLinkSate.tPeerAlias.arrAlias){
            if (EmMtAliasType.emAliasE164 == tMtAlias.emAliasType){
                e164 = tMtAlias.achAlias;
            }else if (EmMtAliasType.emAliasH323 == tMtAlias.emAliasType){
                alias = tMtAlias.achAlias;
            }else if (EmMtAliasType.emAliasEmail == tMtAlias.emAliasType){
                email = tMtAlias.achAlias;
            }
            if (null!=e164 && null!=alias && null!=email){
                break;
            }
        }

        return new ConfInvitationInfo(e164, alias,
                EmEndpointType.emEndpointTypeMT == tMtCallLinkSate.emEndpointType,
                callBitRate);
    }


    static TMTInstanceCreateConference confPara2CreateConference(ConfPara confPara) {
        TMTInstanceCreateConference to = new TMTInstanceCreateConference();
        to.achName = confPara.confName;
        to.dwDuration = confPara.duration;
        to.dwBitrate = confPara.bAudio ? 64 : 4 * 1024;
        // 数据协作
        TMTDCSAttribute tDCSAttr = new TMTDCSAttribute();
        tDCSAttr.emDCSMode = confPara.enableDC ? EmDcsConfMode.emConfModeAuto : EmDcsConfMode.emConfModeStop;
        to.tDCSAttr = tDCSAttr;
        // 虚拟会议
        if (null != confPara.virtualConfId && !confPara.virtualConfId.isEmpty()){
            to.emVConfCreateType = EmVConfCreateType.emCreateVirtualConf;
            to.achVConfId = confPara.virtualConfId;
        }
        // 混音
        TMTConfMixInfo tmtConfMixInfo = new TMTConfMixInfo();
        tmtConfMixInfo.emMode = EmMtMixType.mcuWholeMix_Api;
        to.tMix = tmtConfMixInfo;
        // 会议类型
        to.emMeetingtype = ConfType2EmMeetingSafeType(confPara.confType);

        //主视频格式
        List<TMTVideoFormatList> videoFormatLists = new ArrayList<>();
        TMTVideoFormatList videoFormat = new TMTVideoFormatList();
        if (confPara.bAudio) {
            videoFormat.dwRate = 64;
        }else{
            videoFormat.emVideoFormat = EmVidFormat.emVH264;
            videoFormat.emVideoProfile = EmH264Profile.emBaseline;
            videoFormat.emResolution = confPara.bHighDefinition ? EmMtResolution.emMtHD1080p1920x1080_Api : EmMtResolution.emMtHD720p1280x720_Api;
            videoFormat.dwRate = 4 * 1024;
            videoFormat.dwFrame = 30;
        }
        videoFormatLists.add(videoFormat);
        to.atVideoFormatList = videoFormatLists;
        to.dwVFormatNum = to.atVideoFormatList.size();

        // 参会成员
        to.atInviteMembers = new ArrayList<>();
        to.atInviteMembers.add(ConfMemberInfo2TMTInviteMember(confPara.creator));
        if (null != confPara.initedConfMemberInfoList) {
            for (ConfMemberInfo mi : confPara.initedConfMemberInfoList) {
                to.atInviteMembers.add(ConfMemberInfo2TMTInviteMember(mi));
            }
        }
        to.dwIMemberNum = to.atInviteMembers.size();

        to.tChairman = ConfMemberInfo2TMTCreateConfMember(confPara.creator);

        // 是否隐藏
        to.emSafeConf = confPara.bHide ? EmMtOpenMode.emMt_Hide : EmMtOpenMode.emMt_Open;
        to.achPassword = confPara.passwd;

        return to;
    }

    static TMTInviteMember ConfMemberInfo2TMTInviteMember(ConfMemberInfo mi){
        TMTInviteMember inviteMember = new TMTInviteMember();
        inviteMember.emProtocol = EmConfProtocol.emrtc;
        if (null != mi.e164 && !mi.e164.trim().isEmpty()) {
            inviteMember.achAccount = mi.e164;
            inviteMember.emAccountType = EmMtAddrType.emAddrE164;
        } else if (null != mi.moid && !mi.moid.trim().isEmpty()) {
            inviteMember.achAccount = mi.moid;
            inviteMember.emAccountType = EmMtAddrType.emAddrMoid;
        } else if (null != mi.alias && !mi.alias.trim().isEmpty()) {
            inviteMember.achAccount = mi.alias;
            inviteMember.emAccountType = EmMtAddrType.emAddrAlias;
        } else if (null != mi.email && !mi.email.trim().isEmpty()) {
            inviteMember.achAccount = mi.email;
            inviteMember.emAccountType = EmMtAddrType.emAddrAlias;
        }else {
            return null;
        }
        return inviteMember;
    }

    static TMTCreateConfMember ConfMemberInfo2TMTCreateConfMember(ConfMemberInfo mi){
        return new TMTCreateConfMember(mi.e164, mi.moid, EmMtAddrType.emAddrMoid);
    }

    static ConfInfo tMTInstanceConferenceInfo2ConfInfo(TMTInstanceConferenceInfo ci){
        return new ConfInfo(ci.achName, ci.achConfID, ci.achStartTime, ci.achEndTime);
    }

    static ConfManSMS TShortMsg2ConfManSMS(TShortMsg shortMsg){
        return new ConfManSMS(shortMsg.achText);
    }

    static EmMeetingSafeType ConfType2EmMeetingSafeType(ConfType confType){
        switch (confType){
            case TRADITIONAL:
                return EmMeetingSafeType.emRestMeetingType_Public;
            case PORT:
                return EmMeetingSafeType.emRestMeetingType_Port;
            case RTC:
                return EmMeetingSafeType.emRestMeetingType_Sfu;
            case MIX:
                return EmMeetingSafeType.emRestMeetingType_Mix_Api;
            case AUTO:
                return EmMeetingSafeType.emRestMeetingType_Auto_Api;
            default:
                return EmMeetingSafeType.emRestMeetingType_Auto_Api;
        }
    }
}
