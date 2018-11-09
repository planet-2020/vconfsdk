package com.kedacom.vconf.sdk.datacollaborate.bean;

public class DCMatrixOp extends DCOp {
    public float[] matrixValue = new float[9];
    public DCMatrixOp(String[] matrixValue, int sn){
        for (int i=0; i<matrixValue.length; ++i) {
            this.matrixValue[i] = Float.valueOf(matrixValue[i]);
        }
        this.sn = sn;
        type = OP_MATRIX;
    }
    public DCMatrixOp(float[] matrixValue, int sn){
        this.matrixValue = matrixValue;
        this.sn = sn;
        type = OP_MATRIX;
    }
}
