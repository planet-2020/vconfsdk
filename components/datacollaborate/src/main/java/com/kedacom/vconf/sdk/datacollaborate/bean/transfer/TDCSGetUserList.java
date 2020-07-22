/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;

import androidx.annotation.NonNull;

import java.util.List;

public class TDCSGetUserList{
    public int dwListNum;
    public List<TDCSConfUserInfo> atUserList;

    @NonNull
    @Override
    public String toString() {
        return "TDCSGetUserList{" +
                "dwListNum=" + dwListNum +
                ", atUserList=" + atUserList +
                '}';
    }
}
