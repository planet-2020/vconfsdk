package com.kedacom.vconf.sdk.common.type.vconf;

/**
 * Created by Sissi on 2019/10/24
 */
public final class TMtId {
    public int         dwMcuId;                ///< mcu 号码
    public int         dwTerId;                ///< 终端 号码

    public boolean isValid(){
        return dwMcuId != 0 || dwTerId != 0;
    }

    public TMtId(int dwMcuId, int dwTerId) {
        this.dwMcuId = dwMcuId;
        this.dwTerId = dwTerId;
    }
}
