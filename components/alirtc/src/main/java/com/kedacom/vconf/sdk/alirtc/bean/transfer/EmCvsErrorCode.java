package com.kedacom.vconf.sdk.alirtc.bean.transfer;

import com.kedacom.vconf.sdk.utils.json.EnumCustomValueStrategy;

@EnumCustomValueStrategy
public enum EmCvsErrorCode {
    emCvsUnKnown_Api(30301),                  //初始值
    emCvsInternalServer_Api(30302),           //服务器内部错误
    emCvsInputInvaild_Api(30303),             //参数有误
    emCvsWriteFileFail_Api(30304),            //写文件失败
    emCvsTimeOut_Api(30305),                  //超时
    emCvsYunAnomaly_Api(30306),               //阿里云服务器异常
    emCvsUserInvalid_Api(30307),              //用户错误
    emCvsPwdError_Api(30308),                 //密码错误
    emCvsRedisAnomaly_Api(30309),             //redis读写异常
    emCvsClientNoAuth_Api(30310),             //ws客户端没有权限验证
    emCvsTokenInvaild_Api(30311),             //无效的token
    emCvsUserLogged_Api(30312),               //用户已经登录
    emCvsUserNoPermission_Api(30313),         //权限不足,拒绝访问
    emCvsUserUnLogged_Api(30314),             //用户未登录
    emCvsUserLoginRemote_Api(30315),          //抢登(其他地方登陆)
    emCvsUserDisconnect_Api(30316),           //用户断链
    emCvsUserExpired_Api(30317),              //硬终端用户过期
    emCvsUserJoinOtherConf_Api(30318),        //用户加入其他会议
    emCvsClientIsReg_Api(30319),             //ws链路已经注册
    emCvsConfExist_Api(30321),                //会议已经存在
    emCvsConfNoAbility_Api(30322),            //服务器会议能力不足
    emCvsConfUnExist_Api(30323),              //会议不存在
    emCvsConfNotVirRoom_Api(30324),           //不是专属会议室
    emCvsDomainmemsLimit_Api(30325),          //用户域入会数达到上限
    emCvsConfNoInSider_Api(30326),            //会议中没有内部人员
    emCvsConfPwdError_Api(30327),             //会议密码错误
    emCvsConfMemberLimit_Api(30328),          //会议人数达到上限
    emCvsConfNotHold_Api(30329),              //预约会议未召开
    emCvsConfUserInConf_Api(30330),           //用户已经在会议中
    emCvsConfUserNotInConf_Api(30331),        //用户不在会议中
    emCvsConfIsOver_Api(30332),               //会议已经结束
    emCvsConfInComingForbid_Api(30333),       //未被允许呼入当前会议
    emCvsConfServiceExpired_Api(30334),       //固定方数会议室服务到期
    emCvsConfStartForbid_Api(30335),          //未被允许使用会议室发起会议
    emCvsConfPwdErrorLimit_Api(30336),        //输错密码次数过多 请等待5分钟后重试
    emCvsConfPwdMissing_Api(30337),           //会议需要密码
    emCvsConfNoPermInLockConf_Api(30338),     //无权进入锁定会议

    end(987654321);

    public int value;

    EmCvsErrorCode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

}
