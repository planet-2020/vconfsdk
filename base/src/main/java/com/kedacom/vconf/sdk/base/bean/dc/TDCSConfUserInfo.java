/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.base.bean.dc;
public class TDCSConfUserInfo {
    public String achE164;
    /** 只有在添加与会方，删除与会方用到 */
    public String achName;
    public EmDcsType emMttype;
    /** 暂时只在获取与会人员列表中有效 */
    public boolean bOnline;
    public boolean bIsOper;
    public boolean bIsConfAdmin;
}