package com.kedacom.vconf.sdk.datacollaborate.bean;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

public class OpInsertPic extends OpPaint {
    private String picId;
    private String picName;
    private String picSavePath;
    private Bitmap pic;
    private int picWidth;
    private int picHeight;
    private float insertPosX;
    private float insertPosY;
    private float[] matrixValue = new float[9];

    public OpInsertPic(){
        type = EOpType.INSERT_PICTURE;
    }

    public OpInsertPic(String picId, String picName, int picWidth, int picHeight,
                       float insertPosX, float insertPosY, float[] matrixValue){
        this.picId = picId;
        this.picName = picName;
        this.picWidth = picWidth;
        this.picHeight = picHeight;
        this.insertPosX = insertPosX;
        this.insertPosY = insertPosY;
        this.matrixValue = matrixValue;
        type = EOpType.INSERT_PICTURE;
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
        return "{"+String.format("picId=%s, picName=%s, picSavePath=%s, pic=%s, picWidth=%s, picHeight=%s, insertPosX=%s, insertPosY, matrix=%s",
                picId, picName, picSavePath, pic, picWidth, picHeight, insertPosX, insertPosY, stringBuffer.toString())+super.toString()+"}";
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

    public float getInsertPosX() {
        return insertPosX;
    }

    public void setInsertPosX(float insertPosX) {
        this.insertPosX = insertPosX;
    }

    public float getInsertPosY() {
        return insertPosY;
    }

    public void setInsertPosY(float insertPosY) {
        this.insertPosY = insertPosY;
    }

    public float[] getMatrixValue() {
        return matrixValue;
    }

    public void setMatrixValue(float[] matrixValue) {
        this.matrixValue = matrixValue;
    }
}
