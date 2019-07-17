package com.kedacom.vconf.sdk.datacollaborate.bean;


import androidx.annotation.NonNull;

/**
 * 更新图片。
 *
 * 因为插入图片操作到达时图片本身尚未下载到本地，彼时插入图片操作的“图片”是置空的，
 * 插入图片操作本身也无实际效果只作占位用。等到图片下载完成后需用此“更新图片”操作更新前面插入的图片。
 * */
public class OpUpdatePic extends OpPaint {
    private String picId;
    private String picSavePath;
    public OpUpdatePic(String boardId, String picId, String picSavePath){
        this.boardId = boardId;
        this.picId = picId;
        this.picSavePath = picSavePath;
        type = EOpType.UPDATE_PICTURE;
    }

    @NonNull
    @Override
    public String toString() {
        return "OpUpdatePic{" +
                "picId='" + picId + '\'' +
                ", picSavePath='" + picSavePath + '\'' +'\n'+
                super.toString() +
                '}';
    }

    public String getPicId() {
        return picId;
    }

    public void setPicId(String picId) {
        this.picId = picId;
    }

    public String getPicSavePath() {
        return picSavePath;
    }

    public void setPicSavePath(String picSavePath) {
        this.picSavePath = picSavePath;
    }
}
