/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.base.bean.dc;

import java.util.ArrayList;
import java.util.List;

public class TDCSGetAllBoard{

    public String achConfE164;
    public int dwBoardNum;
    public List<TDCSBoardInfo> atBoardInfo;

    public TDCSGetAllBoard() {
        atBoardInfo = new ArrayList<>();
        atBoardInfo.add(new TDCSBoardInfo());
    }

    @Override
    public String toString() {
        return "TDCSGetAllBoard{" +
                "achConfE164='" + achConfE164 + '\'' +
                ", dwBoardNum=" + dwBoardNum +
                ", atBoardInfo=" + atBoardInfo +
                '}';
    }
}
