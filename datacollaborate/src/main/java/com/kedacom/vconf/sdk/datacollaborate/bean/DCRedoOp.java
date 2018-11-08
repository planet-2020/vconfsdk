package com.kedacom.vconf.sdk.datacollaborate.bean;

public class DCRedoOp extends DCOp {
    public boolean done;
    public DCRedoOp(int sn){
        this.sn = sn;
        type = DCOp.OP_REDO;
    }
}
