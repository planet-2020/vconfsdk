package com.kedacom.vconf.sdk.common.type.vconf;

import com.kedacom.vconf.sdk.common.constant.EmConfProtocol;
import com.kedacom.vconf.sdk.common.constant.EmEndpointType;
import com.kedacom.vconf.sdk.common.constant.EmMtCallDisReason;
import com.kedacom.vconf.sdk.common.constant.EmMtCallState;
import com.kedacom.vconf.sdk.common.constant.EmMtCallingType;
import com.kedacom.vconf.sdk.common.constant.EmMtModel;
import com.kedacom.vconf.sdk.common.constant.EmSipConnectType;
import com.kedacom.vconf.sdk.common.type.TMTTime;
import com.kedacom.vconf.sdk.common.type.TNetAddr;

public class TMtCallLinkSate {

    public EmMtCallingType emCallingType; // /<呼叫类型
    public EmMtCallState emCallState; // 呼叫状态
    public EmConfProtocol emConfProtocol; // 呼叫协议

    public boolean bIsCaller; // TRUE = 主叫 FALSE=被叫
    public int dwCallRate; // 呼叫码率。注意P2P音频会议中的dwCallRate根据主呼终端的呼叫码率来定的，不一定是64K。
    public int dwCallUpRate; // 带宽检测，调整后获得的最小的可用上行的码率
    public int dwCallDownRate; // 带宽检测，调整后获得的最小的可用下行的码率
    public EmMtCallDisReason emCallDisReason; // 呼叫挂断原因

    public TNetAddr tPeerAddr; // 对端IP地址(网络序)
    public EmMtModel emPeerModel; // 对端型号
    public int dwPeerVendor; // /< 对端厂商
    public String achPeerProductId; // 对端型号
    public EmEndpointType emEndpointType;// < 对端类型，mt,或者mcu
    public TMultMtAlias tPeerAlias; // 对端别名，可以多个alias, e164
    public TMTTime tConfEstablishedTime; // /< 会议成功建立的时间， 是个本地时间
    public String achPeerVersionId;      ///< 对端VersionId
    public boolean bIsPeerStackAfter5p0; //< 判断对端是否是5.0

    EmSipConnectType emSipConnect;                               ///< sip连接类型
    public boolean bQtCall;                                    ///< 是否是量通呼叫
    public boolean bPeerSptSubMtlist;                          ///< 对端是否支持订阅终端列表

    @Override
    public String toString() {
        return "TMtCallLinkSate{" +
                "emCallingType=" + emCallingType +
                ", emCallState=" + emCallState +
                ", emConfProtocol=" + emConfProtocol +
                ", bIsCaller=" + bIsCaller +
                ", dwCallRate=" + dwCallRate +
                ", dwCallUpRate=" + dwCallUpRate +
                ", dwCallDownRate=" + dwCallDownRate +
                ", emCallDisReason=" + emCallDisReason +
                ", tPeerAddr=" + tPeerAddr +
                ", emPeerModel=" + emPeerModel +
                ", dwPeerVendor=" + dwPeerVendor +
                ", achPeerProductId='" + achPeerProductId + '\'' +
                ", emEndpointType=" + emEndpointType +
                ", tPeerAlias=" + tPeerAlias +
                ", tConfEstablishedTime=" + tConfEstablishedTime +
                ", achPeerVersionId='" + achPeerVersionId + '\'' +
                ", bIsPeerStackAfter5p0=" + bIsPeerStackAfter5p0 +
                ", emSipConnect=" + emSipConnect +
                ", bQtCall=" + bQtCall +
                ", bPeerSptSubMtlist=" + bPeerSptSubMtlist +
                '}';
    }
}