/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;

import androidx.annotation.NonNull;

public class TDCSResult {
    public boolean bSucces;
    public int dwErrorCode;
    public String achConfE164;

    public TDCSResult() {
        bSucces = true;
    }

    @NonNull
    @Override
    public String toString() {
        return "TDCSResult{" +
                "bSucces=" + bSucces +
                ", dwErrorCode=" + dwErrorCode +
                ", achConfE164='" + achConfE164 + '\'' +
                '}';
    }
}
