package com.kedacom.vconf.sdk.datacollaborate;

import android.graphics.Paint;
import android.view.View;

public interface IPaintView { // TODO 删掉，功能集中在board中

//    int TYPE_PIC = 10;
//    int TYPE_SHAPE = 11;

    int STYLE_PENCIL = 0;
    int STYLE_LINE = 1;
    int STYLE_RECT = 2;
    int STYLE_OVAL = 3;
    int STYLE_ERASER = 4;
    int STYLE_HAND = 5;

//    String getMyId();
//    int getType();

    String setBoardId(String boardId);
    String getBoardId();

    View getPaintView();

//    void setToolType(int toolType);
//    void setPaint(Paint paint);
//    View snapshoot();
//
//    void setOnPaintOpGeneratedListener(IOnPaintOpGeneratedListener paintOpGeneratedListener);
//
//    interface IOnPaintOpGeneratedListener{
//        void onPaintOpGenerated(Paint Op);
//    }
}
