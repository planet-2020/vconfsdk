package com.kedacom.vconf.sdk.webrtc;

import com.kedacom.vconf.sdk.common.constant.EmDcsConfMode;
import com.kedacom.vconf.sdk.common.constant.EmH264Profile;
import com.kedacom.vconf.sdk.common.constant.EmMeetingSafeType;
import com.kedacom.vconf.sdk.common.constant.EmMtAliasType;
import com.kedacom.vconf.sdk.common.constant.EmMtMixType;
import com.kedacom.vconf.sdk.common.constant.EmMtResolution;
import com.kedacom.vconf.sdk.common.constant.EmVConfCreateType;
import com.kedacom.vconf.sdk.common.constant.EmVidFormat;
import com.kedacom.vconf.sdk.common.type.vconf.TMTConfMixInfo;
import com.kedacom.vconf.sdk.common.type.vconf.TMTDCSAttribute;
import com.kedacom.vconf.sdk.common.type.vconf.TMTInstanceCreateConference;
import com.kedacom.vconf.sdk.common.type.vconf.TMTVideoFormatList;
import com.kedacom.vconf.sdk.common.type.vconf.TMtAlias;
import com.kedacom.vconf.sdk.common.type.vconf.TMtCallLinkSate;
import com.kedacom.vconf.sdk.webrtc.bean.ConfPara;
import com.kedacom.vconf.sdk.webrtc.bean.MakeCallResult;
import com.kedacom.vconf.sdk.webrtc.bean.StreamInfo;
import com.kedacom.vconf.sdk.webrtc.bean.trans.TRtcStreamInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Sissi on 2019/11/14
 */
final class ToDoConverter {

    static MakeCallResult fromTransferObj(TMtCallLinkSate tMtCallLinkSate) {
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

        return new MakeCallResult(e164, alias, email, callBitRate);
    }

    static TMTInstanceCreateConference toTransferObj(ConfPara confPara) {
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

        if (!confPara.bAudio) {
            //主视频格式
            TMTVideoFormatList videoFormat = new TMTVideoFormatList();
            videoFormat.emVideoFormat = EmVidFormat.emVH264;
            videoFormat.emVideoProfile = EmH264Profile.emBaseline;
            videoFormat.emResolution = confPara.bHighDefinition ? EmMtResolution.emMtHD1080p1920x1080_Api : EmMtResolution.emMtHD720p1280x720_Api;
            videoFormat.dwRate = confPara.bHighDefinition ? 2048 : 1024;
            videoFormat.dwFrame = 30;
            List<TMTVideoFormatList> videoFormatLists = new ArrayList<>();
            videoFormatLists.add(videoFormat);
            to.atVideoFormatList = videoFormatLists;
            to.dwVFormatNum = to.atVideoFormatList.size();
        }

        return to;
    }

    static StreamInfo fromTransferObj(TRtcStreamInfo rtcStreamInfo) {
        int type;
        if (rtcStreamInfo.bAudio){
            type = StreamInfo.Type_Unknown;
        }else{
            if (rtcStreamInfo.bAss){
                type = StreamInfo.Type_RemoteScreenShare;
            }else{
                type = StreamInfo.Type_RemoteCamera;
            }
        }
        return new StreamInfo(rtcStreamInfo.tMtId.dwMcuId, rtcStreamInfo.tMtId.dwTerId, rtcStreamInfo.achStreamId, type);
    }

}
