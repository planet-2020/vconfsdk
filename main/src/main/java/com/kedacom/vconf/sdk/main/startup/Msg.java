package com.kedacom.vconf.sdk.main.startup;


import com.kedacom.vconf.sdk.annotation.Message;
import com.kedacom.vconf.sdk.annotation.Request;
import com.kedacom.vconf.sdk.annotation.Response;
import com.kedacom.vconf.sdk.main.startup.bean.transfer.MTCLoginResult;
import com.kedacom.vconf.sdk.main.startup.bean.transfer.TApsLoginResult;
import com.kedacom.vconf.sdk.main.startup.bean.transfer.TMTApsLoginParam;
import com.kedacom.vconf.sdk.main.startup.bean.transfer.TNetWorkInfo;
import com.kedacom.vconf.sdk.main.startup.bean.transfer.TSrvStartResult;


/**
 * Created by Sissi on 2019/7/19.
 * 启动模块消息定义。
 */

@Message(
        module = "SU"
)
enum Msg {

    /**设置终端组件工作空间
     * 其创建文件均在该路径下*/
    @Request(method = "SetSysWorkPathPrefix",
            owner = MethodOwner.KernalCtrl,
            paras = String.class  // native工作空间路径
            )
    SetMtWorkspace,

    /**启动终端组件基础功能*/
    @Request(method = "MtStart",
            owner = MethodOwner.KernalCtrl,
            paras = {int.class, // 终端型号
                                // （登录aps和升级检测也都传了“型号”参数，但那个型号仅用于登录和升级，
                                // 平台需要那样的参数（至于为什么没能做到各场景下统一用一个型号那是平台的问题），
                                // 我们平时所提及的“终端型号”指此处的）
                    String.class, // 终端型号名称（有型号为啥还要型号名称？）
                    String.class, // 终端软件版本号
                    String.class  // oem名称
            })
    StartMtBase,


    /**启动终端组件SDK*/
    @Request(method = "Start",
            owner = MethodOwner.MtcLib,
            paras = boolean.class, // 是否为mtc终端（业务组件将jni层分为两种使用模式：本地和远程，其中远程模式即对应mtc），移动软终端目前始终为非mtc终端。
            rspSeq = "StartMtSdkRsp")
    StartMtSdk,

    /**启动终端组件SDK响应*/
    @Response(id = "MTCLoginRsp",
            clz = MTCLoginResult.class)
    StartMtSdkRsp,

    /**启动终端组件服务
     * 对比“启动终端组件基础功能”，
     * 服务是一些可选的模块化的功能，如即时聊天、会议、数据协作等。
     * */
    @Request(method = "SYSStartService",
            owner = MethodOwner.MtServiceCfgCtrl,
            paras = StringBuffer.class, // 服务名称
            userParas = String.class,
            rspSeq = "StartMtServiceRsp"
            )
    StartMtService,

    /**启动终端组件服务响应*/
    @Response(id = "SrvStartResultNtf",
            clz = TSrvStartResult.class)
    StartMtServiceRsp,

    /**启用/停用终端组件日志文件功能*/
    @Request(method = "SetLogCfgCmd",
            owner = MethodOwner.ConfigCtrl,
            paras = boolean.class // true启用，false停用
    )
    ToggleMtFileLog,

    /**设置网络配置*/
    @Request(method = "SendUsedNetInfoNtf",
            owner = MethodOwner.CommonCtrl,
            paras = StringBuffer.class,
            userParas = TNetWorkInfo.class
    )
    SetNetWorkCfg,


    /**登录APS*/
    @Request(method = "LoginApsServerCmd",
            owner = MethodOwner.LoginCtrl,
            paras = StringBuffer.class,
            userParas = TMTApsLoginParam.class)
    LoginAps,

    /**登录APS响应*/
    @Response(id = "ApsLoginResultNtf",
            clz = TApsLoginResult.class)
    LoginApsRsp,

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
    }
}
