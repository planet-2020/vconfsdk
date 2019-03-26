package com.kedacom.vconf.sdk.datacollaborate.bean;

import android.graphics.Matrix;

import java.util.Map;

public class OpZoomPic extends OpPaint {

    private Map<String, Matrix> picMatrices;

    public OpZoomPic(){
        type = EOpType.ZOOM_PICTURE;
    }

    public OpZoomPic(Map<String, Matrix> picMatrices) {
        this.picMatrices = picMatrices;
        type = EOpType.ZOOM_PICTURE;
    }

    @Override
    public String toString() {
        return "OpZoomPic{" +
                "picMatrices=" + picMatrices +
                super.toString() +'\n'+
                '}';
    }

    public Map<String, Matrix> getPicMatrices() {
        return picMatrices;
    }

    public void setPicMatrices(Map<String, Matrix> picMatrices) {
        this.picMatrices = picMatrices;
    }
}
