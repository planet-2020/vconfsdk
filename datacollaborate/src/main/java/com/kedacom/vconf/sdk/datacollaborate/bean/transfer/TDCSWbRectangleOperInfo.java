/*
 * Copyright (c) 2017 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;

public class TDCSWbRectangleOperInfo {
    public String achTabId;             // 白板tab id（guid）
    public int dwSubPageId;             // 子页面id
    public TDCSWbRectangle tRectangle;  // 矩形操作信息
    public TDCSWbRectangleOperInfo(String achTabId, TDCSWbRectangle tRectangle) {
        this(achTabId, 0, tRectangle);
    }
    public TDCSWbRectangleOperInfo(String achTabId, int dwSubPageId, TDCSWbRectangle tRectangle) {
        this.achTabId = achTabId;
        this.dwSubPageId = dwSubPageId;
        this.tRectangle = tRectangle;
    }
}
