/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.base.bean.dc;
public class TDCSWbDisPlayInfo {
    public String achTabId;        //tab白板页
    public int dwSubPageId;        //子页ID
    public String[] aachMatrixValue;     //滚动到的目标点坐标

    public TDCSWbDisPlayInfo(String achTabId, int dwSubPageId, String[] aachMatrixValue) {
        this.achTabId = achTabId;
        this.dwSubPageId = dwSubPageId;
        this.aachMatrixValue = aachMatrixValue;
    }
}
