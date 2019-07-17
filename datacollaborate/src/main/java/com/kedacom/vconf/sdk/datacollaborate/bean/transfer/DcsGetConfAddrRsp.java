package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;

import androidx.annotation.NonNull;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.LameJsonAdapter;

@JsonAdapter(DcsGetConfAddrRsp.Adapter.class)
public class DcsGetConfAddrRsp {
    public TDCSResult MainParam;
    public TDCSConfAddr AssParam;

    @NonNull
    @Override
    public String toString() {
        return "DcsGetConfAddrRsp{" +
                "MainParam=" + MainParam +
                ", AssParam=" + AssParam +
                '}';
    }

    static final class Adapter extends LameJsonAdapter<DcsGetConfAddrRsp, TDCSResult, TDCSConfAddr> { }
}
