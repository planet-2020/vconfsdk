package com.kedacom.vconf.sdk.main.conf.bean;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Enum2CustomValueJsonAdapter;

/**
 * Created by Sissi on 2019/7/30
 */
@JsonAdapter(Enum2CustomValueJsonAdapter.class)
public enum EmConfLoginResultCode {

    emGKFailedBegin_Api(0),         ///<起始值
    emGKUnReachable_Api(1),         ///<对端不可达
    emInvalidAliase_Api(2),         ///<无效的别名
    emDupAlias_Api(3),              ///<别名重复
    emInvalidCallAddress_Api(4),    ///<无效的呼叫地址
    emResourceUnavailable_Api(5),   ///<资源不可用
    //emUnknown_Api = 6,               ///<未知原因,不用
    emRegNumberFull_Api(7),         ///<注册数量满，PCMT绑定GK失败消息提示
    emGKSecurityDenial_Api(8),      ///<GK注册权限失败
    emGKDismatch_Api(9),            ///<GK不是运营版本,服务器不匹配
    emUnRegGKReq_Api(10),           ///<GK被抢登后，要求注销GK
    emRRQCreateHRASFailed_Api(11),  ///<rrq创建句柄失败
    emRRQSendFailed_Api(12),        ///<rrq发送失败

    emSipFailedBegin_Api(50),        ///<sip注册失败原因开始
    emSipLocalNormalUnreg_Api(51),           ///<未注册
    emSipInvalidUserNameAndPassword_Api(52),  ///<无效的用户名和密码
    emSipRegistrarUnReachable_Api(53),       ///<注册不可达
    emSipInvalidAlias_Api(54),               ///<无效的别名
    emSipUnknownReason_Api(55),              ///<未知原因
    emSipRegisterFailed_Api(56),             ///<注册失败
    emSipRegisterNameDup_Api(57),            ///<注册名称重复

    /////sec 注册失败reason
    emSecCrtNotFind_Api(58),   ///ca证书找不到
    emSecCrtVerifyFail_Api(59),   ///证书验证失败
    emSecCrtExpired_Api(60),   ///证书过期 有效期错误
    emSecCrtFormatError_Api(61),   ///证书格式错误
    emSecLoadCertFailed_Api(62),

    emUnRegSuc_Api(90),                     ///< 取消注册成功
    emRegSuccess_Api(100);                   ///<注册成功， sip和323都是这个

    private final int value;
    EmConfLoginResultCode(int value){
        this.value = value;
    }

    public int getValue() {
        return value;
    }

}
