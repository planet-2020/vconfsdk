package com.kedacom.vconf.sdk.utils.bitmap;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.Looper;

import com.kedacom.vconf.sdk.utils.log.KLog;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Sissi on 2019/12/16
 */
public final class BitmapHelper {

    private static ExecutorService executor = Executors.newSingleThreadExecutor();
    private static Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void decode(String bitmapFilePath, Bitmap.Config config, int outputSizeLimit, IResultListener resultListener){
        executor.execute(() -> {
            printMemUsage();
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = config;
            Bitmap bitmap = BitmapFactory.decodeFile(bitmapFilePath, options);
            int origW = bitmap.getWidth();
            int origH = bitmap.getHeight();
            KLog.p("bitmap %s, origW=%s, origH=%s, origSize=%s, limit=%s", bitmapFilePath, origW, origH, origW*origH, outputSizeLimit);
            if (outputSizeLimit>0 && origW * origH > outputSizeLimit){
                bitmap = scale(bitmap, outputSizeLimit);
            }
            Bitmap finalBitmap = bitmap;
            mainHandler.post(() -> resultListener.onResult(finalBitmap));
            printMemUsage();
        });

    }

    public static void decode(String bitmapFilePath, int outputWidth, int outputHeight, Bitmap.Config config, IResultListener resultListener){
        executor.execute(() -> {
            printMemUsage();
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = config;
            Bitmap bitmap = BitmapFactory.decodeFile(bitmapFilePath, options);
            KLog.p("bitmap %s", bitmapFilePath);
            Bitmap scaledBitmap = scale(bitmap, outputWidth, outputHeight);
            mainHandler.post(() -> resultListener.onResult(scaledBitmap));
            printMemUsage();
        });
    }


    public static Bitmap decode(String bitmapFilePath, int outputSizeLimit, Bitmap.Config config){
        printMemUsage();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = config;
        Bitmap bitmap = BitmapFactory.decodeFile(bitmapFilePath, options);
        int origW = bitmap.getWidth();
        int origH = bitmap.getHeight();
        KLog.p("bitmap %s, origW=%s, origH=%s, origSize=%s, limit=%s", bitmapFilePath, origW, origH, origW*origH, outputSizeLimit);
        if (outputSizeLimit>0 && origW * origH > outputSizeLimit){
            bitmap = scale(bitmap, outputSizeLimit);
        }
        printMemUsage();
        return bitmap;

    }

    public static Bitmap decode(String bitmapFilePath, int outputWidth, int outputHeight, Bitmap.Config config){
        printMemUsage();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = config;
        Bitmap bitmap = BitmapFactory.decodeFile(bitmapFilePath, options);
        KLog.p("bitmap %s", bitmapFilePath);
        Bitmap scaledBitmap = scale(bitmap, outputWidth, outputHeight);
        printMemUsage();
        return scaledBitmap;
    }

    public static Bitmap scale(Bitmap bitmap, int targetResolution){
        int origW = bitmap.getWidth();
        int origH = bitmap.getHeight();
        KLog.p("bitmap origW=%s, origH=%s, targetResolution=%s", origW, origH, targetResolution);
        if (origW*origH == targetResolution){
            return bitmap;
        }
        float scaleFactor = (float) Math.sqrt(targetResolution/ (double)(origW * origH));
        Matrix matrix = new Matrix();
        matrix.postScale(scaleFactor, scaleFactor);
        return Bitmap.createBitmap(bitmap, 0, 0, origW, origH, matrix, false);
    }

    public static Bitmap scale(Bitmap bitmap, int targetWidth, int targetHeight){
        int origW = bitmap.getWidth();
        int origH = bitmap.getHeight();
        KLog.p("bitmap origW=%s, origH=%s, outputWidth=%s, outputHeight=%s", origW, origH, targetWidth, targetHeight);
        if (origW == targetWidth && origH == targetHeight){
            return bitmap;
        }
        Matrix matrix = new Matrix();
        matrix.postScale(targetWidth/(float)origW, targetHeight/(float)origH);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, origW, origH, matrix, false);
        return bitmap;
    }

    public interface IResultListener{
        void onResult(Bitmap bitmap);
    }

    private static void printMemUsage(){
        KLog.p("maxMemory=%s, totalMemory=%s, freeMemory=%s",
                Runtime.getRuntime().maxMemory() / 1024,
                Runtime.getRuntime().totalMemory() / 1024,
                Runtime.getRuntime().freeMemory() / 1024);
    }

}
