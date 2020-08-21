package com.kedacom.vconf.sdk.webrtc;

import com.kedacom.vconf.sdk.common.constant.EmConfProtocol;
import com.kedacom.vconf.sdk.common.constant.EmDcsConfMode;
import com.kedacom.vconf.sdk.common.constant.EmEndpointType;
import com.kedacom.vconf.sdk.common.constant.EmH264Profile;
import com.kedacom.vconf.sdk.common.constant.EmMeetingSafeType;
import com.kedacom.vconf.sdk.common.constant.EmMtAddrType;
import com.kedacom.vconf.sdk.common.constant.EmMtAliasType;
import com.kedacom.vconf.sdk.common.constant.EmMtMixType;
import com.kedacom.vconf.sdk.common.constant.EmMtOpenMode;
import com.kedacom.vconf.sdk.common.constant.EmMtResolution;
import com.kedacom.vconf.sdk.common.constant.EmVConfCreateType;
import com.kedacom.vconf.sdk.common.constant.EmVidFormat;
import com.kedacom.vconf.sdk.common.type.vconf.TMTConfMixInfo;
import com.kedacom.vconf.sdk.common.type.vconf.TMTDCSAttribute;
import com.kedacom.vconf.sdk.common.type.vconf.TMTInstanceConferenceInfo;
import com.kedacom.vconf.sdk.common.type.vconf.TMTInstanceCreateConference;
import com.kedacom.vconf.sdk.common.type.vconf.TMTInviteMember;
import com.kedacom.vconf.sdk.common.type.vconf.TMTVideoFormatList;
import com.kedacom.vconf.sdk.common.type.vconf.TMtAlias;
import com.kedacom.vconf.sdk.common.type.vconf.TMtCallLinkSate;
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

    static CreateConfResult callLinkState2CreateConfResult(TMtCallLinkSate tMtCallLinkSate) {
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

        return new CreateConfResult(e164, e164, alias, email, callBitRate);
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
        to.emMeetingtype = EmMeetingSafeType.emRestMeetingType_Sfu;

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
        TMTInviteMember inviteMember = new TMTInviteMember();
        inviteMember.achAccount = confPara.creatorE164;
        inviteMember.emAccountType = EmMtAddrType.emAddrE164;
        inviteMember.emProtocol = EmConfProtocol.emrtc;
        to.atInviteMembers = new ArrayList<>();
        to.atInviteMembers.add(inviteMember);
        if (null != confPara.initedConfMemberInfoList) {
            for (ConfMemberInfo mi : confPara.initedConfMemberInfoList) {
                inviteMember = new TMTInviteMember();
                if (null != mi.e164 && !mi.e164.trim().isEmpty()) {
                    inviteMember.achAccount = mi.e164;
                    inviteMember.emAccountType = EmMtAddrType.emAddrE164;
                } else if (null != mi.moid && !mi.moid.trim().isEmpty()) {
                    inviteMember.achAccount = mi.moid;
                    inviteMember.emAccountType = EmMtAddrType.emAddrMoid;
                } else if (null != mi.alias && !mi.alias.trim().isEmpty()) {
                    inviteMember.achAccount = mi.alias;
                    inviteMember.emAccountType = EmMtAddrType.emAddrAlias;
                } else {
                    continue;
                }
                to.atInviteMembers.add(inviteMember);
            }
        }
        to.dwIMemberNum = to.atInviteMembers.size();

        // 是否隐藏
        to.emSafeConf = confPara.bHide ? EmMtOpenMode.emMt_Hide : EmMtOpenMode.emMt_Open;
        to.achPassword = confPara.passwd;

        return to;
    }

    static ConfInfo tMTInstanceConferenceInfo2ConfInfo(TMTInstanceConferenceInfo ci){
        return new ConfInfo(ci.achName, ci.achConfID, ci.achStartTime, ci.achEndTime);
    }

}
