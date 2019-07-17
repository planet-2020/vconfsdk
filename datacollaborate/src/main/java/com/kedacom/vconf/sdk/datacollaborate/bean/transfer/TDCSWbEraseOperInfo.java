/*
 * Copyright (c) 2017 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;

public class TDCSWbEraseOperInfo {
    public String achTabId;         // 白板tab id（guid）
    public int dwSubPageId;
    public TDCSWbPoint tBeginPt;    // 矩形擦除区域的开始坐标（此参数矩形擦除使用）
    public TDCSWbPoint tEndPt;      // 矩形擦除区域的结束坐标（此参数矩形擦除使用）
    public boolean bNexVision;

    public TDCSWbEraseOperInfo(String achTabId, TDCSWbPoint tBeginPt, TDCSWbPoint tEndPt) {
        this(achTabId, 0, tBeginPt, tEndPt, false);
    }
    public TDCSWbEraseOperInfo(String achTabId, int dwSubPageId, TDCSWbPoint tBeginPt, TDCSWbPoint tEndPt, boolean bNexVision) {
        this.achTabId = achTabId;
        this.dwSubPageId = dwSubPageId;
        this.tBeginPt = tBeginPt;
        this.tEndPt = tEndPt;
        this.bNexVision = bNexVision;
    }
}
