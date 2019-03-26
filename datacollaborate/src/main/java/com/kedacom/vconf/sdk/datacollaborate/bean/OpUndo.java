package com.kedacom.vconf.sdk.datacollaborate.bean;


public class OpUndo extends OpPaint {
    public OpUndo(){
        type = EOpType.UNDO;
    }

    @Override
    public String toString() {
        return String.format("OpUndo{%s}", super.toString());
    }
}
