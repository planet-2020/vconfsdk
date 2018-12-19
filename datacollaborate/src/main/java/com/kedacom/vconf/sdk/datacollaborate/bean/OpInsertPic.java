package com.kedacom.vconf.sdk.datacollaborate.bean;

import android.graphics.Bitmap;
import android.graphics.Matrix;

import androidx.annotation.NonNull;

public class OpInsertPic extends OpPaint {
    private String picId;
    private String picName;
    private String picSavePath;
    private Bitmap pic;
    private int picWidth;
    private int picHeight;
    private Matrix matrix;

    public OpInsertPic(){
        type = EOpType.INSERT_PICTURE;
    }

    public OpInsertPic(String picId, String picName, int picWidth, int picHeight, Matrix matrix){
        this.picId = picId;
        this.picName = picName;
        this.picWidth = picWidth;
        this.picHeight = picHeight;
        this.matrix = matrix;
        type = EOpType.INSERT_PICTURE;
    }

    @NonNull
    @Override
    public String toString() {
        return "{"+String.format("picId=%s, picName=%s, picSavePath=%s, pic=%s, picWidth=%s, picHeight=%s, matrix=%s",
                picId, picName, picSavePath, pic, picWidth, picHeight, matrix)+super.toString()+"}";
    }

    public String getPicId() {
        return picId;
    }

    public void setPicId(String picId) {
        this.picId = picId;
    }

    public String getPicName() {
        return picName;
    }

    public void setPicName(String picName) {
        this.picName = picName;
    }

    public String getPicSavePath() {
        return picSavePath;
    }

    public void setPicSavePath(String picSavePath) {
        this.picSavePath = picSavePath;
    }

    public Bitmap getPic() {
        return pic;
    }

    public void setPic(Bitmap pic) {
        this.pic = pic;
    }

    public int getPicWidth() {
        return picWidth;
    }

    public void setPicWidth(int picWidth) {
        this.picWidth = picWidth;
    }

    public int getPicHeight() {
        return picHeight;
    }

    public void setPicHeight(int picHeight) {
        this.picHeight = picHeight;
    }


    public float[] getMatrixValue() {
        float[] matrixValue = new float[9];
        matrix.getValues(matrixValue);
        return matrixValue;
    }

    public void setMatrixValue(float[] matrixValue) {
        matrix.setValues(matrixValue);
    }

    public Matrix getMatrix() {
        return matrix;
    }

    public void setMatrix(Matrix matrix) {
        this.matrix = matrix;
    }
}
