package com.kedacom.vconf.sdk.datacollaborate.bean;


import android.graphics.Matrix;

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

    @Override
    public String toString() {
        return "OpMatrix{" +
                "matrix=" + matrix +'\n'+
                super.toString() +
                '}';
    }

    private float[] matrixValue = new float[9];
    public float[] getMatrixValue() {
        matrix.getValues(matrixValue);
        return matrixValue;
    }

    public Matrix getMatrix() {
        return matrix;
    }

}
