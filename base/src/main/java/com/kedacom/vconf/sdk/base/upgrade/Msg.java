package com.kedacom.vconf.sdk.base.upgrade;


import com.kedacom.vconf.sdk.annotation.Message;
import com.kedacom.vconf.sdk.annotation.Request;
import com.kedacom.vconf.sdk.annotation.Response;
import com.kedacom.vconf.sdk.base.upgrade.bean.transfer.TMTSUSAddr;
import com.kedacom.vconf.sdk.base.upgrade.bean.transfer.TMTUpgradeClientInfo;
import com.kedacom.vconf.sdk.base.upgrade.bean.transfer.TMTUpgradeVersionInfoList;
import com.kedacom.vconf.sdk.common.bean.transfer.TSrvStartResult;

import static com.kedacom.vconf.sdk.annotation.Request.GET;


/**
 * Created by Sissi on 2019/7/19.
 * 启动模块消息定义。
 */

@Message(
        module = "UG"
)
enum Msg {
    /**启动业务组件功能模块
     * */
    @Request(method = "SYSStartService",
            owner = MethodOwner.MtServiceCfgCtrl,
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
            owner = MethodOwner.ConfigCtrl,
            paras = StringBuffer.class,
            userParas = TMTSUSAddr.class,
            type = GET
    )
    GetServerAddr,

    /**检查更新
     * */
    @Request(method = "MTCheckUpgradeCmd",
            owner = MethodOwner.MtEntityCtrl,
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
            owner = MethodOwner.MtEntityCtrl,
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
            owner = MethodOwner.MtEntityCtrl)
    CancelUpgrade,


    END;

    private static class MethodOwner {
        private static final String PKG = "com.kedacom.kdv.mt.mtapi.";

        private static final String KernalCtrl = PKG+"KernalCtrl";
        private static final String MtcLib = PKG+"MtcLib";
        private static final String LoginCtrl = PKG+"LoginCtrl";
        private static final String MonitorCtrl = PKG+"MonitorCtrl";
        private static final String CommonCtrl = PKG+"CommonCtrl";
        private static final String ConfigCtrl = PKG+"ConfigCtrl";
        private static final String MtServiceCfgCtrl = PKG+"MtServiceCfgCtrl";
        private static final String MeetingCtrl = PKG+"MeetingCtrl";
        private static final String MtEntityCtrl = PKG+"MtEntityCtrl";
    }
}
