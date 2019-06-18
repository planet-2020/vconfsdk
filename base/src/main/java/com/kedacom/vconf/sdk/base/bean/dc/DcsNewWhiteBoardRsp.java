/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.base.bean.dc;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.base.tools.AbsJsonDeserializer;


@JsonAdapter(DcsNewWhiteBoardRsp.Deserializer.class)
public class DcsNewWhiteBoardRsp {
    public TDCSBoardResult MainParam;
    public TDCSBoardInfo AssParam;

    @Override
    public String toString() {
        return "DcsNewWhiteBoardRsp{" +
                "MainParam=" + MainParam +
                ", AssParam=" + AssParam +
                '}';
    }

    static final class Deserializer extends AbsJsonDeserializer<DcsNewWhiteBoardRsp> { }
}
