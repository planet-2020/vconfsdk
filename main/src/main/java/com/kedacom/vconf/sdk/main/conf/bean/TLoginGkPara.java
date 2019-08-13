package com.kedacom.vconf.sdk.main.conf.bean;

/**
 * Created by Sissi on 2019/7/30
 */
public class TLoginGkPara {
    public boolean bUsedCSU; // 是否使用GK
    public String achDomain; // GK域名
    public long dwIp; // GKIp (网络序)
    public String achNumber; // 注册时使用的号码
    public String achPassword; // 密码
    public int dwPort; // < GK端口， 如果不填，标准是1720， 非标的是用平台返回的值
    //该字段主要表现为:网络异常的情况下 终端的注册rrq gk没有回复 协议超时上报unreachable 超时的时间
    //界面可以不设置 组件默认为20s
    public int dwTimeToLive;   ///< GK保活超时时间 单位(s)
}
