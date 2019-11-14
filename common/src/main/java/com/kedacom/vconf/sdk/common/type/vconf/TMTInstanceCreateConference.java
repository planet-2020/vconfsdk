package com.kedacom.vconf.sdk.common.type.vconf;

import com.kedacom.vconf.sdk.common.constant.EmCallMode;
import com.kedacom.vconf.sdk.common.constant.EmClosedMeeting;
import com.kedacom.vconf.sdk.common.constant.EmEncryptArithmetic;
import com.kedacom.vconf.sdk.common.constant.EmMeetingSafeType;
import com.kedacom.vconf.sdk.common.constant.EmMtDualMode;
import com.kedacom.vconf.sdk.common.constant.EmMtFecMode;
import com.kedacom.vconf.sdk.common.constant.EmMtOpenMode;
import com.kedacom.vconf.sdk.common.constant.EmRestCascadeMode;
import com.kedacom.vconf.sdk.common.constant.EmVConfCreateType;
import com.kedacom.vconf.sdk.common.constant.EmVideoQuality;

import java.util.List;

public class TMTInstanceCreateConference {
    public String achName;        ///< 会议名称
    public int dwDuration;                                ///< 时长
    public int dwBitrate;                                ///< 会议码率
    public EmClosedMeeting emCloseConf;                            ///< 会议免打扰，1开启，0关闭
    public EmMtOpenMode emSafeConf;                                ///< 会议安全

    public String achPassword;    ///< 密码
    public EmEncryptArithmetic emEncryptedtype;                        ///< 传输加密类型
    public EmMeetingSafeType emMeetingtype;                          ///< 1 端口会议   0 传统媒体会议

    public int dwCallTimes;                            ///< 呼叫次数
    public int dwCallInterval;                         ///< 呼叫间隔(秒)

    public boolean bInitmute;                              ///< 初始化哑音  1是，0否
    public boolean bInitSilence;                           ///< 初始化静音1是，0否

    public EmVideoQuality emVidoQuality;                          ///< 视频质量,0：质量优先；1：速度优先
    public String achEncryptedkey; ///< AES加密KEY
    public EmMtDualMode emDualmode;                                ///< 双流设置

    public boolean bVoiceActivityDetection;                ///< 是否开启语言激励，1是，0否
    public int dwVacinterval;                             ///< 语音激励敏感度(s)，支持5、15、30、60
    public EmRestCascadeMode emCascadeMode;                          ///< 级联模式,0简单级联，1合并级联
    public boolean bCascadeUpload;                         ///< 是否级联上传:1是，0否
    public boolean bCascadeReturn;                         ///< 是否级联回传:1是，0否
    public int dwCascadeReturnPara;                    ///< 级联回传带宽参数

    public int dwMaxJoinMt;                            ///< 最大与会终端数
    public boolean bAutoEnd;                               ///< 是否自动结会：1是，0否
    public boolean bPreoccpuyResouce;                      ///< 预占资源 0-不预占 1-预占
    public TMTCreateConfMember tSpeaker;     //发言人
    public TMTCreateConfMember tChairman;   //主席
    public boolean bPublicConf;                            ///< 是否公共会议室  1是，0否
    public TMTConfMixInfo tMix;                                   ///< 混音信息
    public int dwVFormatNum;
    public List<TMTVideoFormatList> atVideoFormatList;     ///< 主视频格式列表

    public int dwIMemberNum;
    public List<TMTInviteMember> atInviteMembers;     ///< 参会成员

    public TMTConfVMPInfomation tVmp;//画面合成设置

    public int dwVipNum;
    public List<TMTCreateConfMember> atViplist;            ///< VIP账号

    public TMTConfPollInfomation tPoll;                                          ///轮询设置
    public boolean bEncryptedAuth;       ///是否双向认证
    public EmCallMode emCallMode;       ///呼叫模式
    public TMTRecordAttribute tRecordAttr; ///录像设置
    public TMTDCSAttribute tDCSAttr;  ///数据协作
    public EmVConfCreateType emVConfCreateType; // 创会类型，如果不填则是默认的实时会议。
    public String achVConfId;       // 根据虚拟会议室ID创会（emVConfCreateType填emCreateVirtualConf）；根据模板ID创会（emVConfCreateType填emCreateConfByTemplate）
    public EmMtFecMode emFecMode;   // FEC开关
    public boolean bDoubleFlow;     // 成为发言人后立即发起内容共享，默认为0-否
    public boolean bMuteFilter;     // 是否开启全场哑音例外，默认为0-不开启
    public List<TMTCreateConfMember> atKeepCallingMembers;  // 追呼成员数组（暂定2个）
    public int dwKeepCallingMemberNum;  // 追呼成员个数


}