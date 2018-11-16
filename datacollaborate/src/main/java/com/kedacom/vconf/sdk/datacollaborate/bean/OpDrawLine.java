package com.kedacom.vconf.sdk.datacollaborate.bean;

public class OpDrawLine extends OpPaint {
    public float startX;
    public float startY;
    public float stopX;
    public float stopY;
    public OpDrawLine(float startX, float startY, float stopX, float stopY, int sn, PaintCfg paintCfg, String boardId){
        this.startX = startX;
        this.startY = startY;
        this.stopX = stopX;
        this.stopY = stopY;
        this.sn = sn;
        this.paintCfg = paintCfg;
        this.boardId = boardId;
        type = OpPaint.OP_DRAW_LINE;
    }
}
