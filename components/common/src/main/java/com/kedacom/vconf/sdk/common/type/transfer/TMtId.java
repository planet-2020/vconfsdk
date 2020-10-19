package com.kedacom.vconf.sdk.common.type.transfer;

/**
 * Created by Sissi on 2019/10/24
 */
public final class TMtId {
    public int         dwMcuId;                ///< mcu 号码
    public int         dwTerId;                ///< 终端 号码

    public boolean isValid(){
        // NOTE: 此为目前经平台确认的有效范围，将来很可能变更！！！到时候请记起此坑！
        return 0< dwTerId && dwTerId<=192;
    }

    public TMtId(int dwMcuId, int dwTerId) {
        this.dwMcuId = dwMcuId;
        this.dwTerId = dwTerId;
    }
}
