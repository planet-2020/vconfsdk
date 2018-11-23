package com.kedacom.vconf.sdk.datacollaborate.bean;

import android.graphics.Bitmap;
import android.graphics.PointF;

import com.kedacom.vconf.sdk.base.MsgBeans;

public class OpInsertPic extends OpPaint {
    private String picId;
    private Bitmap pic;
    private int picWidth;
    private int picHeight;
    private float insertPosX;
    private float insertPosY;
    private float[] matrixValue = new float[9];

    public OpInsertPic(){
        type = OP_INSERT_PICTURE;
    }

    public OpInsertPic(String picId, Bitmap pic, int picWidth, int picHeight, float insertPosX, float insertPosY, String[] matrixValue){
        this.picId = picId;
        this.pic = pic;
        this.picWidth = picWidth;
        this.picHeight = picHeight;
        this.insertPosX = insertPosX;
        this.insertPosY = insertPosY;
        this.matrixValue = matrixValueStr2Float(matrixValue);
        type = OP_INSERT_PICTURE;
    }


    public OpInsertPic fromTransferObj(MsgBeans.DCInertPicOp to) {
        super.fromTransferObj(to);
        picId = to.picId;
        picWidth = to.width;
        picHeight = to.height;
        insertPosX = to.dstPos.x;
        insertPosY = to.dstPos.y;
        matrixValue = matrixValueStr2Float(to.matrixValue);
        return this;
    }

    public MsgBeans.DCInertPicOp toTransferObj(MsgBeans.DCInertPicOp to) {
        super.toTransferObj(to);
        to.picId = picId;
        to.width = picWidth;
        to.height = picHeight;
        to.dstPos = new PointF(insertPosX, insertPosY);
        to.matrixValue = matrixValueFloat2Str(matrixValue);
        return to;
    }


    public String getPicId() {
        return picId;
    }

    public void setPicId(String picId) {
        this.picId = picId;
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
