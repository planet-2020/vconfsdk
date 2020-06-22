package com.kedacom.vconf.sdk.base.upgrade;

import android.app.Application;
import android.content.Context;

import com.kedacom.vconf.sdk.amulet.Caster;
import com.kedacom.vconf.sdk.amulet.IResultListener;
import com.kedacom.vconf.sdk.base.upgrade.bean.transfer.*;
import com.kedacom.vconf.sdk.common.bean.TerminalType;
import com.kedacom.vconf.sdk.common.bean.transfer.TSrvStartResult;
import com.kedacom.vconf.sdk.utils.log.KLog;

import java.util.Map;


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

    @Override
    protected Map<Msg[], NtfProcessor<Msg>> subscribeNtfs() {
        return null;
    }


    /**
     * 检查更新
     * @param terminalType 终端类型
     * @param version 软件版本
     * @param e164
     * */
    public void checkUpgrade(TerminalType terminalType, String version, String e164, IResultListener resultListener){
        TMTSUSAddr addr = (TMTSUSAddr) get(Msg.GetServerAddr);
        if (null == addr){
            reportFailed(-1, resultListener);
            return;
        }
        TMTUpgradeClientInfo checkUpgradePara = new TMTUpgradeClientInfo(
                new TMTUpgradeNetParam(addr.dwIP),
                new TMTUpgradeDeviceInfo(terminalType.getVal(), e164, version, addr.dwIP)
        );
        req(Msg.CheckUpgrade, new SessionProcessor<Msg>() {
            @Override
            public void onRsp(Msg rsp, Object rspContent, IResultListener resultListener, Msg req, Object[] reqParas, boolean[] isConsumed) {
                TMTUpgradeVersionInfo[] remoteVersionList = ((TMTUpgradeVersionInfoList)rspContent).tVerList;
                if (null != remoteVersionList && remoteVersionList.length>0){
                    reportSuccess(null, resultListener);
                }else{
                    reportFailed(-1, resultListener);
                }
            }
        }, resultListener, checkUpgradePara);
    }

}
