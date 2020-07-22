/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;

public class TDCSConfInfo {

    public String achConfE164;
    public String achConfName;
    public EmDcsConfMode emConfMode;
    public EmDcsConfType emConfType;

    public TDCSConfInfo(String achConfE164, String achConfName, EmDcsConfMode emConfMode, EmDcsConfType emConfType) {
        this.achConfE164 = achConfE164;
        this.achConfName = achConfName;
        this.emConfMode = emConfMode;
        this.emConfType = emConfType;
    }
}
