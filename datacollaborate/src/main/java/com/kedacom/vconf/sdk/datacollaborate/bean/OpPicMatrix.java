package com.kedacom.vconf.sdk.datacollaborate.bean;

public class OpPicMatrix extends OpMatrix {
    public OpPicMatrix(String[] matrixValue, int sn, String boardId) {
        super(matrixValue, sn, boardId);
        type = OP_PIC_MATRIX;
    }
}
