package com.kedacom.vconf.sdk.datacollaborate;

import android.view.View;

public interface IPaintBoard {
    default String getMyId() {
        return null;
    }

    View getBoardView();
    void setPicPaintView(IPaintView paintView);  //XXX 如何保证设置下去的是DefaultPaintView?
    void setShapePaintView(IPaintView paintView);
    IPaintView getPicPaintView();
    IPaintView getShapePaintView();
    void focusLayer(int layer);
}
