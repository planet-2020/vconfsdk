package com.kedacom.vconf.sdk.datacollaborate.bean;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

/**
 * 更新图片。
 *
 * 因为插入图片操作到达时图片本身尚未下载到本地，彼时插入图片操作的“图片”是置空的，
 * 插入图片操作本身也无实际效果只作占位用。等到图片下载完成后需用此“更新图片”操作更新前面插入的图片。
 * */
public class OpUpdatePic extends OpPaint {
    public String picId;
    public Bitmap pic;
    public OpUpdatePic(String boardId, String picId, Bitmap pic){
        this.boardId = boardId;
        this.picId = picId;
        this.pic = pic;
        type = OP_UPDATE_PICTURE;
    }

    @NonNull
    @Override
    public String toString() {
        return "{"+String.format("picId=%s, pic=%s", picId, pic)+super.toString()+"}";
    }
}
