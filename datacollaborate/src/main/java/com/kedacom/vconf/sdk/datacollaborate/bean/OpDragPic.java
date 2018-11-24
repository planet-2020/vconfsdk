package com.kedacom.vconf.sdk.datacollaborate.bean;

import java.util.Map;

public class OpDragPic extends OpPaint {

    private Map<String, float[]> picMatrices;

    public OpDragPic(){
        type = OP_DRAG_PICTURE;
    }

    public OpDragPic(Map<String, float[]> picMatrices) {
        this.picMatrices = picMatrices;
        type = OP_DRAG_PICTURE;
    }


    public Map<String, float[]> getPicMatrices() {
        return picMatrices;
    }

    public void setPicMatrices(Map<String, float[]> picMatrices) {
        this.picMatrices = picMatrices;
    }

}
