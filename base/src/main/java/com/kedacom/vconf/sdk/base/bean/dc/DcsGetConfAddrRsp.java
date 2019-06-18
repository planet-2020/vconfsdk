package com.kedacom.vconf.sdk.base.bean.dc;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.base.tools.AbsJsonDeserializer;

@JsonAdapter(DcsGetConfAddrRsp.Deserializer.class)
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

    static final class Deserializer extends AbsJsonDeserializer<DcsGetConfAddrRsp> { }
}
