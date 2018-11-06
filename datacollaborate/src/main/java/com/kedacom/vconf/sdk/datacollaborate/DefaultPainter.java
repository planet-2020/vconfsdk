package com.kedacom.vconf.sdk.datacollaborate;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.view.TextureView;
import android.view.View;

import com.kedacom.vconf.sdk.base.KLog;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCPaintInfo;

public class DefaultPainter implements IDCPainter {

    private TextureView textureView;
    Canvas canvas;
    private Paint paint;

    public DefaultPainter(Context context) {
        paint = new Paint();
        textureView = new TextureView(context);
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                KLog.p("surface=%s", surface);
//                drawLine(0, 0, 200, 300, new DCPaintInfo(3, 0x7F00FF00));
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                KLog.p("surface=%s", surface);
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                KLog.p("surface=%s", surface);
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                KLog.p("surface=%s", surface);
            }
        });
    }

    public View getWhiteBoardView(){
        return textureView;
    }


    @Override
    public void onWhiteBoard() {

    }

    @Override
    public void startDraw(){
        canvas = textureView.lockCanvas();
        if (null == canvas){
            KLog.p(KLog.ERROR, "lockCanvas failed");
        }
    }


    @Override
    public void finishDraw(){
        if (null != canvas){
            textureView.unlockCanvasAndPost(canvas);
            canvas = null;
        }
    }

    private Paint cfgPaint(DCPaintInfo paintInfo){
        paint.reset();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(paintInfo.strokeWidth);
        paint.setColor(paintInfo.color);
        return paint;
    }

    @Override
    public void drawLine(float startX, float startY, float stopX, float stopY, DCPaintInfo paintInfo) {
        Canvas cv = canvas;
        if (null == cv
                && null == (cv = textureView.lockCanvas())){
            return;
        }

        KLog.p("canvas=%s", cv);
        cv.drawLine(startX, startY, stopX, stopY, cfgPaint(paintInfo));

        if (null == canvas) {
            textureView.unlockCanvasAndPost(cv);
        }
    }

    @Override
    public void drawRect(float left, float top, float right, float bottom, DCPaintInfo paintInfo) {
        Canvas cv = canvas;
        if (null == cv
                && null == (cv = textureView.lockCanvas())){
            return;
        }

        KLog.p("canvas=%s", cv);
        cv.drawRect(left, top, right, bottom, cfgPaint(paintInfo));

        if (null == canvas) {
            textureView.unlockCanvasAndPost(cv);
        }
    }

    @Override
    public void drawOval(float left, float top, float right, float bottom, DCPaintInfo paintInfo) {
        Canvas cv = canvas;
        if (null == cv
                && null == (cv = textureView.lockCanvas())){
            return;
        }

        KLog.p("canvas=%s", cv);
        cv.drawOval(new RectF(left, top, right, bottom), cfgPaint(paintInfo));

        if (null == canvas) {
            textureView.unlockCanvasAndPost(cv);
        }
    }

    @Override
    public void drawPath(Path path, DCPaintInfo paintInfo) {
        Canvas cv = canvas;
        if (null == cv
                && null == (cv = textureView.lockCanvas())){
            return;
        }

        KLog.p("canvas=%s", cv);
        cv.drawPath(path, cfgPaint(paintInfo));

        if (null == canvas) {
            textureView.unlockCanvasAndPost(cv);
        }
    }

    @Override
    public void drawBitmap(Bitmap bitmap, Rect src, Rect dst, DCPaintInfo paintInfo) {
        Canvas cv = canvas;
        if (null == cv
                && null == (cv = textureView.lockCanvas())){
            return;
        }

        cv.drawBitmap(bitmap, src, dst, cfgPaint(paintInfo));

        if (null == canvas) {
            textureView.unlockCanvasAndPost(cv);
        }
    }

    @Override
    public void erase(float left, float top, float right, float bottom, DCPaintInfo paintInfo) {
        // 保存cv的初始状态？然后restore？
    }

}
