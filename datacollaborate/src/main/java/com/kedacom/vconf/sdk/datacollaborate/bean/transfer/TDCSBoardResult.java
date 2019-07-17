/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;

import androidx.annotation.NonNull;

public class TDCSBoardResult {
    public boolean bSuccess;
    public int dwErrorCode;
    public String achConfE164;
    public String achTabId;
    public int dwPageId;

    public TDCSBoardResult() {
        bSuccess = true;
    }

    @NonNull
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
