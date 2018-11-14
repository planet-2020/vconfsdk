package com.kedacom.vconf.sdk.datacollaborate.bean;

public class UndoOp extends PaintOp {
//    public boolean done;
    public UndoOp(int sn){
        this.sn = sn;
        type = PaintOp.OP_UNDO;
    }
}
