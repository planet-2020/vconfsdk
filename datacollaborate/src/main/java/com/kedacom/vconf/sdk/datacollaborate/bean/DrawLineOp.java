package com.kedacom.vconf.sdk.datacollaborate.bean;

public class DrawLineOp extends PaintOp {
    public float startX;
    public float startY;
    public float stopX;
    public float stopY;
    public DrawLineOp(float startX, float startY, float stopX, float stopY, int sn, PaintCfg paintCfg, String boardId){
        this.startX = startX;
        this.startY = startY;
        this.stopX = stopX;
        this.stopY = stopY;
        this.sn = sn;
        this.paintCfg = paintCfg;
        this.boardId = boardId;
        type = PaintOp.OP_DRAW_LINE;
    }
}
