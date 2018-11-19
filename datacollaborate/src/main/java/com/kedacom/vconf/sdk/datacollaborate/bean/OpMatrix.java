package com.kedacom.vconf.sdk.datacollaborate.bean;

public class OpMatrix extends OpPaint {
    public float[] matrixValue;
    public OpMatrix(float[] matrixValue, int sn, String boardId){
        this.matrixValue = matrixValue;
        this.sn = sn;
        this.boardId = boardId;
        type = OP_MATRIX;
    }
}
