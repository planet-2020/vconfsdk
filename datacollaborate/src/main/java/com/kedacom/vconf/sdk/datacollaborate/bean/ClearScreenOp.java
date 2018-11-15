package com.kedacom.vconf.sdk.datacollaborate.bean;

public class ClearScreenOp extends PaintOp {
    public ClearScreenOp(int sn, String boardId){
        this.sn = sn;
        this.boardId = boardId;
        type = PaintOp.OP_CLEAR_SCREEN;
    }
}
