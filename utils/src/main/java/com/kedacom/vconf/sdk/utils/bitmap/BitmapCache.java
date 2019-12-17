package com.kedacom.vconf.sdk.utils.bitmap;

import android.graphics.Bitmap;
import android.util.LruCache;

import com.kedacom.vconf.sdk.utils.log.KLog;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.os.Environment.isExternalStorageRemovable;

/**
 * Created by Sissi on 2019/12/16
 */
public final class BitmapCache {



//    private DiskLruCache diskLruCache;
    private int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
    private int cacheSize = maxMemory / 8;
    private LruCache<String, Bitmap> memoryCache = new LruCache<String, Bitmap>(cacheSize) {
        @Override
        protected int sizeOf(String key, Bitmap bitmap) {
            // The cache size will be measured in kilobytes rather than
            // number of items.
            return bitmap.getByteCount() / 1024;
        }

        @Override
        protected Bitmap create(String key) {
            KLog.p("create key=%s", key);
            return null;
        }

        @Override
        protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
            KLog.p("entryRemoved evicted=%s, key=%s, oldValue=%s, newValue=%s", evicted, key, oldValue, newValue);
        }
    };

    private static final int maxBitmapSize = 1920*2*1080*2; // UHD/4K

//    private static DiskLruCache diskLruCache;
//    private static final int DISK_CACHE_SIZE = 1024 * 1024 * 10; // 10MB
//    private static final String DISK_CACHE_SUBDIR = "bmcache";

    private ExecutorService executor = Executors.newSingleThreadExecutor();

//    private static Application context;
//    public static void init(Application app){
//        context = app;
//
//        // Get max available VM memory, exceeding this amount will throw an
//        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
//        // int in its constructor.
//        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
//        // Use 1/8th of the available memory for this memory cache.
//        cacheSize = maxMemory/8;
//        KLog.p("cacheSize=%s", cacheSize);
//        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
//            @Override
//            protected int sizeOf(String key, Bitmap bitmap) {
//                // The cache size will be measured in kilobytes rather than
//                // number of items.
//                return bitmap.getByteCount() / 1024;
//            }
//        };
//
////        // Initialize disk cache on background thread
////        File cacheDir = getDiskCacheDir(app, DISK_CACHE_SUBDIR);
////        KLog.p("disk cache path=%s", cacheDir.getAbsolutePath());
////        executor.execute(new Runnable() {
////            @Override
////            public void run() {
////                try {
////                    diskLruCache = DiskLruCache.open(cacheDir, 100, 2, DISK_CACHE_SIZE);
////                } catch (IOException e) {
////                    KLog.p(KLog.ERROR, "DiskLruCache.open failed");
////                    e.printStackTrace();
////                }
////            }
////        });
//
//    }


    public void add(String key, Bitmap bitmap){
        if (get(key) != null) {
            return;
        }

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if (w*h > maxBitmapSize){
            bitmap = BitmapHelper.scale(bitmap, maxBitmapSize);
        }
        printStatus();
        memoryCache.put(key, bitmap);
        printStatus();
    }

    public Bitmap get(String key){
        printStatus();
        return memoryCache.get(key);
    }


//    // Creates a unique subdirectory of the designated app cache directory. Tries to use external
//    // but if not mounted, falls back on internal storage.
//    private static File getDiskCacheDir(Context context, String uniqueName) {
//        // Check if media is mounted or storage is built-in, if so, try and use external cache dir
//        // otherwise use internal cache dir
//        final String cachePath =
//                Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
//                        !isExternalStorageRemovable() ? context.getExternalCacheDir().getPath() :
//                        context.getCacheDir().getPath();
//
//        return new File(cachePath + File.separator + uniqueName);
//    }


    public void printStatus(){
        KLog.p("maxMemory=%s, totalMemory=%s, freeMemory=%s, cachesize=%s, cacheMaxSize=%s",
                Runtime.getRuntime().maxMemory() / 1024,
                Runtime.getRuntime().totalMemory() / 1024,
                Runtime.getRuntime().freeMemory() / 1024,
                memoryCache.size(), memoryCache.maxSize());
    }


}
