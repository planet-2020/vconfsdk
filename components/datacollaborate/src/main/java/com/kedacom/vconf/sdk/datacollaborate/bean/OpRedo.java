package com.kedacom.vconf.sdk.datacollaborate.bean;


import androidx.annotation.NonNull;

public class OpRedo extends OpPaint {
    public OpRedo(){
        type = EOpType.REDO;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("OpRedo{%s}", super.toString());
    }
}
