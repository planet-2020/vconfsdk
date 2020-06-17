package com.kedacom.vconf.sdk.base.startup;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;

import com.annimon.stream.Stream;
import com.annimon.stream.function.Consumer;
import com.kedacom.kdv.mt.mtapi.IMtcCallback;
import com.kedacom.vconf.sdk.amulet.Caster;
import com.kedacom.vconf.sdk.amulet.CrystalBall;
import com.kedacom.vconf.sdk.amulet.IResultListener;
import com.kedacom.vconf.sdk.base.startup.bean.*;
import com.kedacom.vconf.sdk.base.startup.bean.transfer.*;
import com.kedacom.vconf.sdk.common.bean.TerminalType;
import com.kedacom.vconf.sdk.common.constant.EmMtModel;
import com.kedacom.vconf.sdk.common.type.BaseTypeBool;
import com.kedacom.vconf.sdk.common.type.TRestErrorInfo;
import com.kedacom.vconf.sdk.utils.log.KLog;
import com.kedacom.vconf.sdk.utils.net.NetAddrHelper;
import com.kedacom.vconf.sdk.utils.net.NetworkHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by Sissi on 2019/7/19
 */
public class StartupManager extends Caster<Msg> {
    private static StartupManager instance = null;
    private Context context;

    private boolean started;
    private List<String> services = new ArrayList<>(Arrays.asList(
            "rest"         // 包含了接入功能如登录aps
//            "upgrade",      // 升级服务
//            "record"        // 会议记录
    ));

    private StartupManager(Context ctx) {
        context = ctx;
    }

    public synchronized static StartupManager getInstance(Application ctx) {
        if (instance == null) {
            instance = new StartupManager(ctx);
        }
        return instance;
    }

    @Override
    protected Map<Msg[], NtfProcessor<Msg>> subscribeNtfs() {
        return null;
    }

    /**
     * 启动，完成一些初始化的工作。
     * @param type 终端类型
     * @param resultListener 启动结果监听器
     * */
    public void start(TerminalType type, @NonNull IResultListener resultListener){
        if (started) {
            KLog.p(KLog.ERROR, "started yet!");
            return;
        }
        // 设置业务组件工作空间
//        File dir = new File(context.getFilesDir(), "cellar");
//        if (!dir.exists()){
//            if(!dir.mkdir()){
//                throw new RuntimeException("try to create dir "+dir.getAbsolutePath()+" failed");
//            }
//        }
//        req(Msg.SetMtWorkspace, null, dir.getAbsolutePath()); // TODO mtcapi-jni中没有SetSysWorkPathPrefix

        // 启动业务组件基础模块
        EmMtModel model = ToDoConverter.toTransferObj(type);
        req(Msg.StartMtBase, new SessionProcessor<Msg>() {
            // StartMtBase并不会给响应，我们必定是等待超时。
            // 我们利用超时机制做延时以保证此刻业务组件基础模块已经完全起来了，在此之前我们不能调用业务组件任何其他接口！
            @Override
            public boolean onTimeout(IResultListener resultListener, Msg req, Object[] reqParas) {

                // 启动业务组件sdk
                req(Msg.StartMtSdk, new SessionProcessor<Msg>() {
                    boolean hasServiceStartFailed = false;

                    @Override
                    public void onReqSent(IResultListener resultListener, Msg req, Object[] reqParas) {
                        // 设置业务组件sdk回调
                        set(Msg.SetMtSdkCallback, new IMtcCallback() {
                            @Override
                            public void Callback(String msg) {
                                try {
                                    JSONObject mtapi = new JSONObject(msg);
                                    String msgId = mtapi.getJSONObject("head").getString("eventname");
                                    String body = mtapi.getString("body");
                                    if (null == msgId || null == body) {
                                        KLog.p(KLog.ERROR, "invalid msg: msgId=%s, body=%s", msgId, body);
                                        return;
                                    }

                                    CrystalBall.instance().onAppear(msgId, body);

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }

                    @Override
                    public boolean onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas) {
                        boolean startSdkSuccess = ((TMTLoginMtResult) rspContent).bLogin;
                        if (startSdkSuccess){
                            // 启动业务组件其他模块
                            Stream.of(services).forEach(new Consumer<String>() {  // NOTE: 此处不要使用lambda，否则amulet绑定生命周期对象会有问题（待完善）
                                @Override
                                public void accept(String s) {
                                    req(Msg.StartMtService, new SessionProcessor<Msg>() {
                                        @Override
                                        public boolean onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas) {
                                            services.remove(s);
                                            TSrvStartResult result = (TSrvStartResult) rspContent;
                                            boolean success = result.MainParam.basetype && result.AssParam.achSysalias.equals(reqParas[0]);
                                            if (!success){
                                                KLog.p(KLog.ERROR, "service %s start failed!", s);
                                                hasServiceStartFailed = true;
                                            }
                                            if (services.isEmpty()){
                                                if (!hasServiceStartFailed) {
                                                    reportSuccess(null, resultListener);
                                                }else{
                                                    reportFailed(-1, resultListener);
                                                }
                                            }
                                            return true;
                                        }
                                    }, resultListener , s);
                                }
                            });

                            // 设置是否将业务组件日志写入日志文件
                            //        req(Msg.ToggleMtFileLog, null, true);
                            //        try {
//            req(Msg.SetNetWorkCfg, null,
//                    new TNetWorkInfo(convertTransType(NetworkHelper.getTransType()),
//                            NetAddrHelper.ipStr2Int(NetworkHelper.getAddr()),
//                            NetAddrHelper.ipStr2Int(NetworkHelper.getMask()),
//                            NetAddrHelper.ipStr2Int(NetworkHelper.getGateway()),
//                            NetAddrHelper.ipStr2Int(NetworkHelper.getDns()))
//            );
//        } catch (NetAddrHelper.InvalidIpv4Exception e) {
//            e.printStackTrace();
//            reportFailed(-1, resultListener);
//        }

                        }else{
                            reportFailed(-1, resultListener);
                        }
                        return true;
                    }
                },
                        resultListener,
                        false,
                        false,
                        new MtLoginMtParam(EmClientAppType.emClientAppSkyAndroid_Api, EmAuthType.emInnerPwdAuth_Api,
                        "admin", "2018_Inner_Pwd_|}><NewAccess#@k", "127.0.0.1", 60001)
                );

                return true;
            }

        }, resultListener, model, type.getVal(), "v0.1.0");

        started = true;
    }

    /**
     * 设置是否启用telnet调试
     * */
    public void setTelnetDebugEnable(boolean enable, IResultListener resultListener){
        BaseTypeBool baseTypeBool = new BaseTypeBool();
        baseTypeBool.basetype = enable;
        req(Msg.SetTelnetDebugEnable, new SessionProcessor<Msg>() {
            @Override
            public boolean onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas) {
                boolean enabled = ((BaseTypeBool) rspContent).basetype;
                if (enabled){
                    reportSuccess(null, resultListener);
                }else{
                    reportFailed(-1, resultListener);
                }
                return true;
            }
        }, resultListener, baseTypeBool);
    }


    /**
     * 登录Aps服务器
     * */
    public void loginAps(String ip, String account, String pwd, IResultListener resultListener){
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


    private EmNetTransportType convertTransType(int type){
        switch (type){
            case NetworkHelper.TRANS_ETHERNET:
                return EmNetTransportType.EthnetCard1;
            case NetworkHelper.TRANS_WIFI:
                return EmNetTransportType.Wifi;
            case NetworkHelper.TRANS_CELLULAR:
                return EmNetTransportType.MobileData;
            default:
                return EmNetTransportType.None;
        }
    }


}
