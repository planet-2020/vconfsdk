package com.kedacom.vconf.sdk.datacollaborate.bean;

public class OpMatrix extends OpPaint {
    public float[] matrixValue = new float[9];
    public OpMatrix(String[] matrixValue, int sn, String boardId){
        for (int i=0; i<matrixValue.length; ++i) {
            this.matrixValue[i] = Float.valueOf(matrixValue[i]);
        }
        this.sn = sn;
        this.boardId = boardId;
        type = OP_MATRIX;
    }
}
