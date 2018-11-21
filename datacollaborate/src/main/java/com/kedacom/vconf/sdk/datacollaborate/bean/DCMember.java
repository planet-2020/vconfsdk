/**
 * 数据协作成员信息
 * */

package com.kedacom.vconf.sdk.datacollaborate.bean;

import com.kedacom.vconf.sdk.base.MsgBeans;

public class DCMember {
    public String e164;
    public String name;
    public TerminalType type; // 类型
    public boolean isOperator; // 是否协作方
    public boolean isChairman; // 是否主席
//    public boolean isOnline;
    /**
     * 转为传给下层时需要使用的类型
     * */
    public MsgBeans.TDCSConfUserInfo convert(){
        MsgBeans.TDCSConfUserInfo confUserInfo = new MsgBeans.TDCSConfUserInfo();
        confUserInfo.achE164 = e164;
        confUserInfo.achName = name;
        confUserInfo.emMttype = type.convert();
        confUserInfo.bIsOper = isOperator;
        confUserInfo.bIsConfAdmin = isChairman;
        return confUserInfo;
    }
}
