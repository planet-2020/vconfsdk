package com.kedacom.vconf.sdk.datacollaborate.bean;

public class RedoOp extends PaintOp {
//    public boolean done;
    public RedoOp(int sn, String boardId){
        this.sn = sn;
        this.boardId = boardId;
        type = PaintOp.OP_REDO;
    }
}
