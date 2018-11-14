package com.kedacom.vconf.sdk.datacollaborate.bean;

public class DrawRectOp extends PaintOp {
    public float left;
    public float top;
    public float right;
    public float bottom;
    public DrawRectOp(float left, float top, float right, float bottom, int sn, PaintCfg paintCfg){
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.sn = sn;
        this.paintCfg = paintCfg;
        type = PaintOp.OP_DRAW_RECT;
    }
}
