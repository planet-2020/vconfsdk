package com.kedacom.vconf.sdk.datacollaborate;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;

import com.kedacom.vconf.sdk.base.KLog;
import com.kedacom.vconf.sdk.datacollaborate.bean.DCPaintInfo;

public class DefaultPainter implements IDCPainter {


    private SurfaceView surfaceView;
    private TextureView textureView;
    private Canvas canvas;
    private Paint paint;

    private HandlerThread handlerThread;
    private Handler handler;

    public DefaultPainter(Context context) {

        handlerThread = new HandlerThread("DC.OpThr", Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        paint = new Paint();
        surfaceView = new SurfaceView(context);
        textureView = new TextureView(context);
        textureView.setOpaque(false);
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                KLog.p("surface=%s", surface);
                drawColor();
//                drawLine(0, 0, 10, 80, new DCPaintInfo(10, 0x7FFF0000));
//                drawLine(0, 0, 80, 10, new DCPaintInfo(10, 0x7FFF0000));
//                drawLine(0, 0, 80, 78, new DCPaintInfo(10, 0x7FFF0000));
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
//        return surfaceView;
    }


    @Override
    public void onWhiteBoard() {

    }

    @Override
    public void startBatchDraw(){
        handler.post(()->{
            canvas = textureView.lockCanvas();
//            canvas = surfaceView.getHolder().lockCanvas();
            if (null == canvas){
                KLog.p(KLog.ERROR, "lockCanvas failed");
            }
        });

    }


    @Override
    public void finishBatchDraw(){
        handler.post(()-> {
            if (null != canvas) {
                textureView.unlockCanvasAndPost(canvas);
//                surfaceView.getHolder().unlockCanvasAndPost(canvas);
                canvas = null;
            }
        });
    }

    private final PorterDuffXfermode DUFFMODE_SRCOVER = new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER);
    private final PorterDuffXfermode DUFFMODE_DSTOVER = new PorterDuffXfermode(PorterDuff.Mode.DST_OVER);
    private Paint cfgPaint(DCPaintInfo paintInfo){
        paint.reset();
//        Paint paint = new Paint();
//        paint.setXfermode(DUFFMODE_DSTOVER);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(paintInfo.strokeWidth);
        paint.setColor(paintInfo.color);
        return paint;
    }

    @Override
    public void drawLine(float startX, float startY, float stopX, float stopY, DCPaintInfo paintInfo) {
        handler.post(()->{
            Canvas cv = null==canvas ? textureView.lockCanvas() : canvas;
//            Canvas cv = null==canvas ? surfaceView.getHolder().lockCanvas() : canvas;
            if (null == cv){
                KLog.p(KLog.ERROR,"lockCanvas failed");
                return;
            }
            KLog.p("line{(%s,%s),(%s,%s)}", startX, startY, stopX, stopY);
            cv.drawLine(startX, startY, stopX, stopY, cfgPaint(paintInfo));
            if (null == canvas) {
                textureView.unlockCanvasAndPost(cv);
//                surfaceView.getHolder().unlockCanvasAndPost(cv);
            }
        });
    }

    @Override
    public void drawRect(float left, float top, float right, float bottom, DCPaintInfo paintInfo) {
        handler.post(()->{
            Canvas cv = null==canvas ? textureView.lockCanvas() : canvas;
//            Canvas cv = null==canvas ? surfaceView.getHolder().lockCanvas() : canvas;
            if (null == cv){
                KLog.p(KLog.ERROR,"lockCanvas failed");
                return;
            }
            KLog.p("rect{%s,%s,%s,%s}", left, top, right, bottom);
            cv.drawRect(left, top, right, bottom, cfgPaint(paintInfo));
            if (null == canvas) {
                textureView.unlockCanvasAndPost(cv);
//                surfaceView.getHolder().unlockCanvasAndPost(cv);
            }
        });

    }

    @Override
    public void drawOval(float left, float top, float right, float bottom, DCPaintInfo paintInfo) {
        handler.post(()->{
            Canvas cv = null==canvas ? textureView.lockCanvas() : canvas;
//            Canvas cv = null==canvas ? surfaceView.getHolder().lockCanvas() : canvas;
            if (null == cv){
                KLog.p(KLog.ERROR,"lockCanvas failed");
                return;
            }
            KLog.p("oval{%s,%s,%s,%s}", left, top, right, bottom);
            cv.drawOval(new RectF(left, top, right, bottom), cfgPaint(paintInfo));
            if (null == canvas) {
                textureView.unlockCanvasAndPost(cv);
//                surfaceView.getHolder().unlockCanvasAndPost(cv);
            }
        });

    }

    @Override
    public void drawPath(Path path, DCPaintInfo paintInfo) {
        handler.post(()->{
            Canvas cv = null==canvas ? textureView.lockCanvas() : canvas;
//            Canvas cv = null==canvas ? surfaceView.getHolder().lockCanvas() : canvas;
            if (null == cv){
                KLog.p(KLog.ERROR,"lockCanvas failed");
                return;
            }
            KLog.p("path{%s}", path);
            cv.drawPath(path, cfgPaint(paintInfo));
            if (null == canvas) {
                textureView.unlockCanvasAndPost(cv);
//                surfaceView.getHolder().unlockCanvasAndPost(cv);
            }
        });
    }

    @Override
    public void drawBitmap(Bitmap bitmap, Rect src, Rect dst, DCPaintInfo paintInfo) {
        handler.post(()->{
            Canvas cv = null==canvas ? textureView.lockCanvas() : canvas;
//            Canvas cv = null==canvas ? surfaceView.getHolder().lockCanvas() : canvas;
            if (null == cv){
                KLog.p(KLog.ERROR,"lockCanvas failed");
                return;
            }
            KLog.p("bitmap{%s}", bitmap);
            cv.drawBitmap(bitmap, src, dst, cfgPaint(paintInfo));
            if (null == canvas) {
                textureView.unlockCanvasAndPost(cv);
//                surfaceView.getHolder().unlockCanvasAndPost(cv);
            }
        });
    }

    public void drawColor(){
        handler.post(()->{
            Canvas cv = null==canvas ? textureView.lockCanvas() : canvas;
//            Canvas cv = null==canvas ? surfaceView.getHolder().lockCanvas() : canvas;
            if (null == cv){
                KLog.p(KLog.ERROR,"lockCanvas failed");
                return;
            }
            KLog.p("color{%s}", Color.LTGRAY);
            cv.drawColor(Color.LTGRAY);
            if (null == canvas) {
                textureView.unlockCanvasAndPost(cv);
//                surfaceView.getHolder().unlockCanvasAndPost(cv);
            }
        });
    }

    @Override
    public void erase(float left, float top, float right, float bottom, DCPaintInfo paintInfo) {
        // 保存cv的初始状态？然后restore？
    }


    

}
