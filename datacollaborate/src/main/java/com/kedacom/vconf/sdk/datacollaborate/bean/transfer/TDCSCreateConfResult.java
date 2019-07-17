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

    public TDCSCreateConfResult() {
        this.achConfE164 = "confE164";
        this.achConfName = "confName";
        this.bSuccess = true;
        this.dwErrorCode = 0;
        this.emConfMode = EmDcsConfMode.emConfModeAuto;
        this.emConfType = EmDcsConfType.emConfTypeMCC;
        this.bCreator = true;
    }
}
