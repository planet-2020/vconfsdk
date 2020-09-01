package com.kedacom.vconf.sdk.base.upgrade;


import com.kedacom.vconf.sdk.annotation.Module;
import com.kedacom.vconf.sdk.annotation.Notification;
import com.kedacom.vconf.sdk.annotation.Request;
import com.kedacom.vconf.sdk.annotation.Response;
import com.kedacom.vconf.sdk.base.login.bean.transfer.TMtSvrStateList;
import com.kedacom.vconf.sdk.base.upgrade.bean.transfer.TCheckUpgradeRsp;
import com.kedacom.vconf.sdk.base.upgrade.bean.transfer.TMTSUSAddr;
import com.kedacom.vconf.sdk.base.upgrade.bean.transfer.TMTUpgradeClientInfo;
import com.kedacom.vconf.sdk.base.upgrade.bean.transfer.TMTUpgradeDownloadInfo;
import com.kedacom.vconf.sdk.common.bean.transfer.TSrvStartResult;

import static com.kedacom.vconf.sdk.annotation.Request.*;

@Module(name = "UG")
enum Msg {
    /**启动（升级）服务
     * */
    @Request(name = "SYSStartService",
            owner = MtServiceCfgCtrl,
            paras = StringBuffer.class,
            userParas = String.class, // 服务名称，如"rest"接入、"upgrade"升级
            rspSeq = "StartMtServiceRsp"
    )
    StartMtService,

    @Response(name = "SrvStartResultNtf",
            clz = TSrvStartResult.class)
    StartMtServiceRsp,

    /**获取升级服务器地址
     * */
    @Request(name = "GetSUSCfg",
            owner = ConfigCtrl,
            paras = StringBuffer.class,
            userParas = TMTSUSAddr.class,
            outputParaIndex = LastIndex
    )
    GetServerAddr,

    /**检查更新
     * */
    @Request(name = "MTCheckUpgradeCmd",
            owner = MtEntityCtrl,
            paras = StringBuffer.class,
            userParas = TMTUpgradeClientInfo.class,
            rspSeq = "CheckUpgradeRsp"
            )
    CheckUpgrade,

    @Response(name = "UpgradeVersionInfoNtf",
            clz = TCheckUpgradeRsp.class)
    CheckUpgradeRsp,


    /**下载升级包
     * */
    @Request(name = "MTStartDownloadUpgradeFileCmd",
            owner = MtEntityCtrl,
            paras = {StringBuffer.class,
                    int.class
            },
            userParas = {String.class, // 升级包本地存放路径
                    int.class // CheckUpgradeRsp消息体中的TMTUpgradeVersionInfo#dwVer_id
            },
            rspSeq = {"DownloadUpgradeRsp", GREEDY},
            rspSeq2 = {"DownloadUpgradeRsp", GREEDY, "ServerDisconnected"}, // 失败的情形
            rspSeq3 = {"ServerDisconnected"}, // 失败的情形
            timeout = 300
    )
    DownloadUpgrade,

    @Response(name = "UpgradeFileDownloadInfoNtf",
            clz = TMTUpgradeDownloadInfo.class)
    DownloadUpgradeRsp,

    @Response(name = "UpgradeFileDownloadOkNtf",
            clz = Void.class)
    DownloadUpgradeFin,


    /**取消升级
     * */
    @Request(name = "MTCancelUpgradeCmd",
            owner = MtEntityCtrl,
            rspSeq = "CancelUpgradeRsp"
    )
    CancelUpgrade,

    @Response(name = "SetSvrLoginStatusRtNtf",
            clz = TMtSvrStateList.class)
    CancelUpgradeRsp,

    /**
     * 升级服务器已断开
     * 例如升级过程中CancelUpgrade会收到该消息（非升级状态下CancelUpgrade则不会收到该消息）
     * */
    @Notification
    @Response(name = "UpgradeDisconnectServerNtf", clz = Void.class)
    ServerDisconnected,

}
