/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;


import androidx.annotation.NonNull;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.LameJsonAdapter;

@JsonAdapter(DcsGetAllWhiteBoardRsp.Adapter.class)  // 指定json反序列化的解析器
public class DcsGetAllWhiteBoardRsp{
    public TDCSResult MainParam;
    public TDCSGetAllBoard AssParam;

    @NonNull
    @Override
    public String toString() {
        return "DcsGetAllWhiteBoardRsp{" +
                "MainParam=" + MainParam +
                ", AssParam=" + AssParam +
                '}';
    }

    static final class Adapter extends LameJsonAdapter<DcsGetAllWhiteBoardRsp, TDCSResult, TDCSGetAllBoard> { }

}
