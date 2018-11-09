package com.kedacom.vconf.sdk.datacollaborate.bean;

public class DCUndoOp extends DCOp {
//    public boolean done;
    public DCUndoOp(int sn){
        this.sn = sn;
        type = DCOp.OP_UNDO;
    }
}
