package com.kedacom.vconf.sdk.webrtc;

import androidx.annotation.NonNull;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * Rtc相关结果码
 * Created by Sissi on 2020/1/6
 */
public final class RtcResultCode {
    static final int Failed = -1; // 未知错误
    static final int Success = 0; // 成功
    static final int LoggedIn = 1; // 已登录
    static final int LoggedOut = 2; // 已注销

    static final int InnerCodeUpperBound = 100;

    public static final int IncorrectConfPassword = InnerCodeUpperBound+1;  // 会议密码错误
    public static final int ConfereeNumReachLimit = InnerCodeUpperBound+2;  // 与会方人数达上限
    public static final int InstantConfDenied = InnerCodeUpperBound+3;      // 平台会议审批功能已开启，不能创建即时会议，只能预约创会。
    public static final int NotLoggedInYet = InnerCodeUpperBound+4;          // 尚未成功登录
    public static final int NetworkUnreachable = InnerCodeUpperBound+5;     // 网络不可达
    public static final int AlreadyLoggedIn = InnerCodeUpperBound+6;         // 已登录（重复登录）
    public static final int UnknownServerAddress = InnerCodeUpperBound+7;   // 未知服务器
    public static final int ConfNeedPassword = InnerCodeUpperBound+8;   // 会议需要密码
    public static final int InConfAlready = InnerCodeUpperBound+9;     // （尝试入会时）已在会议中。可能同一个帐号登录了多个设备，其他设备已入会。
    public static final int NoSuchConf = InnerCodeUpperBound+10;     // 会议不存在

    /**
     * 错误码映射关系：
     * 消息    :   原始错误码   :   面向用户的错误码
     * */
    private static Table<Msg, Object, Integer> resultCodes = HashBasedTable.create();

    static {
        resultCodes.put(Msg.LoginStateChanged, 100, LoggedIn); // 既可作为响应也可作为通知成功结果码命名采用动词分词
        resultCodes.put(Msg.LoginStateChanged, 72, UnknownServerAddress);
        resultCodes.put(Msg.LoginStateChanged, 73, NetworkUnreachable);
        resultCodes.put(Msg.LoginStateChanged, 77, AlreadyLoggedIn);
        resultCodes.put(Msg.LoginStateChanged, 90, LoggedOut);
        resultCodes.put(Msg.CreateConfRsp, 1000, Success); // 纯响应的结果码成功命名Success
        resultCodes.put(Msg.CreateConfRsp, 99997, InstantConfDenied);
        resultCodes.put(Msg.ConfCanceled, 40, NotLoggedInYet);
        resultCodes.put(Msg.ConfCanceled, 39, ConfereeNumReachLimit);
        resultCodes.put(Msg.ConfCanceled, 59, InConfAlready);
        resultCodes.put(Msg.ConfCanceled, 62, NoSuchConf);
        resultCodes.put(Msg.QueryConfInfoRsp, 1000, Success); // 纯响应的结果码成功命名Success
    }

    public static int trans(@NonNull Msg msg, int rawResultCode){
        Object userResultCode = resultCodes.row(msg).get(rawResultCode);
        if (null == userResultCode) return Failed;
        return (int)userResultCode;
    }

}
