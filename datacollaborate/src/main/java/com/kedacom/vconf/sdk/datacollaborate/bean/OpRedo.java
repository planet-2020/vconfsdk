package com.kedacom.vconf.sdk.datacollaborate.bean;

public class OpRedo extends OpPaint {
//    public boolean done;
    public OpRedo(int sn, String boardId){
        this.sn = sn;
        this.boardId = boardId;
        type = OP_REDO;
    }
}
