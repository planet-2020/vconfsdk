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
import com.kedacom.vconf.sdk.utils.view.ResolutionHelper;

import org.webrtc.CapturerObserver;
import org.webrtc.EglBase;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.TextureBufferImpl;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFileRenderer;
import org.webrtc.VideoFrame;
import org.webrtc.YuvConverter;

import java.io.File;
import java.io.IOException;

/**
 * 窗口采集。
 * 截取指定窗口的图像
 * */
class WindowCapturer implements VideoCapturer {

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
                int[] res = ResolutionHelper.adjust(window.getWidth(), window.getHeight(), 1280, 720, ResolutionHelper.SCALE_ASPECT_FILL);
                int w = res[0];
                int h = res[1];
                // 修正宽高为偶数
                w = w%2==0 ? w : w+1;
                h = h%2==0 ? h : h+1;
                float scaleX = (float)w/window.getWidth();
                float scaleY = scaleX;
                KLog.p("windowW=%s, windowH=%s, BitmapW=%s, BitmapH=%s, scaleX=%s, scaleY=%s",
                        window.getWidth(), window.getHeight(), w, h, scaleX, scaleY);
                TextureBufferImpl buffer = new TextureBufferImpl(w, h, VideoFrame.TextureBuffer.Type.RGB, textures[0], matrix, surTexture.getHandler(), yuvConverter, null);
                Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

                Canvas canvas = new Canvas(bitmap);
                Matrix matrix1 = new Matrix();
                matrix1.postScale(scaleX, scaleY);
                canvas.setMatrix(matrix1);
                Handler uiHandler = new Handler(Looper.getMainLooper());

                VideoFileRenderer videoFileRenderer = null;
                if (false){
                    // 保存码流（仅用于排查问题）
                    EglBase eglBase = EglBase.create();
                    File dir = new File(appContext.getExternalFilesDir(null), "webrtc");
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                    File savedVideo = new File(dir.getAbsolutePath()+"/"+"sendAss.dump");
                    try {
                        videoFileRenderer = new VideoFileRenderer(savedVideo.getAbsolutePath(), w, h, eglBase.getEglBaseContext());
                    } catch (IOException e) {
                        throw new RuntimeException(
                                "Failed to open video file for output: " + savedVideo, e);
                    }
                }

                while (true) {
                    uiHandler.post(() -> {
                        synchronized (lock) {
                            if (null != window) window.draw(canvas);
                        }
                    });

                    synchronized (lock) {
                        if (null != surTexture) {
                            surTexture.getHandler().post(() -> {
                                synchronized (lock) {
                                    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
                                    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
                                    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
                                }

                                VideoFrame.I420Buffer i420Buf = yuvConverter.convert(buffer);
                                long frameTime = System.nanoTime() - start;
                                VideoFrame videoFrame = new VideoFrame(i420Buf, 0, frameTime);
                                if (null != capturerObs) capturerObs.onFrameCaptured(videoFrame);
                                if (videoFileRenderer != null) {
                                    videoFileRenderer.onFrame(videoFrame);
                                }
                                videoFrame.release();
                            });
                        }
                    }

                    Thread.sleep(66); // 15fps

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
