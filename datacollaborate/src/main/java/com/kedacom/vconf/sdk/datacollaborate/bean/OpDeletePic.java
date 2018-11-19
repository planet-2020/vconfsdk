package com.kedacom.vconf.sdk.datacollaborate.bean;

public class OpDeletePic extends OpPaint {
    public String[] picIds;
    public OpDeletePic(String[] picIds, int sn, String boardId){
        this.picIds = picIds;
        this.sn = sn;
        this.boardId = boardId;
        type = OpPaint.OP_DELETE_PICTURE;
    }
}
