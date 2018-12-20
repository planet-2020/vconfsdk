/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.base.bean.dc;

public class TDCSNewWhiteBoard{
    public String achConfE164;
    public TDCSBoardInfo tBoardinfo;

    public TDCSNewWhiteBoard(String achConfE164, TDCSBoardInfo tBoardinfo) {
        this.achConfE164 = achConfE164;
        this.tBoardinfo = tBoardinfo;
    }
}
