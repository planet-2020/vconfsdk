package com.kedacom.vconf.sdk.base.startup.bean.transfer;

import java.util.List;

public class MtXAPSvrListCfg {
    public int                 byCurIndex;      ///< 当前生效的地址索引
    public List<MtXAPSvrCfg> arrMtXAPSvr;     ///< XAP登录列表
    public int                 byCnt;           ///< 实际个数

    public MtXAPSvrListCfg(int byCurIndex, List<MtXAPSvrCfg> arrMtXAPSvr) {
        this.byCurIndex = byCurIndex;
        this.arrMtXAPSvr = arrMtXAPSvr;
        byCnt = arrMtXAPSvr.size();
    }
}
