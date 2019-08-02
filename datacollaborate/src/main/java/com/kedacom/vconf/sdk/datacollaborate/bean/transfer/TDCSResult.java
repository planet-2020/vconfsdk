/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;

import androidx.annotation.NonNull;

public class TDCSResult {
    public boolean bSuccess;
    public int dwErrorCode;
    public String achConfE164;

    public TDCSResult(boolean bSuccess, int dwErrorCode, String achConfE164) {
        this.bSuccess = bSuccess;
        this.dwErrorCode = dwErrorCode;
        this.achConfE164 = achConfE164;
    }

    @NonNull
    @Override
    public String toString() {
        return "TDCSResult{" +
                "bSuccess=" + bSuccess +
                ", dwErrorCode=" + dwErrorCode +
                ", achConfE164='" + achConfE164 + '\'' +
                '}';
    }
}
