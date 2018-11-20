package com.kedacom.vconf.sdk.datacollaborate.bean;

public class OpErase extends OpPaint {
    public float left;
    public float top;
    public float right;
    public float bottom;
    public OpErase(float left, float top, float right, float bottom, int sn, String boardId){
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.sn = sn;
        type = OP_ERASE;
        this.boardId = boardId;
        paintCfg = new PaintCfg(PaintCfg.MODE_ERASE);
    }
}
