/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;

import androidx.annotation.NonNull;

public class DcsDownloadImageRsp {

    public TDCSResult MainParam;
    public TDCSImageUrl AssParam;

    @NonNull
    @Override
    public String toString() {
        return "DcsDownloadImageRsp{" +
                "MainParam=" + MainParam +
                ", AssParam=" + AssParam +
                '}';
    }
}
