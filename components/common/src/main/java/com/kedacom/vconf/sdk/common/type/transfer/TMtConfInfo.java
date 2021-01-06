package com.kedacom.vconf.sdk.common.type.transfer;

public class TMtConfInfo {
	public TMTTime tStartTime; // 开始时间，控制台填0为立即开始
	public int wDuration; // 持续时间(分钟)，0表示不自动停止
	public int wBitRate; // 会议码率(单位:Kbps,1K=1024)
	public int wSecBitRate; // 双速会议的第2码率(单位:Kbps,为0表示是单速会议)
	public EmMtResolution emPriVideoResolution; // 主视频格式
	public EmMtResolution emAssVideoResolution; // 辅视频格式
	public int dwTalkHoldTime; // 最小发言持续时间(单位:秒)

	public String achConfPwd; // 会议密码
	public String achConfName; // 会议名
	public String achConfE164; // 会议的E164号码

	public TMtId tChairman; // 主席终端，MCU号为0表示无主席
	public TMtId tSpeaker; // 发言终端，MCU号为0表示无发言人
	public TMtVmpParamApi tVmpParam; // 当前视频复合参数，仅视频复合时有效
	public TMtMixParam tMixParam; // mix参数
	public boolean bIsSatdcastMode; // 是否支持卫星分散会议：0-不支持，1-支持
	public EmMtOpenMode emOpenMode; // 会议开放方式:
	public boolean bOccupy_Vpu; // 是否占用图像处理（包括画面合成和适配）能力
	public EmMtDualMode emDual_mode; // 发言模式
	public boolean bAllInitDumb; // 终端入会后是否初始哑音 0-不哑音 1-哑音
	public boolean bConfNoDisTurb; // /< 会议是否是 免打扰模式
	public boolean bPortMode; // /< 是否是端口模式会议
	public EmRecordState emRecord_mode;    // 录像模式，0-不录像 1-录像 2-录像暂停
	public boolean             bForceBroadcast;                                                 ///<  是否是强制广播
	public EmMeetingSafeType emConfType;                                       ///< 会议类型 ，对应会管会议类型
	public boolean             bWaterMark;                                         ///< 会议水印
	public boolean             bIsBanMtCancelMute;                                 ///< 全场哑音下是否允许终端自己取消哑音
}
