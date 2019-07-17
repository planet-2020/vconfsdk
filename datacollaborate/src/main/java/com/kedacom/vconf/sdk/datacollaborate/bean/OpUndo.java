package com.kedacom.vconf.sdk.datacollaborate.bean;


import androidx.annotation.NonNull;

public class OpUndo extends OpPaint {
    public OpUndo(){
        type = EOpType.UNDO;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("OpUndo{%s}", super.toString());
    }
}
