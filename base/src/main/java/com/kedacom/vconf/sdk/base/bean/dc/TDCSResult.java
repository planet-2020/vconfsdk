/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.base.bean.dc;

public class TDCSResult {
    public boolean bSucces;
    public int dwErrorCode;
    public String achConfE164;

    @Override
    public String toString() {
        return "TDCSResult{" +
                "bSucces=" + bSucces +
                ", dwErrorCode=" + dwErrorCode +
                ", achConfE164='" + achConfE164 + '\'' +
                '}';
    }
}
