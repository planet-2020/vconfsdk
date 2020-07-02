package com.kedacom.vconf.sdk.base.upgrade;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;

import com.kedacom.vconf.sdk.amulet.Caster;
import com.kedacom.vconf.sdk.amulet.IResultListener;
import com.kedacom.vconf.sdk.base.login.bean.transfer.EmServerState;
import com.kedacom.vconf.sdk.base.login.bean.transfer.EmServerType;
import com.kedacom.vconf.sdk.base.login.bean.transfer.TMtSvrState;
import com.kedacom.vconf.sdk.base.login.bean.transfer.TMtSvrStateList;
import com.kedacom.vconf.sdk.base.upgrade.bean.DownloadProgressInfo;
import com.kedacom.vconf.sdk.base.upgrade.bean.UpgradePkgInfo;
import com.kedacom.vconf.sdk.base.upgrade.bean.transfer.TCheckUpgradeRsp;
import com.kedacom.vconf.sdk.base.upgrade.bean.transfer.TMTSUSAddr;
import com.kedacom.vconf.sdk.base.upgrade.bean.transfer.TMTUpgradeClientInfo;
import com.kedacom.vconf.sdk.base.upgrade.bean.transfer.TMTUpgradeDeviceInfo;
import com.kedacom.vconf.sdk.base.upgrade.bean.transfer.TMTUpgradeDownloadInfo;
import com.kedacom.vconf.sdk.base.upgrade.bean.transfer.TMTUpgradeNetParam;
import com.kedacom.vconf.sdk.base.upgrade.bean.transfer.TMTUpgradeVersionInfo;
import com.kedacom.vconf.sdk.common.bean.TerminalType;
import com.kedacom.vconf.sdk.common.bean.transfer.TSrvStartResult;
import com.kedacom.vconf.sdk.utils.file.FileHelper;
import com.kedacom.vconf.sdk.utils.log.KLog;

import static com.kedacom.vconf.sdk.base.upgrade.UpgradeResultCode.*;


public class UpgradeManager extends Caster<Msg> {
    private static UpgradeManager instance = null;
    private Context context;


    private UpgradeManager(Context ctx) {
        context = ctx;
    }

    public synchronized static UpgradeManager getInstance(Application ctx) {
        if (instance == null) {
            instance = new UpgradeManager(ctx);
            instance.startService();
        }
        return instance;
    }

    // 启动业务组件升级服务
    private void startService(){
        String serviceName = "upgrade";
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
     * 检查更新
     * @param terminalType 终端类型
     * @param version 当前软件版本
     * @param e164 用户e164
     * @param resultListener onSuccess {@link UpgradePkgInfo}；
     *                       onFailed  {@link UpgradeResultCode#NO_UPGRADE_PACKAGE}
     *                                 {@link UpgradeResultCode#ALREADY_NEWEST}
     * */
    public void checkUpgrade(@NonNull TerminalType terminalType, @NonNull String version, @NonNull String e164, @NonNull IResultListener resultListener){
        TMTSUSAddr addr = (TMTSUSAddr) get(Msg.GetServerAddr);
        if (null == addr){
            reportFailed(-1, resultListener);
            return;
        }
        TMTUpgradeClientInfo checkUpgradePara = new TMTUpgradeClientInfo(
                new TMTUpgradeNetParam(addr.dwIP),
                new TMTUpgradeDeviceInfo(terminalType.getVal(), e164, version, addr.dwIP, "kedacom")
        );
        req(Msg.CheckUpgrade, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                TMTUpgradeVersionInfo[] remoteVersionList = ((TCheckUpgradeRsp)rspContent).AssParam.tVerList;
                if (null != remoteVersionList && remoteVersionList.length>0){
                    UpgradePkgInfo upgradePkgInfo = ToDoConverter.TMTUpgradeVersionInfo2UpgradePkgInfo(remoteVersionList[0]);
                    if (version.compareToIgnoreCase(upgradePkgInfo.versionNum) < 0) {
                        reportSuccess(upgradePkgInfo, resultListener);
                    }else{
                        reportFailed(ALREADY_NEWEST, resultListener);
                    }
                }else{
                    reportFailed(NO_UPGRADE_PACKAGE, resultListener);
                }
            }
        }, resultListener, checkUpgradePara);
    }


    /**
     * 下载升级包
     * @param versionId 目标版本id（由checkUpgrade的返回结果中获取）。
     * @param saveDir 升级包存放目录
     * @param resultListener onProgress {@link DownloadProgressInfo}
     *                       onSuccess  null
     *                       onFailed   {@link UpgradeResultCode#SERVER_DISCONNECTED}
     * */
    public void downloadUpgrade(int versionId, String saveDir, IResultListener resultListener){
        if (FileHelper.createDir(saveDir) == null){
            KLog.p(KLog.ERROR, "create save dir %s failed!", saveDir);
            reportFailed(-1, resultListener);
            return;
        }
        req(Msg.DownloadUpgrade, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                if (Msg.DownloadUpgradeRsp == rsp){
                    TMTUpgradeDownloadInfo downloadInfo = (TMTUpgradeDownloadInfo) rspContent;
                    if (downloadInfo.dwErrcode == 0) {
                        reportProgress(new DownloadProgressInfo(downloadInfo.dwCurPercent), resultListener);
                        if (downloadInfo.dwCurPercent == 100) {
                            cancelReq(Msg.DownloadUpgrade, resultListener); // 已下载完毕，取消会话，否则会话会等待超时。
                            reportSuccess(null, resultListener);
                        }
                    } else {
                        reportFailed(-1, resultListener);
                    }
                }else if (Msg.ServerDisconnected == rsp){
                    reportFailed(SERVER_DISCONNECTED, resultListener);
                }
            }

            @Override
            public void onTimeout(IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                req(Msg.CancelUpgrade, null, null);
                isConsumed[0] = true;
                reportTimeout(resultListener);
            }
        }, resultListener, saveDir, versionId);
    }


    /**
     * 取消升级
     * @param resultListener onSuccess null
     * */
    public void cancelUpgrade(IResultListener resultListener){
        req(Msg.CancelUpgrade, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                TMtSvrState[] states = ((TMtSvrStateList) rspContent).arrSvrState;
                boolean got = false;
                for (TMtSvrState state : states){
                    if (EmServerType.emSUS == state.emSvrType
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

}
