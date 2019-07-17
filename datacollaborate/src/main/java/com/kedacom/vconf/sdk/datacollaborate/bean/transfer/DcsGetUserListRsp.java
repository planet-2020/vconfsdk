/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;

import androidx.annotation.NonNull;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.LameJsonAdapter;

@JsonAdapter(DcsGetUserListRsp.Adapter.class)
public class DcsGetUserListRsp{

    public TDCSResult MainParam;
    public TDCSGetUserList AssParam;

    @NonNull
    @Override
    public String toString() {
        return "DcsGetUserListRsp{" +
                "MainParam=" + MainParam +
                ", AssParam=" + AssParam +
                '}';
    }

    static final class Adapter extends LameJsonAdapter<DcsGetUserListRsp, TDCSResult, TDCSGetUserList> { }
}
