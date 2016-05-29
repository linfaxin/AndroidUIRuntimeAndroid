package org.androidui.runtime.image;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import org.androidui.runtime.BuildConfig;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashSet;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 为AndroidUI Runtime设计的图片加载类
 * 设计了三重缓存：图片缓存（Bitmap），字节缓存（Byte），本地缓存(文件)
 *
 * @author linlinfaxin@163.com
 */
public class ImageLoader {
    public static boolean DEBUG = BuildConfig.DEBUG;
    static private File cacheImgDir;//图片缓存到本地的目录
    static private final String DefaultCacheImgDirName = "ImageCache";//默认的图片缓存到本地的目录名
    static final private String ImgFileNameExtension = "";//本地缓存的图片的额外后缀名

    private static final int DefaultLoadNetDelay = 100;//100ms to load net image.
    private static final int MAX_Image_File_Size = 2 * 1024 * 1024;//最大的图片文件大小
    private static int MAX_Bitmap_Width = 1080;//最大的可解析图像宽（如果大于这个值会缩放到1~1/2）
    private static int MAX_Bitmap_Height = 1920;//最大的可解析图像高（如果大于这个值会缩放到1~1/2）

    /**
     * 内容图片缓存，用来缓存之前载入过的bitmap
     */
    private static TimeLruCache<String, Bitmap> bitmapList;
    /**
     * 内容字节缓存，用来缓存之前载入过的bitmap的字节
     */
    private static TimeLruCache<String, byte[]> imageBytesList;

    private static boolean init = false;

    public static void init(Context context) {
        if (context.getExternalCacheDir() == null) return;
        init(context, null);
    }

    /**
     * 初始化ImageLoader类的一些参数，必须在使用前调用
     *
     * @param context 上下文
     * @param imgDir  默认本地图片缓存目录，为空则不缓存到本地文件
     */
    public static void init(Context context, File imgDir) {
        if(init) return;
        init = true;
        //TODO 本地图片缓存使用DiskLru

        MAX_Bitmap_Width = context.getResources().getDisplayMetrics().widthPixels;
        MAX_Bitmap_Height = context.getResources().getDisplayMetrics().heightPixels;

        final int memClass = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
        final long memTotal = 1024l * 1024 * memClass;
        initMaxCache((int) memTotal / 6, (int) memTotal / 12);
        if (imgDir == null) {
            imgDir = new File(context.getExternalCacheDir(), DefaultCacheImgDirName);
        }
        ImageLoader.cacheImgDir = imgDir;
        try {
            if (!imgDir.exists() && !imgDir.mkdirs()) {
                if (DEBUG) Log.w(ImageLoader.class.getSimpleName(), "cacheImgDir mkdirs fail");
            }
            File noMediaFile = new File(imgDir, ".nomedia");
            if (!noMediaFile.exists() && !noMediaFile.createNewFile()) {
                if (DEBUG) Log.w(ImageLoader.class.getSimpleName(), ".nomedia create fail");
            }
        } catch (Exception ignore) {
        }
    }

    private static void initMaxCache(int bitmapSize, int byteSize) {
        if (DEBUG){
            Log.w(ImageLoader.class.getSimpleName(), "initMax bitmapSize:" + bitmapSize + ",byteSize:" + byteSize);
        }

        if (bitmapList != null) bitmapList.evictAll();
        bitmapList = new TimeLruCache<String, Bitmap>(bitmapSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                if (value == null || value.isRecycled()){
                    return 0;
                }
                if (Build.VERSION.SDK_INT >= 19){
                    return value.getAllocationByteCount();
                }
                return value.getRowBytes() * value.getHeight();
            }

            @Override
            protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
                super.entryRemoved(evicted, key, oldValue, newValue);
                if (DEBUG) Log.w(ImageLoader.class.getSimpleName(), "recycle bitmap:" + key);
                if (oldValue != null){
                    oldValue.recycle();
                }
            }
        };
        if (imageBytesList != null) imageBytesList.evictAll();
        imageBytesList = new TimeLruCache<String, byte[]>(byteSize) {
            @Override
            protected int sizeOf(String key, byte[] value) {
                if (value == null){
                    return 0;
                }
                return value.length;
            }
        };
    }

    public static void clearCache() {
        if (bitmapList != null) bitmapList.evictAll();
        if (imageBytesList != null) imageBytesList.evictAll();
        if (ImageLoader.cacheImgDir != null) {
            deleteFile(ImageLoader.cacheImgDir.listFiles());
        }
    }

    private static void deleteFile(File[] files) {
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                deleteFile(f.listFiles());
            }
            f.delete();
        }
    }

    /**
     * 回收这个url对应的相关资源
     */
    public static void recycleWithUrl(String url) {
        bitmapList.remove(url);
        imageBytesList.remove(url);
    }

    /**
     * 将url对应的条目在缓存中放在头部
     */
    public static void notifyUrlUse(String url){
        bitmapList.get(url);
        imageBytesList.get(url);
    }

    /**
     * 异步获取图片，会立即返回。从listen中监听，listen中的方法会在当前线程执行
     */
    @Nullable
    public static LoadImageTask getBitmapInBg(String url, ImageLoadingListener listen) {
        return getBitmapInBg(url, listen, DefaultLoadNetDelay, null);
    }

    /**
     * 异步获取图片（如果已缓存，则同步执行）。从listen中监听，listen中的方法会在当前线程执行
     */
    @Nullable
    private static LoadImageTask getBitmapInBg(String url, ImageLoadingListener listen, int loadNetDelay, final LoadChecker checker) {
        //尝试异步从缓存／网络载入
        final LoadImageTask task = new LoadImageTask(url, listen, loadNetDelay, checker);
        if (checker == null || checker.canLoad()){
            task.execute();
        }
        return task;
    }

    /**
     * 获取图片，会堵塞当前线程
     *
     * @param url 图片地址
     * @return 请求的图片（如果失败会返回loadingFailBitmap）
     */
    @Nullable
    public static Bitmap getBitmap(String url) {
        return getBitmap(url, null);
    }

    /**
     * 获取图片
     *
     * @param url  图片地址
     * @param task 读取bitmap的一个task，用作监听进度
     * @return bitmap
     */
    @Nullable
    private static Bitmap getBitmap(String url, LoadImageTask task) {
        if (TextUtils.isEmpty(url)) {
            return null;
        }
        try {
            Bitmap bitmap = getFromBitmapList(url, task);
            if (bitmap == null) return null;
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 从内存图片缓存中获取图片（如果获取失败则从内存字节缓存中获取）
     *
     * @param url  url 图片的地址（在内存缓存中作为key）
     * @param task 读取bitmap的一个task，用作监听进度
     * @return 图片的字节数组
     */
    @Nullable
    private static Bitmap getFromBitmapList(String url, LoadImageTask task) {
        Bitmap bitmap = bitmapList.get(url);
        if (bitmap == null || bitmap.isRecycled()) {//从内存图片缓存获取失败，则尝试从内存字节缓存获取
            byte[] img_bytes = getFromImageBytesList(url, task);
            if (img_bytes == null || img_bytes.length == 0) return null;
            bitmap = decodeSizeLimitBitmap(img_bytes);
            if (bitmap == null) {
                //解析失败，删除本地图片文件和内存缓存
                imageBytesList.remove(url);
                File imageFile = getImgFile(url);
                deleteFile(imageFile);

                //再次重试（从网络获取）
                img_bytes = getFromImageBytesList(url, task);
                if (img_bytes == null) return null;
                bitmap = decodeSizeLimitBitmap(img_bytes);
                if (bitmap == null) {
                    //解析失败，删除本地图片文件和内存缓存
                    imageBytesList.remove(url);
                    deleteFile(imageFile);
                    return null;
                }

            }else{
                bitmap = scaleToMiniBitmap(bitmap);
                bitmapList.put(url, bitmap, task==null ? System.currentTimeMillis() : task.createTime);
            }
        }
        return bitmap;
    }

    /**
     * 从内存字节缓存中获取图片（如果获取失败则从本地缓存(网络)中获取）
     *
     * @param url  图片的地址（在内存缓存中作为key）
     * @param task 读取bitmap的一个task，用作监听进度
     * @return 图片的字节数组
     */
    @Nullable
    private static byte[] getFromImageBytesList(String url, LoadImageTask task) {
        byte[] img_bytes = imageBytesList.get(url);//尝试从内存缓存imageBytesList中获取
        if (img_bytes == null) {//从imageBytesList获取失败则尝试从本地(网络)获取
            img_bytes = getImgBytesInDisk(url, task);
            if(img_bytes != null && img_bytes.length > 0){
                imageBytesList.put(url, img_bytes, task==null ? System.currentTimeMillis() : task.createTime);
            }
        }
        return img_bytes;
    }

    @Nullable
    public static File getImgFile(String urlStr) {
        if (TextUtils.isEmpty(urlStr)) {
            return null;
        }
        if (urlStr.startsWith("file://")) {
            return new File(URI.create(urlStr));
        }
        if (cacheImgDir == null) return null;
        String filename = convertToFileName(urlStr);
        return new File(cacheImgDir, filename);
    }

    /**
     * 从本地读取图片(如果本地不存在，则从网络获取)
     *
     * @param urlStr 图片的地址，会被转换成本地路径储存在imgDir目录里
     * @param task   读取bitmap的一个task，用作监听进度
     * @return 图片的字节数组
     */
    @Nullable
    private static byte[] getImgBytesInDisk(final String urlStr, final LoadImageTask task) {
        if (Thread.currentThread().isInterrupted()) return null;

        //尝试从本地文件获取图片
        final File imageFile = getImgFile(urlStr);

        synchronized (urlStr) {//避免对相同url的同时下载

            if (imageFile != null && imageFile.length() > 0) {
                try {
                    if (imageFile.length() > MAX_Image_File_Size) {//过大的图片文件载入
                        Bitmap limitBitmap = decodeSizeLimitBitmap(imageFile);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        limitBitmap.compress(CompressFormat.JPEG, 80, baos);
                        return baos.toByteArray();
                    }
                    return getBytesFromInputStream(new FileInputStream(imageFile));

                } catch (Exception e) {
                    e.printStackTrace();
                    deleteFile(imageFile);//删除错误（写入一半）的文件
                }
            }

            //没有从本地文件获取到，那么尝试从网络获取(等待延迟的载入)
            if(task != null){
                SystemClock.sleep(task.loadNetDelay);
            }
            if (Thread.currentThread().isInterrupted()){
                return null;
            }
            if (task != null && task.checker != null && !task.checker.canLoad()){
                return null;
            }

            try {
                imageFile.createNewFile();
            } catch (Exception ignored) {
                //没有地址，可能是没有初始化或者是储存卡不存在
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    ImageDownloader.reqForDownload(urlStr, baos);
                    return baos.toByteArray();
                } catch (Exception ignore) {
                }
                return null;
            }

            File dlFile = ImageDownloader.reqForDownload(urlStr, imageFile);

            if (dlFile == null) {
                if (DEBUG){
                    Log.d(ImageLoader.class.getSimpleName(), "dlFile is empty, download file error ：" + urlStr);
                }
                deleteFile(imageFile);
                return null;
            }
            try {
                FileInputStream fis = new FileInputStream(imageFile);
                return getBytesFromInputStream(fis);
            } catch (Exception e) {
                if (DEBUG) Log.d(ImageLoader.class.getSimpleName(), "read file error ：" + urlStr);
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * 从输入流读取图像数据
     *
     * @param is 输入流，文件输入流或网络输入流
     * @return 字节数组
     */
    private static byte[] getBytesFromInputStream(InputStream is) {
        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream();
            byte[] bytes = new byte[8 * 1024];
            int tempLength;
            while ((tempLength = is.read(bytes)) != -1) {
                if (Thread.currentThread().isInterrupted()) {
                    try {
                        is.close();
                        baos.close();
                    } catch (IOException ignore) {
                    }
                    return null;
                }
                baos.write(bytes, 0, tempLength);
            }
            byte[] imgBytes = baos.toByteArray();
            baos.close();
            is.close();
            return imgBytes;
        } catch (Exception e) {
            if (DEBUG) e.printStackTrace();
            try {
                is.close();
            } catch (IOException ignore) {
            }
            try {
                if (baos != null) baos.close();
            } catch (Exception ignore) {
            }
            return null;
        }
    }

    /**
     * 将url转化为文件名储存
     */
    private static String convertToFileName(String urlStr) {
        try {
            String path = URI.create(urlStr).getPath();
            return urlStr.hashCode() + "." + new File(path).getName() + ImgFileNameExtension;
        } catch (Exception e) {
            e.printStackTrace();
        }


        return urlStr.hashCode() + "." + ImgFileNameExtension;
    }

    private static synchronized Bitmap decodeSizeLimitBitmap(File imageFile) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imageFile.getPath(), options);
        int scale = Math.max(options.outWidth / MAX_Bitmap_Width, options.outHeight / MAX_Bitmap_Height);
        scale++;
        if (scale < 1) scale = 1;

        options = new BitmapFactory.Options();
        options.inSampleSize = scale;
        return BitmapFactory.decodeFile(imageFile.getPath(), options);
    }

    private static synchronized Bitmap decodeSizeLimitBitmap(byte[] img_bytes) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(img_bytes, 0, img_bytes.length, options);
        int scale = Math.max(options.outWidth / MAX_Bitmap_Width, options.outHeight / MAX_Bitmap_Height);
        scale++;
        if (scale < 1) scale = 1;

        options = new BitmapFactory.Options();
        options.inSampleSize = scale;
        return BitmapFactory.decodeByteArray(img_bytes, 0, img_bytes.length, options);
    }

    private static synchronized Bitmap scaleToMiniBitmap(Bitmap in) {
        return scaleToMiniBitmap(in, MAX_Bitmap_Width, MAX_Bitmap_Height);
    }

    //将图片控制在限定高宽之内
    public static Bitmap scaleToMiniBitmap(Bitmap in, int widthLimit, int heightLimit) {
        int inWidth = in.getWidth();
        int inHeight = in.getHeight();
        if (inWidth <= widthLimit && inHeight <= heightLimit) return in;
        float scale = Math.min(widthLimit * 1f / inWidth, heightLimit * 1f / inHeight);
        Bitmap re = Bitmap.createScaledBitmap(in, (int) (inWidth * scale), (int) (inHeight * scale), true);
        in.recycle();
        return re;
    }

    /**
     * 压缩Bitmap，返回压缩后的字节数组
     *
     * @param bitmap    目标Bitmap
     * @param quality   压缩质量
     * @param isRecycle 压缩完成后是否回收Bitmap
     * @return 压缩的字节
     */
    public static byte[] compressBitmap(Bitmap bitmap, int quality, boolean isRecycle) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(CompressFormat.JPEG, quality, baos);
        if (isRecycle) bitmap.recycle();
        try {
            baos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }

    private static void deleteFile(File imageFile) {
        if (imageFile == null) return;
        imageFile.delete();
    }

    /**
     * 图像载入回调接口
     * @author linfaxin
     */
    public interface ImageLoadingListener {
        void onBitmapLoadFinish(@Nullable Bitmap bitmap, String url);
    }

    /**
     * 载入的检查器（判断是否可以载入）
     */
    public interface LoadChecker {
        boolean canLoad();
    }

    /**
     * Task类
     * @author linfaxin
     */
    public static class LoadImageTask extends AsyncTask<Object, Object, Bitmap> {
        //后入先出队列
        private static LinkedBlockingDeque<Runnable> lifoQueue = new LinkedBlockingDeque<Runnable>() {
            @Override
            public boolean offer(Runnable runnable) {
                return super.offerFirst(runnable);
            }
        };
        //5-10个线程同时下载
        private static final Executor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(5, 10, 0L, TimeUnit.MILLISECONDS, lifoQueue);

        HashSet<ImageLoadingListener> listeners = new HashSet<ImageLoadingListener>();
        LoadChecker checker;
        String url;
        int loadNetDelay = 0;
        long createTime = System.currentTimeMillis();

        public LoadImageTask(String url, ImageLoadingListener imageLoadingListener) {
            this(url, imageLoadingListener, 0, null);
        }

        public LoadImageTask(String url, ImageLoadingListener imageLoadingListener, int loadNetDelay, LoadChecker checker) {
            super();
            this.url = url;
            if (imageLoadingListener != null) {
                listeners.add(imageLoadingListener);
            }
            this.loadNetDelay = loadNetDelay;
            this.checker = checker;
        }

        @Override
        protected Bitmap doInBackground(Object... params) {
            if (checker != null && !checker.canLoad()) return null;
            return getBitmap(url, this);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            if (checker != null && !checker.canLoad()) return;

            for (ImageLoadingListener listener : listeners) {
                if (listener != null) {
                    listener.onBitmapLoadFinish(bitmap, url);
                }
            }
        }

        @SuppressLint("NewApi")
        public LoadImageTask execute() {
            if (Build.VERSION.SDK_INT < 11) super.execute();
            else executeOnExecutor(THREAD_POOL_EXECUTOR);
            return this;
        }
    }

    /**
     * 图片下载
     */
    private static class ImageDownloader {
        private static final int DEFAULT_TIMEOUT = 8 * 1000;
        private static final int DEFAULT_SOCKET_BUFFER_SIZE = 8192;//8K

        public static File reqForDownload(String url, File file){
            try {
                file.getParentFile().mkdirs();
                FileOutputStream fos = new FileOutputStream(file);
                reqForDownload(url, fos);
                if(Thread.currentThread().isInterrupted()) return null;
                return file;

            } catch (Exception e) {
                if(BuildConfig.DEBUG) e.printStackTrace();
            }
            return null;
        }

        public static void reqForDownload(String url, OutputStream os) throws Exception{
            HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setConnectTimeout(DEFAULT_TIMEOUT);

            InputStream is = connection.getInputStream();
            byte[] temp=new byte[DEFAULT_SOCKET_BUFFER_SIZE];
            int length;

            try {
                while ((length = is.read(temp)) != -1) {
                    if(Thread.currentThread().isInterrupted()) break;
                    os.write(temp, 0, length);
                }
            } finally {
                os.close();
                is.close();
                connection.disconnect();
            }
        }
    }
}
