/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;

public class TDCSCreateConfResult{
    public String achConfE164;
    public String achConfName;
    public boolean bSuccess;
    public int dwErrorCode;
    public EmDcsConfMode emConfMode;
    public EmDcsConfType emConfType;
    public TDCSConfAddr tConfAddr;
    public boolean bCreator;            // 自己是否是这个数据协作的创建者

    public TDCSCreateConfResult(String achConfE164, String achConfName, boolean bSuccess, int dwErrorCode, EmDcsConfMode emConfMode, EmDcsConfType emConfType, boolean bCreator) {
        this.achConfE164 = achConfE164;
        this.achConfName = achConfName;
        this.bSuccess = bSuccess;
        this.dwErrorCode = dwErrorCode;
        this.emConfMode = emConfMode;
        this.emConfType = emConfType;
        this.bCreator = bCreator;
    }

    public TDCSCreateConfResult(boolean bSuccess, int dwErrorCode) {
        this.bSuccess = bSuccess;
        this.dwErrorCode = dwErrorCode;
    }
}
