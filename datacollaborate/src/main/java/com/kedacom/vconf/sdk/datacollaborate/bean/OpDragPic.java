package com.kedacom.vconf.sdk.datacollaborate.bean;

import java.util.Map;

import androidx.annotation.NonNull;

public class OpDragPic extends OpPaint {

    private Map<String, float[]> picMatrices;

    public OpDragPic(){
        type = EOpType.DRAG_PICTURE;
    }

    public OpDragPic(Map<String, float[]> picMatrices) {
        this.picMatrices = picMatrices;
        type = EOpType.DRAG_PICTURE;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        for (String picId : picMatrices.keySet()){
            stringBuffer.append("[").append(picId).append("(");
            for (float val : picMatrices.get(picId)){
                stringBuffer.append(val).append(",");
            }
            stringBuffer.append(")").append("], ");
        }
        return "{"+String.format("picMatrices={%s}", stringBuffer.toString())+super.toString()+"}";
    }

    public Map<String, float[]> getPicMatrices() {
        return picMatrices;
    }

    public void setPicMatrices(Map<String, float[]> picMatrices) {
        this.picMatrices = picMatrices;
    }

}
