package com.kedacom.vconf.sdk.datacollaborate.bean;

public class DCOvalOp extends DCOp {
    public float left;
    public float top;
    public float right;
    public float bottom;

    public DCOvalOp(float left, float top, float right, float bottom, int sn, DCPaintCfg paintCfg){
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.sn = sn;
        this.paintCfg = paintCfg;
        type = DCOp.OP_DRAW_OVAL;
    }
}
