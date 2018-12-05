/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.base.bean.dc;

import java.util.List;

/**
 * Created by zhoutianjie on 2018/12/5.
 */

public class TDCSCreateConf {

    public EmDcsConfType emConfType;
    public String achConfE164;
    public String achConfName;
    public EmDcsConfMode emConfMode;

    public int dwListNum;
    public List<TDCSConfUserInfo> atUserList;

    public String achConfAdminE164;
    public EmDcsType     emAdminMtType;
}
