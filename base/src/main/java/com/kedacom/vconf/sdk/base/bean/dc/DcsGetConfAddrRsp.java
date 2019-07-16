package com.kedacom.vconf.sdk.base.bean.dc;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.base.tools.LameJsonAdapter;

@JsonAdapter(DcsGetConfAddrRsp.Adapter.class)
public class DcsGetConfAddrRsp {
    public TDCSResult MainParam;
    public TDCSConfAddr AssParam;

    @Override
    public String toString() {
        return "DcsGetConfAddrRsp{" +
                "MainParam=" + MainParam +
                ", AssParam=" + AssParam +
                '}';
    }

    static final class Adapter extends LameJsonAdapter<DcsGetConfAddrRsp> { }
}
