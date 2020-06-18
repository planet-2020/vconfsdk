package com.kedacom.vconf.sdk.base.login;

import android.app.Application;
import android.content.Context;
import android.os.Handler;

import androidx.annotation.NonNull;

import com.kedacom.vconf.sdk.amulet.Caster;
import com.kedacom.vconf.sdk.amulet.IResultListener;
import com.kedacom.vconf.sdk.base.login.bean.transfer.*;
import com.kedacom.vconf.sdk.common.bean.transfer.TSrvStartResult;
import com.kedacom.vconf.sdk.common.type.TRestErrorInfo;
import com.kedacom.vconf.sdk.utils.log.KLog;
import com.kedacom.vconf.sdk.utils.net.NetAddrHelper;

import java.util.Collections;
import java.util.Map;

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

    @Override
    protected Map<Msg[], NtfProcessor<Msg>> subscribeNtfs() { // TODO Msg改为Startup的看能否编译报错
        return super.subscribeNtfs();
    }

    // 启动业务组件接入服务
    private void startService(){
        String serviceName = "rest";
        req(Msg.StartMtService, new SessionProcessor<Msg>() {
            @Override
            public boolean onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas) {
                TSrvStartResult result = (TSrvStartResult) rspContent;
                boolean success = result.MainParam.basetype && result.AssParam.achSysalias.equals(reqParas[0]);
                if (success){
                    KLog.p("start %s service success!", serviceName);
                }
                return true;
            }
        }, null , serviceName);
    }

    /**
     * 登录Aps服务器
     * */
    public void loginAps(@NonNull String ip, @NonNull String account, @NonNull String pwd, IResultListener resultListener){
        long ipLong;
        try {
            ipLong = NetAddrHelper.ipStr2LongLittleEndian(ip);
        } catch (NetAddrHelper.InvalidIpv4Exception e) {
            e.printStackTrace();
            reportFailed(-1, resultListener);
            return;
        }
        MtXAPSvrCfg mtXAPSvrCfg = new MtXAPSvrCfg(
                EmServerAddrType.emSrvAddrTypeCustom.ordinal(),
                ip,
                "",
                ipLong,
                true,
                60090   // 端口暂时写死
        );

        // 配置Aps
        req(Msg.SetApsServerCfg, new SessionProcessor<Msg>() {
                @Override
                public boolean onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas) {
                    // 登录Aps
                    req(Msg.LoginAps, new SessionProcessor<Msg>() {
                            @Override
                            public boolean onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas) {
                                TApsLoginResult apsLoginResult = (TApsLoginResult) rspContent;
                                if (apsLoginResult.MainParam.bSucess){
                                    // 获取平台分配的token
                                    req(Msg.QueryAccountToken, new SessionProcessor<Msg>() {
                                        @Override
                                        public boolean onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas) {
                                            TRestErrorInfo restErrorInfo = (TRestErrorInfo) rspContent;
                                            if (restErrorInfo.dwErrorID == 1000){
                                                // 登录platform
                                                req(Msg.LoginPlatform, new SessionProcessor<Msg>() {
                                                    @Override
                                                    public boolean onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas) {
                                                        TLoginPlatformRsp res = (TLoginPlatformRsp) rspContent;
                                                        if (res.MainParam.dwErrorID == 1000){
                                                            reportSuccess(null, resultListener);
                                                        }else{
                                                            reportFailed(-1, resultListener);
                                                        }
                                                        return true;
                                                    }
                                                }, resultListener, new TMTWeiboLogin(account, pwd));
                                            }else{
                                                reportFailed(-1, resultListener);
                                            }
                                            return true;
                                        }
                                    }, resultListener, NetAddrHelper.ipLongLittleEndian2Str(apsLoginResult.AssParam.dwIP));

                                }else{
                                    reportFailed(-1, resultListener);
                                }
                                return true;
                            }
                        },
                        resultListener, new TMTApsLoginParam(account, pwd, "", "Skywalker_Ali", "")
                    );

                    return true;
                }

            },
            resultListener, new MtXAPSvrListCfg(0, Collections.singletonList(mtXAPSvrCfg))
        );

    }


    /**
     * 登出Aps服务器
     * */
    public void logoutAps(){

    }


}
