package com.kedacom.vconf.sdk.datacollaborate.bean;

import android.graphics.Matrix;

import java.util.Map;

import androidx.annotation.NonNull;

public class OpDragPic extends OpPaint {

    /**
     * 拖动图片传过来的matrix，记为dragMatrix，
     * 图片最终位置=mixMatrix*dragMatrix
     * mixMatrix在插入图片时计算得到，参见{@link OpInsertPic#mixMatrix}
     * */
    private Map<String, Matrix> picMatrices;

    public OpDragPic(){
        type = EOpType.DRAG_PICTURE;
    }

    public OpDragPic(Map<String, Matrix> picMatrices) {
        this.picMatrices = picMatrices;
        type = EOpType.DRAG_PICTURE;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        for (String picId : picMatrices.keySet()){
            stringBuffer.append(picMatrices.get(picId)).append(", ");
        }
        return "{"+String.format("picMatrices={%s}", stringBuffer.toString())+super.toString()+"}";
    }

    public Map<String, Matrix> getPicMatrices() {
        return picMatrices;
    }

    public void setPicMatrices(Map<String, Matrix> picMatrices) {
        this.picMatrices = picMatrices;
    }
}
