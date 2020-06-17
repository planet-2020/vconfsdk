package com.kedacom.vconf.sdk.alirtc.bean.transfer;

public class TMtRegistCsvInfo{
    public String achDevType; // 设备类型
    public String achCurSoftVer; // 设备目前软件版本
    public boolean bForceRegister; // 是否抢登

    public TMtRegistCsvInfo(String achDevType, String achCurSoftVer, boolean bForceRegister) {
        this.achDevType = achDevType;
        this.achCurSoftVer = achCurSoftVer;
        this.bForceRegister = bForceRegister;
    }
}