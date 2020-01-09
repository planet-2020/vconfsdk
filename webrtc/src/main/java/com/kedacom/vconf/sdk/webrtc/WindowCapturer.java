package com.kedacom.vconf.sdk.webrtc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.NonNull;

import com.kedacom.vconf.sdk.utils.log.KLog;

import org.webrtc.CapturerObserver;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.TextureBufferImpl;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;
import org.webrtc.YuvConverter;

/**
 * 窗口采集。
 * 截取指定窗口的图像
 * */
public class WindowCapturer implements VideoCapturer {

    private SurfaceTextureHelper surTexture;
    private Context appContext;
    private org.webrtc.CapturerObserver capturerObs;
    private Thread captureThread;
    private View window;
    private final Object lock = new Object();


    public WindowCapturer(@NonNull View window) {
        this.window = window;
    }

    @Override
    public void initialize(SurfaceTextureHelper surfaceTextureHelper, Context applicationContext, CapturerObserver capturerObserver) {
        surTexture = surfaceTextureHelper;
        appContext = applicationContext;
        capturerObs = capturerObserver;
    }

    @Override
    public void startCapture(int width, int height, int fps) {
        captureThread = new Thread(() -> {
            try {
                if (null == surTexture || null == window){
                    KLog.p(KLog.ERROR, "null == surTexture || null == window");
                    return;
                }

                long start = System.nanoTime();
                if (null != capturerObs) capturerObs.onCapturerStarted(true);

                int[] textures = new int[1];
                GLES20.glGenTextures(1, textures, 0);

                YuvConverter yuvConverter = new YuvConverter();
                Matrix matrix = new Matrix();
                matrix.postScale(-1, 1);
                matrix.postRotate(180);
                TextureBufferImpl buffer = new TextureBufferImpl(window.getWidth(), window.getHeight(), VideoFrame.TextureBuffer.Type.RGB, textures[0], matrix, surTexture.getHandler(), yuvConverter, null);
                Bitmap bitmap = Bitmap.createBitmap(window.getWidth(), window.getHeight(), Bitmap.Config.ARGB_8888);

                Canvas canvas = new Canvas(bitmap);
                Handler uiHandler = new Handler(Looper.getMainLooper());
                while (true) {
                    uiHandler.post(() -> {
                        synchronized (lock) {
                            if (null != window) window.draw(canvas);  // TODO 判断是否前台，非前台不需要draw。
                            if (null != surTexture) {
                                surTexture.getHandler().post(() -> {
                                    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
                                    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
                                    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

                                    VideoFrame.I420Buffer i420Buf = yuvConverter.convert(buffer);

                                    long frameTime = System.nanoTime() - start;
                                    VideoFrame videoFrame = new VideoFrame(i420Buf, 0, frameTime);
                                    if (null != capturerObs) capturerObs.onFrameCaptured(videoFrame);
                                    videoFrame.release();
                                });
                            }
                        }
                    });

                    Thread.sleep(100);
                }
            } catch(InterruptedException ex) {
                ex.printStackTrace();
            }
        });
        captureThread.start();
    }

    @Override
    public void stopCapture() {
        captureThread.interrupt();
    }

    @Override
    public void changeCaptureFormat(int width, int height, int fps) {

    }

    @Override
    public void dispose() {
        synchronized (lock) {
            surTexture = null;
            appContext = null;
            capturerObs = null;
            window = null;
        }
    }

    @Override
    public boolean isScreencast() {
        return false;
    }
}
