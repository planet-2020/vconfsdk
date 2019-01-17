package com.kedacom.vconf.sdk.datacollaborate;

import android.graphics.Matrix;

import com.kedacom.vconf.sdk.base.KLog;

class MatrixHelper {

    private static float[] matrixVals = new float[9];

    static Matrix invert(Matrix matrix){
        Matrix invertedMatrix = new Matrix();
        matrix.invert(invertedMatrix);
        return invertedMatrix;
    }

    static float getTransX(Matrix matrix){
        matrix.getValues(matrixVals);
        return matrixVals[Matrix.MTRANS_X];
    }

    static float getTransY(Matrix matrix){
        matrix.getValues(matrixVals);
        return matrixVals[Matrix.MTRANS_Y];
    }

    static float getScaleX(Matrix matrix){
        matrix.getValues(matrixVals);
        return matrixVals[Matrix.MSCALE_X];
    }

    static float getScaleY(Matrix matrix){
        matrix.getValues(matrixVals);
        return matrixVals[Matrix.MSCALE_Y];
    }

    static float getScale(Matrix matrix){
        matrix.getValues(matrixVals);
        float scaleX = matrixVals[Matrix.MSCALE_X];
        float scaleY = matrixVals[Matrix.MSCALE_Y];
        return scaleX > scaleY ? scaleX : scaleY;
    }


    static float[] valStr2Float(String[] strMatrixValue){
        float[] matrixValue = new float[9];
        for (int i=0; i<9; ++i){
            KLog.p("strMatrixValue[%s]=%s", i, strMatrixValue[i]);
            matrixValue[i] = Float.valueOf(strMatrixValue[i]);
        }
        return matrixValue;
    }

    static String[] valFloat2Str(float[] matrixValue){
        String[] strMatrixValue = new String[9];
        for (int i=0; i<9; ++i){
            strMatrixValue[i] = Float.toString(matrixValue[i]);
        }
        return strMatrixValue;
    }


}
