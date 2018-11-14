package com.kedacom.vconf.sdk.datacollaborate.bean;

public class RedoOp extends PaintOp {
//    public boolean done;
    public RedoOp(int sn){
        this.sn = sn;
        type = PaintOp.OP_REDO;
    }
}
