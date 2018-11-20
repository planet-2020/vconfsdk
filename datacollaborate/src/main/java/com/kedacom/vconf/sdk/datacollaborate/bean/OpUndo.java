package com.kedacom.vconf.sdk.datacollaborate.bean;

public class OpUndo extends OpPaint {
//    public boolean done;
    public OpUndo(int sn, String boardId){
        this.sn = sn;
        this.boardId = boardId;
        type = OP_UNDO;
    }
}
