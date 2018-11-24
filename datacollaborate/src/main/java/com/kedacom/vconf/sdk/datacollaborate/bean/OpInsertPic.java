package com.kedacom.vconf.sdk.datacollaborate.bean;

import android.graphics.Bitmap;

public class OpInsertPic extends OpPaint {
    private String picId;
    private String picName;
    private Bitmap pic;
    private int picWidth;
    private int picHeight;
    private float insertPosX;
    private float insertPosY;
    private float[] matrixValue = new float[9];

    public OpInsertPic(){
        type = OP_INSERT_PICTURE;
    }

    public OpInsertPic(String picId, String picName, Bitmap pic, int picWidth, int picHeight,
                       float insertPosX, float insertPosY, float[] matrixValue){
        this.picId = picId;
        this.picName = picName;
        this.pic = pic;
        this.picWidth = picWidth;
        this.picHeight = picHeight;
        this.insertPosX = insertPosX;
        this.insertPosY = insertPosY;
        this.matrixValue = matrixValue;
        type = OP_INSERT_PICTURE;
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
