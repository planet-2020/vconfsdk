/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.base.bean.dc;
public class TDCSWbPencilOperInfo {
    public String achTabId;         // 白板tab id（guid）
    public int dwSubPageId;         // 子页面id
    public TDCSWbPencil tPencil;    // 铅笔操作信息
    public boolean bFinished; // 是否结束（画曲线是增量同步的）

    public TDCSWbPencilOperInfo(String achTabId, TDCSWbPencil tPencil) {
        this(achTabId, 0, tPencil, false);
    }
    public TDCSWbPencilOperInfo(String achTabId, int dwSubPageId, TDCSWbPencil tPencil, boolean bFinished) {
        this.achTabId = achTabId;
        this.dwSubPageId = dwSubPageId;
        this.tPencil = tPencil;
        this.bFinished = bFinished;
    }
}
