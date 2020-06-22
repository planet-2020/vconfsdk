package com.kedacom.vconf.sdk.base.upgrade;


import com.kedacom.vconf.sdk.amulet.Atlas;
import com.kedacom.vconf.sdk.annotation.Message;
import com.kedacom.vconf.sdk.annotation.Request;
import com.kedacom.vconf.sdk.annotation.Response;
import com.kedacom.vconf.sdk.base.upgrade.bean.transfer.TMTSUSAddr;
import com.kedacom.vconf.sdk.base.upgrade.bean.transfer.TMTUpgradeClientInfo;
import com.kedacom.vconf.sdk.base.upgrade.bean.transfer.TMTUpgradeVersionInfoList;
import com.kedacom.vconf.sdk.common.bean.transfer.TSrvStartResult;


@Message(
        module = "UG"
)
enum Msg {
    /**启动（升级）服务
     * */
    @Request(method = "SYSStartService",
            owner = Atlas.MtServiceCfgCtrl,
            paras = StringBuffer.class,
            userParas = String.class, // 服务名称，如"rest"接入、"upgrade"升级
            rspSeq = "StartMtServiceRsp"
    )
    StartMtService,

    @Response(id = "SrvStartResultNtf",
            clz = TSrvStartResult.class)
    StartMtServiceRsp,

    /**获取升级服务器地址
     * */
    @Request(method = "GetSUSCfg",
            owner = Atlas.ConfigCtrl,
            paras = StringBuffer.class,
            userParas = TMTSUSAddr.class,
            isGet = true
    )
    GetServerAddr,

    /**检查更新
     * */
    @Request(method = "MTCheckUpgradeCmd",
            owner = Atlas.MtEntityCtrl,
            paras = StringBuffer.class,
            userParas = TMTUpgradeClientInfo.class,
            rspSeq = "CheckUpgradeRsp"
            )
    CheckUpgrade,

    @Response(id = "UpgradeVersionInfoNtf",
            clz = TMTUpgradeVersionInfoList.class)
    CheckUpgradeRsp,


    /**下载升级包
     * */
    @Request(method = "MTStartDownloadUpgradeFileCmd",
            owner = Atlas.MtEntityCtrl,
            paras = {StringBuffer.class,
                    int.class
            },
            userParas = {String.class, // 升级包本地存放路径
                    int.class // CheckUpgradeRsp消息体中的TMTUpgradeVersionInfo#dwVer_id
            }
    )
    DownloadUpgrade,


    /**取消升级
     * */
    @Request(method = "MTCancelUpgradeCmd",
            owner = Atlas.MtEntityCtrl)
    CancelUpgrade,


    END;

}
