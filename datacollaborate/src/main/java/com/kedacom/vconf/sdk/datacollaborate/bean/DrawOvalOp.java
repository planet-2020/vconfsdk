package com.kedacom.vconf.sdk.datacollaborate.bean;

public class DrawOvalOp extends PaintOp {
    public float left;
    public float top;
    public float right;
    public float bottom;

    public DrawOvalOp(float left, float top, float right, float bottom, int sn, PaintCfg paintCfg, String boardId){
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.sn = sn;
        this.paintCfg = paintCfg;
        this.boardId = boardId;
        type = PaintOp.OP_DRAW_OVAL;
    }
}
