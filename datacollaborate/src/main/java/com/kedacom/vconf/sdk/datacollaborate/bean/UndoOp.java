package com.kedacom.vconf.sdk.datacollaborate.bean;

public class UndoOp extends PaintOp {
//    public boolean done;
    public UndoOp(int sn, String boardId){
        this.sn = sn;
        this.boardId = boardId;
        type = PaintOp.OP_UNDO;
    }
}
