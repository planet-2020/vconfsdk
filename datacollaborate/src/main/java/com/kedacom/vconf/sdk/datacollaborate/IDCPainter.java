package com.kedacom.vconf.sdk.datacollaborate;

import android.graphics.Bitmap;
import android.graphics.Path;
import android.graphics.Rect;

import com.kedacom.vconf.sdk.datacollaborate.bean.DCPaintInfo;

public interface IDCPainter {
    void onWhiteBoard();
    default void startBatchDraw(){}
    void drawLine(float startX, float startY, float stopX, float stopY, DCPaintInfo paintInfo);
    void drawRect(float left, float top, float right, float bottom, DCPaintInfo paintInfo);
    void drawOval(float left, float top, float right, float bottom, DCPaintInfo paintInfo);
    void drawPath(Path path, DCPaintInfo paintInfo);
    void drawBitmap(Bitmap bitmap, Rect src, Rect dst, DCPaintInfo paintInfo);
    void erase(float left, float top, float right, float bottom, DCPaintInfo paintInfo);
    default void finishBatchDraw(){}
}
