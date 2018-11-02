package com.kedacom.vconf.sdk.datacollaborate;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.view.TextureView;
import android.view.View;

import com.kedacom.vconf.sdk.datacollaborate.bean.DCPaintInfo;

public class DefaultPainter implements IDCPainter {

    private TextureView textureView;

    public DefaultPainter(Context context) {
        textureView = new TextureView(context);
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
    }

    public View getWhiteBoardView(){
        return textureView;
    }

    @Override
    public void drawLine(float startX, float startY, float stopX, float stopY, DCPaintInfo paintInfo) {
        Canvas canvas = textureView.lockCanvas();
        if (null == canvas){
            return;
        }
        Paint paint = new Paint();
        paint.setStrokeWidth(paintInfo.strokeWidth);
        paint.setColor(paintInfo.color);
        canvas.drawLine(startX, startY, stopX, stopY, paint);
        textureView.unlockCanvasAndPost(canvas);
    }

    @Override
    public void drawRect(float left, float top, float right, float bottom, DCPaintInfo paintInfo) {

    }

    @Override
    public void drawOval(float left, float top, float right, float bottom, DCPaintInfo paintInfo) {

    }

    @Override
    public void drawPath(Path path, DCPaintInfo paintInfo) {

    }

    @Override
    public void drawBitmap(Bitmap bitmap, Rect src, Rect dst, DCPaintInfo paintInfo) {

    }

    @Override
    public void erase(float left, float top, float right, float bottom, DCPaintInfo paintInfo) {

    }
}
