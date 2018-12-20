/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.base.bean.dc;
public class TDCSSwitchReq {
    public String achConfE164;
    public String achTabId;
    public int dwWbPageId;

    public TDCSSwitchReq(String achConfE164, String achTabId) {
        this.achConfE164 = achConfE164;
        this.achTabId = achTabId;
        this.dwWbPageId = 0;
    }
}
