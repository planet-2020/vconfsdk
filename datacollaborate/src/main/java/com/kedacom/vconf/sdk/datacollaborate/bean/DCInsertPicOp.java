package com.kedacom.vconf.sdk.datacollaborate.bean;

import android.graphics.Bitmap;

public class DCInsertPicOp extends DCOp {
    public Bitmap pic;
    public int picWidth;
    public int picHeight;
    public float insertPosX;
    public float insertPosY;
    public float[] matrixValue = new float[9];

    public DCInsertPicOp(Bitmap pic, int picWidth, int picHeight, float insertPosX, float insertPosY, String[] matrixValue, int sn){
        this.pic = pic;
        this.picWidth = picWidth;
        this.picHeight = picHeight;
        this.insertPosX = insertPosX;
        this.insertPosY = insertPosY;
        for (int i=0; i<matrixValue.length; ++i) {
            this.matrixValue[i] = Float.valueOf(matrixValue[i]);
        }
        this.sn = sn;
        type = DCOp.OP_INSERT_PICTURE;
        paintCfg = new DCPaintCfg(DCPaintCfg.MODE_PICTURE);
    }
}
