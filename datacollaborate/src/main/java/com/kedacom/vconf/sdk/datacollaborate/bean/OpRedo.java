package com.kedacom.vconf.sdk.datacollaborate.bean;


public class OpRedo extends OpPaint {
    public OpRedo(){
        type = EOpType.REDO;
    }

    @Override
    public String toString() {
        return String.format("OpRedo{%s}", super.toString());
    }
}
