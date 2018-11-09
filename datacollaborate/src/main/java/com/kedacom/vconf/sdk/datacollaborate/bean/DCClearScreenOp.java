package com.kedacom.vconf.sdk.datacollaborate.bean;

public class DCClearScreenOp extends DCOp {
    public DCClearScreenOp(int sn){
        this.sn = sn;
        type = DCOp.OP_CLEAR_SCREEN;
    }
}
