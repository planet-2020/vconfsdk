package com.kedacom.vconf.sdk.main.startup;

import android.app.Application;
import android.content.Context;

import com.kedacom.vconf.sdk.amulet.Caster;
import com.kedacom.vconf.sdk.amulet.IResultListener;
import com.kedacom.vconf.sdk.main.startup.bean.*;
import com.kedacom.vconf.sdk.main.startup.bean.transfer.*;
import com.kedacom.vconf.sdk.utils.log.KLog;
import com.kedacom.vconf.sdk.utils.net.NetAddrHelper;
import com.kedacom.vconf.sdk.utils.net.NetworkHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Sissi on 2019/7/19
 */
public class StartupManager extends Caster<Msg> {
    private static StartupManager instance = null;

    private Context context;

    private static Map<EmMtModel, String> modelMap = new HashMap<>();
    static {
        modelMap.put(EmMtModel.emSkyAndroidPhone, "SKY for Android Phone");
        modelMap.put(EmMtModel.emSkyAndroidPad, "SKY for Android Pad");
        modelMap.put(EmMtModel.emTrueTouchAndroidPhone, "TrueTouch for android");
        modelMap.put(EmMtModel.emTrueTouchAndroidPad, "TrueTouch for android");
    }

    private StartupManager(Context ctx) {
        context = ctx;
    }

    public static StartupManager getInstance(Application ctx) {
        if (instance == null) {
            synchronized (StartupManager.class) {
                if (instance == null) {
                    instance = new StartupManager(ctx);
                }
            }
        }
        return instance;
    }


    @Override
    protected Map<Msg[], RspProcessor<Msg>> rspsProcessors() {
        Map<Msg[], RspProcessor<Msg>> processorMap = new HashMap<>();

        processorMap.put(new Msg[]{
                Msg.StartMtService,
        }, this::onRsps);

        return processorMap;
    }

    @Override
    protected Map<Msg[], NtfProcessor<Msg>> ntfsProcessors() {
        Map<Msg[], NtfProcessor<Msg>> processorMap = new HashMap<>();
        return processorMap;
    }

    /**
     * 启动，完成一些初始化的工作。
     * 启动的过程可展示欢迎界面。
     * */
    public void start(IResultListener resultListener){
        start(ETerminalType.SkyAndroidPhone, resultListener);
    }

    /**
     * 启动，完成一些初始化的工作。
     * 启动的过程可展示欢迎界面。
     * */
    public void start(ETerminalType type, IResultListener resultListener){
        File dir = new File(context.getFilesDir(), "native");
        if (!dir.exists()){
            if(!dir.mkdir()){
                throw new RuntimeException("try to create dir "+dir.getAbsolutePath()+" failed");
            }
        }
        req(Msg.SetMtWorkspace, null, dir.getAbsolutePath());
        EmMtModel model = ToDoConverter.toTransferObj(type);
        req(Msg.StartMtBase, null, model.getValue(), modelMap.get(model), "", "");
        bMtSdkStarted = false;
        req(Msg.StartMtSdk, new IResultListener() {
            @Override
            public void onSuccess(Object result) {
                bMtSdkStarted = true;
                if (startingSrvList.isEmpty()){
                    // SDK和服务都已启动完毕（此时MtBase也应该已经启动完毕（@业务组件）），我们通知用户启动成功。
                    reportSuccess(null, resultListener);
                }
            }
        }, false);

        startingSrvList.clear();
        startingSrvList.add("im");
        startingSrvList.add("conf");
        startingSrvList.add("upgrade");
        startServices(resultListener);

        req(Msg.ToggleMtFileLog, null, true);

        try {
            req(Msg.SetNetWorkCfg, null,
                    new TNetWorkInfo(convertTransType(NetworkHelper.getTransType()),
                            NetAddrHelper.ipStr2Int(NetworkHelper.getAddr()),
                            NetAddrHelper.ipStr2Int(NetworkHelper.getMask()),
                            NetAddrHelper.ipStr2Int(NetworkHelper.getGateway()),
                            NetAddrHelper.ipStr2Int(NetworkHelper.getDns()))
            );
        } catch (NetAddrHelper.InvalidIpv4Exception e) {
            e.printStackTrace();
            reportFailed(-1, resultListener);
        }

    }

    private boolean bMtSdkStarted;
    private List<String> startingSrvList = new ArrayList<>();
    private void startServices(IResultListener resultListener){
        for (String srv : startingSrvList) {
            req(Msg.StartMtService, new IResultListener() {
                @Override
                public void onArrive(boolean bSuccess) {
                    if (!bSuccess) {
                        KLog.p(KLog.ERROR, "StartMtService %s failed!", srv);
                    }
                    startingSrvList.remove(srv);
                    if (bMtSdkStarted && startingSrvList.isEmpty()){
                        // SDK和服务都已启动完毕（此时MtBase也应该已经启动完毕（@业务组件）），我们通知用户启动成功。
                        reportSuccess(null, resultListener);
                    }
                }
            }, srv); // TODO，筛选出核心服务，其他服务在子模块加载
        }

    }


    private boolean onRsps(Msg rspId, Object rspContent, IResultListener listener, Msg reqId, Object[] reqParas){
        switch (rspId){
            case StartMtSdkRsp:
                break;
            case StartMtServiceRsp:
                break;
        }
        return true;
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
