package com.kedacom.vconf.sdk.datacollaborate.bean;


public class OpMatrix extends OpPaint {
    private float[] matrixValue;

    public OpMatrix(){
        type = OP_MATRIX;
    }

    public OpMatrix(float[] matrixValue){
        this.matrixValue = matrixValue;
        type = OP_MATRIX;
    }


    public float[] getMatrixValue() {
        return matrixValue;
    }

    public void setMatrixValue(float[] matrixValue) {
        this.matrixValue = matrixValue;
    }
}
