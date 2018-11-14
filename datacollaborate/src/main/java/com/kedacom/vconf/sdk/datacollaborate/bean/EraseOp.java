package com.kedacom.vconf.sdk.datacollaborate.bean;

public class EraseOp extends PaintOp {
    public float left;
    public float top;
    public float right;
    public float bottom;
    public EraseOp(float left, float top, float right, float bottom, int sn){
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.sn = sn;
        type = PaintOp.OP_ERASE;
        paintCfg = new PaintCfg(PaintCfg.MODE_ERASE);
    }
}
