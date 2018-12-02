package com.kedacom.vconf.sdk.datacollaborate;

import android.view.View;

import com.kedacom.vconf.sdk.datacollaborate.bean.OpPaint;

public interface IPaintBoard {
    int STYLE_DEFAULT = 0;
    int STYLE_HAND = 1;
    int STYLE_DRAW_LINE = 2;
    int STYLE_DRAW_RECT = 3;
    int STYLE_DRAW_OVAL = 4;
    int STYLE_RECT_ERASE = 5;
    int STYLE_SELECT_PIC = 6;

    String getBoardId();
    View getBoardView();
//    void setPicPaintView(IPaintView paintView);
//    void setShapePaintView(IPaintView paintView);
    IPaintView getPicPaintView();
    IPaintView getShapePaintView();
    void focusLayer(int layer);

    void setStyle(int style);
    int getStyle();
    void setPaintStrokeWidth(int width);
    int getPaintStrokeWidth();
    void setPaintColor(int color);
    int getPaintColor();
    View snapshot();

    void setOnPaintOpGeneratedListener(IOnPaintOpGeneratedListener paintOpGeneratedListener);

    interface IOnPaintOpGeneratedListener{
        void onPaintOpGenerated(OpPaint Op);
    }
}
