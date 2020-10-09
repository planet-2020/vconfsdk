package com.kedacom.vconf.sdk.base.startup;

import android.app.Application;
import android.os.Handler;

import androidx.annotation.NonNull;

import com.kedacom.kdv.mt.mtapi.IMtcCallback;
import com.kedacom.vconf.sdk.amulet.Caster;
import com.kedacom.vconf.sdk.amulet.CrystalBall;
import com.kedacom.vconf.sdk.amulet.IResultListener;
import com.kedacom.vconf.sdk.base.startup.bean.transfer.EmAuthType;
import com.kedacom.vconf.sdk.base.startup.bean.transfer.EmClientAppType;
import com.kedacom.vconf.sdk.base.startup.bean.transfer.MtLoginMtParam;
import com.kedacom.vconf.sdk.base.startup.bean.transfer.TMTLoginMtResult;
import com.kedacom.vconf.sdk.common.bean.TerminalType;
import com.kedacom.vconf.sdk.common.constant.EmMtModel;
import com.kedacom.vconf.sdk.utils.file.FileHelper;
import com.kedacom.vconf.sdk.utils.log.KLog;

import org.json.JSONException;
import org.json.JSONObject;


public class StartupManager extends Caster<Msg> {
    private static StartupManager instance = null;
    private Application context;

    private int state = Idle;
    private static final int Idle = 0;
    private static final int Starting = 1;
    private static final int StartSuccess = 2;
    private static final int StartFailed = 3;


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
     * @param resultListener onSuccess(null);
     *                       onFailed(
     *                       {@link SUResultCode#StartInProgress},null
     *                       / {@link SUResultCode#StartedAlready},null
     *                       / {@link SUResultCode#StartMtSdkFailed},null
     *                       );
     *                       onTimeout();
     * */
    public void start(TerminalType type, @NonNull IResultListener resultListener){
        if (state == Starting) {
            KLog.p(KLog.ERROR, "starting...");
            reportFailed(SUResultCode.StartInProgress, resultListener);
            return;
        }
        if (state == StartSuccess) {
            KLog.p(KLog.ERROR, "started already!");
            reportFailed(SUResultCode.StartedAlready, resultListener);
            return;
        }

        state = Starting;

        // 设置业务组件工作空间
        String ywzjWorkSpace = FileHelper.getPath(FileHelper.Location.EXTERNAL, FileHelper.Type.COMMON, "ywzj");
        req(Msg.SetMtWorkspace, null, null, ywzjWorkSpace);

        // 启动过程中禁止下发请求
        disableReqGlobally(true);

        // 启动业务组件基础模块
        EmMtModel model = ToDoConverter.toTransferObj(type);
        req(true, true, Msg.StartMtBase, new SessionProcessor<Msg>() {
            @Override
            public void onReqSent(IResultListener resultListener, Msg req, Object[] reqParas, Object output) {
               // 启用业务组件保存日志到文件的功能
                req(true, true, Msg.MtLogToFile, null, null, true);

                new Handler().postDelayed(() -> {
                // 启动业务组件sdk
                req(true, true, Msg.StartMtSdk, new SessionProcessor<Msg>() {

                        @Override
                        public void onReqSent(IResultListener resultListener1, Msg req1, Object[] reqParas1, Object output) {
                            // 设置业务组件sdk回调
                            req(true, true, Msg.SetMtSdkCallback, null, null, (IMtcCallback) msg -> {
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
                        public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener1, boolean isFinal, Msg req1, Object[] reqParas1, boolean[] isConsumed) {
                            // 取消禁令
                            disableReqGlobally(false);

                            boolean startSdkSuccess = ((TMTLoginMtResult) rspContent).bLogin;
                            if (startSdkSuccess){
                                state = StartSuccess;
                                reportSuccess(null, resultListener1);
                            }else{
                                state = StartFailed;
                                reportFailed(SUResultCode.StartMtSdkFailed, resultListener);
                            }
                        }

                        @Override
                        public void onTimeout(IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                            // 取消禁令
                            disableReqGlobally(false);

                            state = StartFailed;
                        }
                    },

                    resultListener,
                    false,
                    false,
                    new MtLoginMtParam(EmClientAppType.emClientAppSkyAndroid_Api, EmAuthType.emInnerPwdAuth_Api,
                            "admin", "2018_Inner_Pwd_|}><NewAccess#@k", "127.0.0.1", 60001)
                );

                },
                        500 // 业务组件未提供方法通知启动已完成，故此处延时以保证业务组件启动完成（延时时长是跟业务组件协商的结果）
                );
            }

        }, resultListener, model, type.getVal(), "v0.1.0");

    }

}
