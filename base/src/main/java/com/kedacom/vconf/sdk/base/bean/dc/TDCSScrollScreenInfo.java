/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.base.bean.dc;
public class TDCSScrollScreenInfo {
    public String achTabId; 	    //tab白板页
    public int  dwSubPageId;        //子页ID
    public TDCSWbPoint  tPoint;     //滚动到的目标点坐标

    public TDCSScrollScreenInfo(String achTabId, TDCSWbPoint tPoint) {
        this(achTabId, 0, tPoint);
    }
    public TDCSScrollScreenInfo(String achTabId, int dwSubPageId, TDCSWbPoint tPoint) {
        this.achTabId = achTabId;
        this.dwSubPageId = dwSubPageId;
        this.tPoint = tPoint;
    }
}
