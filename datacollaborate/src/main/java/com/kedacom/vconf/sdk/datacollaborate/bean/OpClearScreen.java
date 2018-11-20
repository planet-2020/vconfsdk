package com.kedacom.vconf.sdk.datacollaborate.bean;

public class OpClearScreen extends OpPaint {
    public OpClearScreen(int sn, String boardId){
        this.sn = sn;
        this.boardId = boardId;
        type = OP_CLEAR_SCREEN;
    }
}
