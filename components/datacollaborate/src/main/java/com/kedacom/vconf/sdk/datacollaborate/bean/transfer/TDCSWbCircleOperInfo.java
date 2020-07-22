/*
 * Copyright (c) 2017 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;

public class TDCSWbCircleOperInfo {
    public String achTabId;         // 白板tab id（guid）
    public int dwSubPageId;         // 子页面id
    public TDCSWbCircle tCircle;    // 圆操作信息

    public TDCSWbCircleOperInfo(String achTabId, TDCSWbCircle tCircle) {
        this(achTabId, 0, tCircle);
    }
    public TDCSWbCircleOperInfo(String achTabId, int dwSubPageId, TDCSWbCircle tCircle) {
        this.achTabId = achTabId;
        this.dwSubPageId = dwSubPageId;
        this.tCircle = tCircle;
    }
}
