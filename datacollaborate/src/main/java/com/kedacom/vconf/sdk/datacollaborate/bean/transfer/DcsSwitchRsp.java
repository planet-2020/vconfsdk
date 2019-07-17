/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;

import androidx.annotation.NonNull;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.LameJsonAdapter;

@JsonAdapter(DcsSwitchRsp.Adapter.class)
public class DcsSwitchRsp {
    public TDCSBoardResult MainParam;
    public TDCSBoardInfo AssParam;

    @NonNull
    @Override
    public String toString() {
        return "DcsSwitchRsp{" +
                "MainParam=" + MainParam +
                ", AssParam=" + AssParam +
                '}';
    }


    static final class Adapter extends LameJsonAdapter<DcsSwitchRsp, TDCSBoardResult, TDCSBoardInfo> { }
}
