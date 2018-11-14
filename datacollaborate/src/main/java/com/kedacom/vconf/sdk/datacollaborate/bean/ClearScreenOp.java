package com.kedacom.vconf.sdk.datacollaborate.bean;

public class ClearScreenOp extends PaintOp {
    public ClearScreenOp(int sn){
        this.sn = sn;
        type = PaintOp.OP_CLEAR_SCREEN;
    }
}
