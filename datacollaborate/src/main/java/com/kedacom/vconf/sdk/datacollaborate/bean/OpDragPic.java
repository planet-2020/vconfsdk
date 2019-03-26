package com.kedacom.vconf.sdk.datacollaborate.bean;

import android.graphics.Matrix;

import java.util.Map;

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

    @Override
    public String toString() {
        return "OpDragPic{" +
                "picMatrices=" + picMatrices +'\n'+
                super.toString() +
                '}';
    }

    public Map<String, Matrix> getPicMatrices() {
        return picMatrices;
    }

    public void setPicMatrices(Map<String, Matrix> picMatrices) {
        this.picMatrices = picMatrices;
    }
}
