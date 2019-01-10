package com.kedacom.vconf.sdk.datacollaborate.bean;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;

import java.io.File;
import java.util.UUID;

import androidx.annotation.NonNull;

public class OpInsertPic extends OpPaint {
    private String picId = UUID.randomUUID().toString();
    private String picName;
    private String picPath;
    private Bitmap pic;
    private int picWidth;
    private int picHeight;
    private PointF insertPos; // 插入位置（图片左上角所在位置的点）
    private Matrix initMatrix; // 初始位置的matrix
    private Matrix matrix; // 图片操作的matrix（不包含fullScreenMatrix中的matrix）

    public OpInsertPic(){
        type = EOpType.INSERT_PICTURE;
    }

    public OpInsertPic(String picPath, Matrix matrix){
        File file = new File(picPath);
        this.picPath = picPath;
        this.picName = file.getName();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(picPath, options);
        this.picWidth = options.outWidth;
        this.picHeight = options.outHeight;
        initMatrix = this.matrix = matrix;
        insertPos = new PointF();
        type = EOpType.INSERT_PICTURE;
    }

    public OpInsertPic(String picId, String picName, int picWidth, int picHeight, PointF insertPos, Matrix matrix){
        this.picId = picId;
        this.picName = picName;
        this.picWidth = picWidth;
        this.picHeight = picHeight;
        this.insertPos = insertPos;
        initMatrix = this.matrix = matrix;
        type = EOpType.INSERT_PICTURE;
    }

    @NonNull
    @Override
    public String toString() {
        return "{"+String.format("picId=%s, picName=%s, picPath=%s, pic=%s, picWidth=%s, picHeight=%s, insertPos=%s, matrix=%s",
                picId, picName, picPath, pic, picWidth, picHeight, insertPos, matrix)+super.toString()+"}";
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

    public String getPicPath() {
        return picPath;
    }

    public void setPicPath(String picPath) {
        this.picPath = picPath;
    }

    public Bitmap getPic() {
        return pic;
    }

    public void setPic(Bitmap pic) {
        this.pic = pic;
        picWidth = pic.getWidth();
        picHeight = pic.getHeight();
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

    public PointF getInsertPos() {
        return insertPos;
    }

    public void setInsertPos(PointF insertPos) {
        this.insertPos = insertPos;
    }

    public Matrix getInitMatrix() {
        return initMatrix;
    }

    public Matrix getMatrix() {
        return matrix;
    }

    public void setMatrix(Matrix matrix) {
        this.matrix = matrix;
    }
}
