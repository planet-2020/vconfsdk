package com.kedacom.vconf.sdk.datacollaborate.bean;

public class DCEraseOp extends DCOp {
    public float left;
    public float top;
    public float right;
    public float bottom;
    public DCEraseOp(float left, float top, float right, float bottom, int sn){
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.sn = sn;
        type = DCOp.OP_ERASE;
        paintCfg = new DCPaintCfg(DCPaintCfg.MODE_ERASE);
    }
}
