package org.androidui.runtime;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Base64;
import android.util.DisplayMetrics;

import com.nostra13.universalimageloader.cache.disc.impl.ext.LruDiskCache;
import com.nostra13.universalimageloader.cache.disc.naming.HashCodeFileNameGenerator;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by linfaxin on 15/12/15.
 *
 */
public class ImageApi {
    private static boolean DEBUG = false;
    private static final String ASSETS_PRE = "file:///android_asset/";

    private static final ThreadPoolExecutor LoaderPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);

    static void initImageLoader(Context context, boolean d){
        if(ImageLoader.getInstance().isInited()) return;
        DEBUG = d;
        File imgDir = new File(context.getExternalCacheDir(), "AndroidUIRuntime/.img/");
        imgDir.mkdirs();

        DisplayMetrics dm = context.getResources().getDisplayMetrics();

        LruDiskCache diskCache = null;
        try {
            diskCache = new LruDiskCache(imgDir, new HashCodeFileNameGenerator(), 30 * 1024 * 1024);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ImageLoaderConfiguration.Builder builder = new ImageLoaderConfiguration.Builder(context)
                .memoryCacheExtraOptions(dm.widthPixels, dm.heightPixels) // default = device screen dimensions
                .tasksProcessingOrder(QueueProcessingType.LIFO) // LIFO
                .diskCache(diskCache)
                ;

        DisplayImageOptions defaultImageOptions = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .build();

        builder.defaultDisplayImageOptions(defaultImageOptions);

        if(DEBUG) builder.writeDebugLogs();
        com.nostra13.universalimageloader.utils.L.writeLogs(DEBUG);
        ImageLoader.getInstance().init(builder.build()); // Do it on Application start
    }

    private Bitmap bitmap;
    private String loadingUrl;
    private Runnable loadingTask;
    private RuntimeBridge bridge;

    public ImageApi(RuntimeBridge bridge) {
        this.bridge = bridge;
    }

    public void loadImage(String url){
        if(TextUtils.equals(url, loadingUrl)) return;
        if(url.startsWith(ASSETS_PRE)) url = "assets://" + url.substring(ASSETS_PRE.length());
        loadingUrl = url;
        loadImageImpl();
    }

    private void loadImageImpl(){
        if(loadingTask!=null) LoaderPool.remove(loadingTask);

        final String taskUrl = loadingUrl;

        if(taskUrl.startsWith("data:image/png;base64,")){//async if base64 png
            String base64 = taskUrl.substring("data:image/png;base64,".length());
            byte[] bitmapArray;
            bitmapArray = Base64.decode(base64, Base64.DEFAULT);
            bitmap = BitmapFactory.decodeByteArray(bitmapArray, 0, bitmapArray.length);
            if(bitmap!=null){
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                int[] leftBorder = new int[height-2];
                int[] topBorder = new int[width-2];
                int[] rightBorder = new int[height-2];
                int[] bottomBorder = new int[width-2];
                bitmap.getPixels(leftBorder, 0, 1, 0, 1, 1, height-1);
                bitmap.getPixels(topBorder, 0, width-2, 1, 0, width-1, 1);
                bitmap.getPixels(rightBorder, 0, 1, width-1, 1, width, height-1);
                bitmap.getPixels(bottomBorder, 0, width-2, 1, height-1, width-1, height);
                bridge.notifyImageLoadFinish(ImageApi.this, bitmap.getWidth(), bitmap.getHeight(),
                        leftBorder, topBorder, rightBorder, bottomBorder);
            }else{
                bridge.notifyImageLoadError(ImageApi.this);
            }
            return;
        }

        loadingTask = new Runnable() {
            @Override
            public void run() {
                if(!taskUrl.endsWith(loadingUrl)) return;

                if(taskUrl.startsWith("data:image/jpg;base64,")){
                    String base64 = taskUrl.substring("data:image/jpg;base64,".length());
                    byte[] bitmapArray;
                    bitmapArray = Base64.decode(base64, Base64.DEFAULT);
                    bitmap = BitmapFactory.decodeByteArray(bitmapArray, 0, bitmapArray.length);

                }else {
                    bitmap = ImageLoader.getInstance().loadImageSync(taskUrl);
                    for (int i = 0; i <= 2; i++) {
                        if (bitmap == null) {
                            SystemClock.sleep(i * 50);//0, 50, 100
                            bitmap = ImageLoader.getInstance().loadImageSync(taskUrl);
                        } else break;
                    }
                }

                if(bitmap!=null){
                    bridge.notifyImageLoadFinish(ImageApi.this, bitmap.getWidth(), bitmap.getHeight());
                }else{
                    bridge.notifyImageLoadError(ImageApi.this);
                }
                if(loadingTask==this) loadingTask = null;
            }
        };

        LoaderPool.execute(loadingTask);
    }

    public void recycle(){
        if(bitmap!=null) bitmap.recycle();

    }

    public Bitmap getBitmap() {
        if(bitmap!=null && bitmap.isRecycled()){
            loadImageImpl();
            return null;
        }
        return bitmap;
    }
}
