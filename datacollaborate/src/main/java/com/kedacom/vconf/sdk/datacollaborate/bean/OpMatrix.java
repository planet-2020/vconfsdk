package com.kedacom.vconf.sdk.datacollaborate.bean;


import android.graphics.Matrix;

import androidx.annotation.NonNull;

public class OpMatrix extends OpPaint {
    private Matrix matrix = new Matrix();

    public OpMatrix(){
        type = EOpType.FULLSCREEN_MATRIX;
    }

    public OpMatrix(Matrix matrix){
        this.matrix.set(matrix);
        type = EOpType.FULLSCREEN_MATRIX;
    }

    public OpMatrix(float[] matrixValue){
        matrix.setValues(matrixValue);
        type = EOpType.FULLSCREEN_MATRIX;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        float[] matrixValue = new float[9];
        matrix.getValues(matrixValue);
        stringBuffer.append("[");
        for (float val : matrixValue){
            stringBuffer.append(val).append(",");
        }
        stringBuffer.append("]");
        return "{"+String.format("matrix=%s", stringBuffer.toString())+super.toString()+"}";
    }

    public float[] getMatrixValue() {
        float[] matrixValue = new float[9];
        matrix.getValues(matrixValue);
        return matrixValue;
    }

    public Matrix getMatrix() {
        return matrix;
    }

}
