package com.kedacom.vconf.sdk.common.type.vconf;

import com.kedacom.vconf.sdk.common.constant.EmCallMode;
import com.kedacom.vconf.sdk.common.constant.EmClosedMeeting;
import com.kedacom.vconf.sdk.common.constant.EmEncryptArithmetic;
import com.kedacom.vconf.sdk.common.constant.EmMeetingSafeType;
import com.kedacom.vconf.sdk.common.constant.EmMtDualMode;
import com.kedacom.vconf.sdk.common.constant.EmMtOpenMode;
import com.kedacom.vconf.sdk.common.constant.EmRestCascadeMode;
import com.kedacom.vconf.sdk.common.constant.EmVideoQuality;

import java.util.List;

public class TMTInstanceConferenceInfo {
    public String achName;
    public String achConfID;
    public EmMeetingSafeType emConfType;                          ///< 1 端口会议   0 传统媒体会议
    public String achStartTime;
    public String achEndTime;
    public int dwDuration;
    public int dwBitrate;
    public EmClosedMeeting emCloseConf;                            ///< 会议免打扰，1开启，0关闭
    public EmMtOpenMode emSafeConf;
    public EmEncryptArithmetic emEncryptedtype;
    public int dwCallTimes;
    public int dwCallInterval;
    public boolean bInitmute;
    public boolean bInitSilence;
    public EmVideoQuality emVidoQuality;
    public String achEncryptedkey;
    public EmMtDualMode emDualmode;
    public boolean bPublicConf;
    public boolean bAutoEnd;
    public boolean bPreoccpuyResouce;
    public int dwMaxJoinMt;
    public boolean bVoiceInspireEnable;
    public TMTConfInitiator tConfInitiator;
    public int dwVFormatNum;
    public List<TMTVideoFormatList> atVideoFormatList;     ///< 主视频格式列表

    public boolean bVmpEnable;
    public boolean bMixEnable;
    public boolean bPollEnable;
    public boolean bNeedPassword;

    public EmRestCascadeMode emCascadeMode;
    public boolean bCascadeUpload;
    public boolean bCascadeReturn;
    public int dwCascadeReturnPara;
    public int dwVacinterval;
    public int dwConfLevel;

    public boolean bEncryptedAuth;
    public boolean bForceBroadcast;
    public EmCallMode emCallMode;

}