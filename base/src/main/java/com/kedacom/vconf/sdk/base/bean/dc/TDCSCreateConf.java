/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.base.bean.dc;

import java.util.List;

public class TDCSCreateConf {

    public EmDcsConfType emConfType;
    public String achConfE164;
    public String achConfName;
    public EmDcsConfMode emConfMode;

    public int dwListNum;
    public List<TDCSConfUserInfo> atUserList;

    public String achConfAdminE164;
    public EmDcsType     emAdminMtType;

    public TDCSCreateConf(EmDcsConfType emConfType, String achConfE164, String achConfName, EmDcsConfMode emConfMode, List<TDCSConfUserInfo> atUserList, String achConfAdminE164, EmDcsType emAdminMtType) {
        this.emConfType = emConfType;
        this.achConfE164 = achConfE164;
        this.achConfName = achConfName;
        this.emConfMode = emConfMode;
        this.dwListNum = null==atUserList?0:atUserList.size();
        this.atUserList = atUserList;
        this.achConfAdminE164 = achConfAdminE164;
        this.emAdminMtType = emAdminMtType;
    }
}
