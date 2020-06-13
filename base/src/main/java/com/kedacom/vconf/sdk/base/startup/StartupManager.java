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
import com.kedacom.vconf.sdk.common.constant.EmMtModel;
import com.kedacom.vconf.sdk.common.type.BaseTypeBool;
import com.kedacom.vconf.sdk.utils.log.KLog;
import com.kedacom.vconf.sdk.utils.net.NetAddrHelper;
import com.kedacom.vconf.sdk.utils.net.NetworkHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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
    private boolean hasServiceStartFailed = false;

//    static {
//        System.loadLibrary("mtcapidll-jni");  // 业务组件会在MtcLib中加载
//    }


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
    protected Set<Msg> subscribeNtfs() {
        return null;
    }

    /**
     * 启动，完成一些初始化的工作。
     * (启动过程中一般展示欢迎界面)
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
//        req(Msg.SetMtWorkspace, null, dir.getAbsolutePath()); // FIXME mtcapi-jni中没有SetSysWorkPathPrefix

        // 启动业务组件基础模块
        EmMtModel model = ToDoConverter.toTransferObj(type);
        req(Msg.StartMtBase, new IResultListener() {
            @Override
            public void onArrive(boolean bSuccess) {
                // StartMtBase并不会给响应，此处我们是等待超时了。
                // 我们利用超时机制做延时以保证此刻业务组件基础模块已经完全起来了，在此之前我们不能调用业务组件任何其他接口！

                // 启动业务组件sdk
                req(Msg.StartMtSdk, new IResultListener() {
                            @Override
                            public void onSuccess(Object result) {

                                // 启动业务组件其他模块
                                Stream.of(services).forEach(new Consumer<String>() {  // NOTE: 此处不要使用lambda，否则amulet绑定生命周期对象会有问题（待完善）
                                    @Override
                                    public void accept(String s) {
                                        req(Msg.StartMtService, new IResultListener() {
                                            @Override
                                            public void onArrive(boolean bSuccess) {
                                                services.remove(s);
                                                if (!bSuccess){
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
                                            }
                                        }, s);
                                    }
                                });

                            }

                            @Override
                            public void onFailed(int errorCode) {
                                reportFailed(errorCode, resultListener);
                            }

                            @Override
                            public void onTimeout() {
                                reportFailed(-1, resultListener);
                            }
                        },
                        false, false, new MtLoginMtParam(
                                EmClientAppType.emClientAppSkyAndroid_Api, EmAuthType.emInnerPwdAuth_Api,
                                "admin", "2018_Inner_Pwd_|}><NewAccess#@k", "127.0.0.1", 60001
                        )
                );

                // 设置业务组件回调
                req(Msg.SetCallback, null, new IMtcCallback() {
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

        }, model, type.getVal(), "v0.1.0");



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

        started = true;

    }

    /**
     * 设置是否启用telnet调试
     * */
    public void setTelnetDebugEnable(boolean enable, IResultListener listener){
        BaseTypeBool baseTypeBool = new BaseTypeBool();
        baseTypeBool.basetype = enable;
        req(Msg.SetTelnetDebugEnable, listener, baseTypeBool);
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

        req(Msg.SetApsServerCfg, new IResultListener() {
            @Override
            public void onArrive(boolean bSuccess) {
                req(Msg.LoginAps, resultListener, new TMTApsLoginParam(account, pwd, "", "Skywalker_Ali", ""));
            }
        }, new MtXAPSvrListCfg(0, Collections.singletonList(mtXAPSvrCfg)));

    }


    /**
     * 登出Aps服务器
     * */
    public void logoutAps(){

    }


    @Override
    protected boolean onRsp(Msg rspId, Object rspContent, IResultListener listener, Msg reqId, Object[] reqParas){
        switch (rspId){
            case StartMtSdkRsp:
                TMTLoginMtResult startSdkResult = (TMTLoginMtResult) rspContent;
                if (startSdkResult.bLogin){
                    reportSuccess(null, listener);
                }else{
                    reportFailed(-1, listener);
                }
                break;

            case StartMtServiceRsp:
                TSrvStartResult result = (TSrvStartResult) rspContent;
                if (result.MainParam.basetype && result.AssParam.achSysalias.equals(reqParas[0])){
                    reportSuccess(null, listener);
                }else{
                    reportFailed(-1, listener);
                }
                break;

            case SetTelnetDebugEnableRsp:
                BaseTypeBool enabled = (BaseTypeBool) rspContent;
                if (enabled.basetype){
                    reportSuccess(null, listener);
                }else{
                    reportFailed(-1, listener);
                }
                break;

            case SetApsServerCfgRsp:
                reportSuccess(null, listener);
                break;

            case LoginApsRsp:
                TApsLoginResult apsLoginResult = (TApsLoginResult) rspContent;
                if (apsLoginResult.bSucess){
                    reportSuccess(null, listener);
                }else{
                    reportFailed(-1, listener);
                }
                break;
        }
        return true;
    }


    @Override
    protected void onNtf(Msg ntf, Object ntfContent) {

    }

    @Override
    protected boolean onTimeout(Msg req, IResultListener listener, Object[] reqParas) {
        return super.onTimeout(req, listener, reqParas);
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
