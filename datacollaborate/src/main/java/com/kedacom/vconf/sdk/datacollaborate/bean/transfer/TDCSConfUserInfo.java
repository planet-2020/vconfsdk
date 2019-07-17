/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;

import androidx.annotation.NonNull;

public class TDCSConfUserInfo {
    public String achE164;
    /** 只有在添加与会方，删除与会方用到 */
    public String achName;
    public EmDcsType emMttype;
    /** 暂时只在获取与会人员列表中有效 */
    public boolean bOnline;
    public boolean bIsOper;
    public boolean bIsConfAdmin;

    public TDCSConfUserInfo(String achE164, String achName, EmDcsType emMttype, boolean bOnline, boolean bIsOper, boolean bIsConfAdmin) {
        this.achE164 = achE164;
        this.achName = achName;
        this.emMttype = emMttype;
        this.bOnline = bOnline;
        this.bIsOper = bIsOper;
        this.bIsConfAdmin = bIsConfAdmin;
    }

    @NonNull
    @Override
    public String toString() {
        return "TDCSConfUserInfo{" +
                "achE164='" + achE164 + '\'' +
                ", achName='" + achName + '\'' +
                ", emMttype=" + emMttype +
                ", bOnline=" + bOnline +
                ", bIsOper=" + bIsOper +
                ", bIsConfAdmin=" + bIsConfAdmin +
                '}';
    }
}
