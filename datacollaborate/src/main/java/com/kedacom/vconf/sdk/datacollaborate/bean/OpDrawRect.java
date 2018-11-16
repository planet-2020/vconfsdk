package com.kedacom.vconf.sdk.datacollaborate.bean;

public class OpDrawRect extends OpPaint {
    public float left;
    public float top;
    public float right;
    public float bottom;
    public OpDrawRect(float left, float top, float right, float bottom, int sn, PaintCfg paintCfg, String boardId){
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.sn = sn;
        this.paintCfg = paintCfg;
        this.boardId = boardId;
        type = OpPaint.OP_DRAW_RECT;
    }
}
