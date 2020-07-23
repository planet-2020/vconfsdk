package com.kedacom.vconf.sdk.base.startup;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;

import com.kedacom.kdv.mt.mtapi.IMtcCallback;
import com.kedacom.vconf.sdk.amulet.Caster;
import com.kedacom.vconf.sdk.amulet.CrystalBall;
import com.kedacom.vconf.sdk.amulet.IResultListener;
import com.kedacom.vconf.sdk.base.startup.bean.transfer.EmAuthType;
import com.kedacom.vconf.sdk.base.startup.bean.transfer.EmClientAppType;
import com.kedacom.vconf.sdk.base.startup.bean.transfer.EmNetTransportType;
import com.kedacom.vconf.sdk.base.startup.bean.transfer.MtLoginMtParam;
import com.kedacom.vconf.sdk.base.startup.bean.transfer.TMTLoginMtResult;
import com.kedacom.vconf.sdk.common.bean.TerminalType;
import com.kedacom.vconf.sdk.common.constant.EmMtModel;
import com.kedacom.vconf.sdk.utils.file.FileHelper;
import com.kedacom.vconf.sdk.utils.log.KLog;
import com.kedacom.vconf.sdk.utils.net.NetworkHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Objects;


public class StartupManager extends Caster<Msg> {
    private static StartupManager instance = null;
    private Application context;

    private boolean started;

    private StartupManager(Application ctx) {
        context = ctx;
    }

    public synchronized static StartupManager getInstance(Application ctx) {
        if (instance == null) {
            instance = new StartupManager(ctx);
        }
        return instance;
    }


    /**
     * 启动，完成一些初始化的工作。
     * @param type 终端类型
     * @param resultListener onSuccess null
     *                       onFailed
     * */
    public void start(TerminalType type, @NonNull IResultListener resultListener){
        if (started) {
            KLog.p(KLog.WARN, "started yet!");
            reportSuccess(null, resultListener);
            return;
        }

        // 启动业务组件基础模块
        EmMtModel model = ToDoConverter.toTransferObj(type);
        req(Msg.StartMtBase, new SessionProcessor<Msg>() {
            @Override
            public void onReqSent(IResultListener resultListener, Msg req, Object[] reqParas) {
                // 设置业务组件工作空间
                String ywzjWorkSpace = FileHelper.getPath(FileHelper.Location.EXTERNAL, FileHelper.Type.COMMON, "ywzj");
                File dir = FileHelper.createDir(Objects.requireNonNull(ywzjWorkSpace));
                req(Msg.SetMtWorkspace, null, null, Objects.requireNonNull(dir).getAbsolutePath());
                // 启用业务组件保存日志到文件的功能
                req(Msg.MtLogToFile, null, null, true);
            }

            // StartMtBase并不会给响应，我们必定是等待超时。
            // 我们利用超时机制做延时以保证此刻业务组件基础模块已经完全起来了，在此之前我们不能调用业务组件任何其他接口！
            @Override
            public void onTimeout(IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {

                isConsumed[0] = true;

                // 启动业务组件sdk
                req(Msg.StartMtSdk, new SessionProcessor<Msg>() {

                    @Override
                    public void onReqSent(IResultListener resultListener, Msg req, Object[] reqParas) {
                        // 设置业务组件sdk回调
                        set(Msg.SetMtSdkCallback, (IMtcCallback) msg -> {
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
                        });
                    }

                    @Override
                    public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                        boolean startSdkSuccess = ((TMTLoginMtResult) rspContent).bLogin;
                        if (startSdkSuccess){
                            reportSuccess(null, resultListener);

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
                    }
                },
                        resultListener,
                        false,
                        false,
                        new MtLoginMtParam(EmClientAppType.emClientAppSkyAndroid_Api, EmAuthType.emInnerPwdAuth_Api,
                        "admin", "2018_Inner_Pwd_|}><NewAccess#@k", "127.0.0.1", 60001)
                );

            }

        }, resultListener, model, type.getVal(), "v0.1.0");

        started = true;
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
