/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.base.bean.dc;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.base.tools.MainAssParaJsonAdapter;

@JsonAdapter(DcsGetWhiteBoardRsp.Adapter.class)
public class DcsGetWhiteBoardRsp {
    public TDCSBoardResult MainParam;
    public TDCSBoardInfo AssParam;

    @Override
    public String toString() {
        return "DcsGetWhiteBoardRsp{" +
                "MainParam=" + MainParam +
                ", AssParam=" + AssParam +
                '}';
    }

    static final class Adapter extends MainAssParaJsonAdapter<DcsGetWhiteBoardRsp> { }
}
