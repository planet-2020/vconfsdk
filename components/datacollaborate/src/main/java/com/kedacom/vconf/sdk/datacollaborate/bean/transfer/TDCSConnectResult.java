/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;

public class TDCSConnectResult {
    public boolean bSuccess;
    public EmDcsConnectErrCode dwErrorCode;

    public TDCSConnectResult(boolean bSuccess) {
        this.bSuccess = bSuccess;
    }
}
