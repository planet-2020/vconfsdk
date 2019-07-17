package com.kedacom.vconf.sdk.utils.math;

import android.graphics.Matrix;
import android.graphics.RectF;

public class MatrixHelper {

    private static float[] matrixVals = new float[9];

    public static Matrix invert(Matrix matrix){
        Matrix invertedMatrix = new Matrix();
        matrix.invert(invertedMatrix);
        return invertedMatrix;
    }

    public static float getTransX(Matrix matrix){
        matrix.getValues(matrixVals);
        return matrixVals[Matrix.MTRANS_X];
    }

    public static float getTransY(Matrix matrix){
        matrix.getValues(matrixVals);
        return matrixVals[Matrix.MTRANS_Y];
    }

    public static float getScaleX(Matrix matrix){
        matrix.getValues(matrixVals);
        return matrixVals[Matrix.MSCALE_X];
    }

    public static float getScaleY(Matrix matrix){
        matrix.getValues(matrixVals);
        return matrixVals[Matrix.MSCALE_Y];
    }

    public static float getScale(Matrix matrix){
        matrix.getValues(matrixVals);
        float scaleX = matrixVals[Matrix.MSCALE_X];
        float scaleY = matrixVals[Matrix.MSCALE_Y];
        return scaleX > scaleY ? scaleX : scaleY;
    }


    public static float[] valStr2Float(String[] strMatrixValue){
        float[] matrixValue = new float[9];
        for (int i=0; i<9; ++i){
            if (null == strMatrixValue[i] || strMatrixValue[i].isEmpty()) {
                if (0==i||4==i||8==i){
                    matrixValue[i] = 1;
                }else{
                    matrixValue[i] = 0;
                }
            }else{
                matrixValue[i] = Float.valueOf(strMatrixValue[i]);
            }
        }
        return matrixValue;
    }

    public static String[] valFloat2Str(float[] matrixValue){
        String[] strMatrixValue = new String[9];
        for (int i=0; i<9; ++i){
            strMatrixValue[i] = Float.toString(matrixValue[i]);
        }
        return strMatrixValue;
    }

    /**
     * 求取从源矩形变换到目标矩形所需要的矩阵。
     * @param srcRect 源矩形
     * @param dstRect 目标矩形
     * @return 从源矩形变换到目标矩形所需要的矩阵。
     * */
    public static Matrix calcTransMatrix(RectF srcRect, RectF dstRect){
        RectF tmpRect = new RectF();
        Matrix matrix = new Matrix();
        // 求取源矩形变换到目标矩形所需的缩放因子
        float scaleX = dstRect.width()/srcRect.width();
        float scaleY = dstRect.height()/srcRect.height();

        // 缩放源矩形使得尺寸跟目标矩形一致
        matrix.postScale(scaleX, scaleY);
        matrix.mapRect(tmpRect, srcRect);

        // 移动缩放后的源矩形使得位置跟目标矩形重合
        matrix.postTranslate(dstRect.left-tmpRect.left, dstRect.top-tmpRect.top);

        return matrix;
    }

}
