/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.base.bean.dc;

public class DcsDownloadImageRsp {

    public TDCSResult MainParam;
    public TDCSImageUrl AssParam;

    @Override
    public String toString() {
        return "DcsDownloadImageRsp{" +
                "MainParam=" + MainParam +
                ", AssParam=" + AssParam +
                '}';
    }
}
