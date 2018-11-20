package com.kedacom.vconf.sdk.datacollaborate.bean;

import java.util.Map;

public class OpDragPic extends OpPaint {
    public Map<String, float[]> picsMatrix;
    public OpDragPic(Map<String, float[]> picsMatrix, int sn, String boardId) {
        this.picsMatrix = picsMatrix;
        this.sn = sn;
        this.boardId = boardId;
        type = OP_DRAG_PICTURE;
    }
}
