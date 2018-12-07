/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.base.bean.dc;
public class TDCSWbZoomInfo {
    public String achTabId;     // 白板tab id（guid）
    public int dwZoom;          // 当前页缩放倍数，取百分制，例如100.0，对应100%

    public TDCSWbZoomInfo(String achTabId, int dwZoom) {
        this.achTabId = achTabId;
        this.dwZoom = dwZoom;
    }
}
