/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.base.bean.dc;
public class TDCSWbTabPageIdInfo {
    public String achTabId;     // 白板tab id（guid）
    public int dwSubPageId;   	// 子页id

    public TDCSWbTabPageIdInfo(String achTabId, int dwSubPageId) {
        this.achTabId = achTabId;
        this.dwSubPageId = dwSubPageId;
    }
}
