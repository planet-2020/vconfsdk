/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.base.bean.dc;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.base.tools.LameJsonAdapter;

@JsonAdapter(DcsSwitchRsp.Adapter.class)
public class DcsSwitchRsp {
    public TDCSBoardResult MainParam;
    public TDCSBoardInfo AssParam;

    @Override
    public String toString() {
        return "DcsSwitchRsp{" +
                "MainParam=" + MainParam +
                ", AssParam=" + AssParam +
                '}';
    }


    static final class Adapter extends LameJsonAdapter<DcsSwitchRsp> { }
}
