package com.kedacom.vconf.sdk.base.login;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;

import com.kedacom.vconf.sdk.amulet.Caster;
import com.kedacom.vconf.sdk.amulet.IResultListener;
import com.kedacom.vconf.sdk.base.login.bean.UserDetails;
import com.kedacom.vconf.sdk.base.login.bean.transfer.*;
import com.kedacom.vconf.sdk.common.bean.transfer.TSrvStartResult;
import com.kedacom.vconf.sdk.common.type.TRestErrorInfo;
import com.kedacom.vconf.sdk.utils.log.KLog;
import com.kedacom.vconf.sdk.utils.net.NetAddrHelper;

import java.util.Collections;

/**
 * Created by Sissi on 2019/7/19
 */
public class LoginManager extends Caster<Msg> {
    private static LoginManager instance = null;
    private Context context;

    private LoginManager(Context ctx) {
        context = ctx;
    }

    public synchronized static LoginManager getInstance(Application ctx) {
        if (instance == null) {
            instance = new LoginManager(ctx);
            instance.startService();
        }
        return instance;
    }

    // 启动业务组件接入服务
    private void startService(){
        String serviceName = "rest";
        req(Msg.StartMtService, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                TSrvStartResult result = (TSrvStartResult) rspContent;
                boolean success = result.MainParam.basetype && result.AssParam.achSysalias.equals(serviceName);
                if (success){
                    KLog.p("start %s service success!", serviceName);
                }
            }
        }, null , serviceName);
    }


    /**
     * 登录APS
     * NOTE: APS为接入服务器，登录APS后才能登录其他服务器如会议、IM、升级等。
     * */
    public void loginAps(@NonNull String apsIp, @NonNull String username, @NonNull String password, IResultListener resultListener){
        long ipLong;
        try {
            ipLong = NetAddrHelper.ipStr2LongLittleEndian(apsIp);
        } catch (NetAddrHelper.InvalidIpv4Exception e) {
            e.printStackTrace();
            reportFailed(-1, resultListener);
            return;
        }
        MtXAPSvrCfg mtXAPSvrCfg = new MtXAPSvrCfg(
                EmServerAddrType.emSrvAddrTypeCustom.ordinal(),
                apsIp,
                "",
                ipLong,
                true,
                60090   // 端口暂时写死
        );

        // 配置Aps
        req(Msg.SetApsServerCfg, new SessionProcessor<Msg>() {
                @Override
                public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                    // 登录Aps
                    req(Msg.LoginAps, new SessionProcessor<Msg>() {
                            @Override
                            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                                TApsLoginResult apsLoginResult = (TApsLoginResult) rspContent;
                                if (apsLoginResult.MainParam.bSucess){
                                    // 获取平台分配的token
                                    req(Msg.QueryAccountToken, new SessionProcessor<Msg>() {
                                        @Override
                                        public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                                            TRestErrorInfo restErrorInfo = (TRestErrorInfo) rspContent;
                                            if (restErrorInfo.dwErrorID == 1000){
                                                // 登录platform
                                                req(Msg.LoginPlatform, new SessionProcessor<Msg>() {
                                                    @Override
                                                    public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                                                        TLoginPlatformRsp res = (TLoginPlatformRsp) rspContent;
                                                        if (res.MainParam.dwErrorID == 1000){
                                                            reportSuccess(null, resultListener);
                                                        }else{
                                                            logoutAps(null);
                                                            reportFailed(-1, resultListener);
                                                        }
                                                    }
                                                }, resultListener, new TMTWeiboLogin(username, password));
                                            }else{
                                                logoutAps(null);
                                                reportFailed(-1, resultListener);
                                            }
                                        }
                                    }, resultListener, NetAddrHelper.ipLongLittleEndian2Str(apsLoginResult.AssParam.dwIP));

                                }else{
                                    reportFailed(-1, resultListener);
                                }
                            }
                        },
                        resultListener, new TMTApsLoginParam(username, password, "", "Skywalker_Ali", "")
                    );
                }

            },
            resultListener, new MtXAPSvrListCfg(0, Collections.singletonList(mtXAPSvrCfg))
        );

    }


    /**
     * 注销APS
     * */
    public void logoutAps(IResultListener resultListener){
        req(Msg.LogoutAps, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                TMtSvrState[] states = ((TMtSvrStateList) rspContent).arrSvrState;
                boolean got = false;
                for (TMtSvrState state : states){
                    if (EmServerType.emAPS == state.emSvrType
                            && EmServerState.emSrvIdle == state.emSvrState){
                        got = true;
                        break;
                    }
                }
                if (got) {
                    reportSuccess(null, resultListener);
                }else {
                    isConsumed[0] = false; // 该条消息不是我们期望的，继续等待后续消息
                }
            }
        }, resultListener);
    }


    /**
     * 查询用户详情
     * @param resultListener
     *          成功反馈：{@link UserDetails}
     *          失败反馈 errorcode
     * */
    public void queryUserDetails(@NonNull IResultListener resultListener){
        TMTUserInfoFromAps userBrief = (TMTUserInfoFromAps) get(Msg.GetUserBrief);
        if (null == userBrief){
            reportFailed(-1, resultListener);
            return;
        }
        String username = userBrief.achE164;
        req(Msg.QueryUserDetails, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                TQueryUserDetailsRsp detailsRsp = (TQueryUserDetailsRsp) rspContent;
                if (1000 == detailsRsp.MainParam.dwErrorID){
                    UserDetails userDetails = ToDoConverter.fromTransferObj(detailsRsp.AssParam);
                    reportSuccess(userDetails, resultListener);
                }else{
                    reportFailed(-1, resultListener);
                }
            }
        }, resultListener, new TMTAccountManagerSystem(username));
    }

}
