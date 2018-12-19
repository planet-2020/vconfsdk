/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.base.bean.dc;

import java.util.List;
public class TDCSOperator{
    public String achConfE164;
    public int dwListNum;
    public List<TDCSConfUserInfo> atOperList;

    public TDCSOperator(String achConfE164, List<TDCSConfUserInfo> atOperList) {
        this.achConfE164 = achConfE164;
        this.dwListNum = null==atOperList?0:atOperList.size();
        this.atOperList = atOperList;
    }

}
