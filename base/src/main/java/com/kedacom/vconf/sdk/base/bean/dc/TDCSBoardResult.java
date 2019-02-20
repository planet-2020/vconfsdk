/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.base.bean.dc;

public class TDCSBoardResult {
    public boolean bSuccess;
    public int dwErrorCode;
    public String achConfE164;
    public String achTabId;
    public int dwPageId;

    public TDCSBoardResult() {
        bSuccess = true;
    }

    @Override
    public String toString() {
        return "TDCSBoardResult{" +
                "bSuccess=" + bSuccess +
                ", dwErrorCode=" + dwErrorCode +
                ", achConfE164='" + achConfE164 + '\'' +
                ", achTabId='" + achTabId + '\'' +
                ", dwPageId=" + dwPageId +
                '}';
    }
}
