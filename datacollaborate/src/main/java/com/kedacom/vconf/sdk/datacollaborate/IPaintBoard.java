package com.kedacom.vconf.sdk.datacollaborate;

import android.view.View;

public interface IPaintBoard {
    String getBoardId();
    View getBoardView();
//    void setPicPaintView(IPaintView paintView);
//    void setShapePaintView(IPaintView paintView);
    IPaintView getPicPaintView();
    IPaintView getShapePaintView();
    void focusLayer(int layer);
}
