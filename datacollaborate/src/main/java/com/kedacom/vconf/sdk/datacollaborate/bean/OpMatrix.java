package com.kedacom.vconf.sdk.datacollaborate.bean;


import androidx.annotation.NonNull;

public class OpMatrix extends OpPaint {
    private float[] matrixValue;

    public OpMatrix(){
        type = OP_MATRIX;
    }

    public OpMatrix(float[] matrixValue){
        this.matrixValue = matrixValue;
        type = OP_MATRIX;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("[");
        for (float val : matrixValue){
            stringBuffer.append(val).append(",");
        }
        stringBuffer.append("]");
        return "{"+String.format("matrix=%s", stringBuffer.toString())+super.toString()+"}";
    }

    public float[] getMatrixValue() {
        return matrixValue;
    }

    public void setMatrixValue(float[] matrixValue) {
        this.matrixValue = matrixValue;
    }
}
