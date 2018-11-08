package com.kedacom.vconf.sdk.datacollaborate.bean;

public class DCLineOp extends DCOp {
    public float startX;
    public float startY;
    public float stopX;
    public float stopY;
    public DCLineOp(float startX, float startY, float stopX, float stopY, int sn, DCPaintCfg paintCfg){
        this.startX = startX;
        this.startY = startY;
        this.stopX = stopX;
        this.stopY = stopY;
        this.sn = sn;
        this.paintCfg = paintCfg;
        type = DCOp.OP_DRAW_LINE;
    }
}
