/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;

import androidx.annotation.NonNull;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.LameJsonAdapter;

@JsonAdapter(DcsGetWhiteBoardRsp.Adapter.class)
public class DcsGetWhiteBoardRsp {
    public TDCSBoardResult MainParam;
    public TDCSBoardInfo AssParam;

    @NonNull
    @Override
    public String toString() {
        return "DcsGetWhiteBoardRsp{" +
                "MainParam=" + MainParam +
                ", AssParam=" + AssParam +
                '}';
    }

    static final class Adapter extends LameJsonAdapter<DcsGetWhiteBoardRsp, TDCSBoardResult, TDCSBoardInfo> { }
}
