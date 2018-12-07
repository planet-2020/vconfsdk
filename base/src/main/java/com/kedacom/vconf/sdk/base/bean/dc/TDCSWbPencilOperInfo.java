/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.base.bean.dc;
public class TDCSWbPencilOperInfo {
    public String achTabId;         // 白板tab id（guid）
    public int dwSubPageId;         // 子页面id
    public TDCSWbPencil tPencil;    // 铅笔操作信息

    public TDCSWbPencilOperInfo(String achTabId, int dwSubPageId, TDCSWbPencil tPencil) {
        this.achTabId = achTabId;
        this.dwSubPageId = dwSubPageId;
        this.tPencil = tPencil;
    }
}
