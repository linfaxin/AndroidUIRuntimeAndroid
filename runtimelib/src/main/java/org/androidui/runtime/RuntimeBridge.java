package org.androidui.runtime;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.Arrays;

/**
 * Created by linfaxin on 15/12/15.
 * Js Bridge
 */
public class RuntimeBridge {
    protected final static String TAG = "RuntimeBridge";
    protected final static boolean DEBUG = false;
    public static boolean DEBUG_TRACK_FPS = false;

    protected SparseArray<SurfaceApi> surfaceInstances = new SparseArray<>();
    private SparseArray<CanvasApi> canvasInstances = new SparseArray<>();
    SparseArray<ImageApi> imageInstances = new SparseArray<>();


    private WeakReference<WebView> webViewRef;

    protected RuntimeBridge(WebView webView) {
        this.webViewRef = new WeakReference<>(webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(this, "AndroidUIRuntime");
    }

    @Nullable
    protected WebView getWebView(){
        return this.webViewRef.get();
    }

    protected SurfaceApi createSurfaceApi(Context context, int surfaceId){
        return new SurfaceApi(context, RuntimeBridge.this);
    }

    protected CanvasApi createCanvasApi(int width, int height){
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new CanvasApi.BitmapCanvas(bitmap);
        return new CanvasApi(canvas);
    }

    private boolean applyTextMeasure = false;
    void applyTextMeasure(){
        if(applyTextMeasure) return;
        applyTextMeasure = true;
        RuntimeInit.initMeasureWidthData();
        execJSOnUI(RuntimeInit.MeasureWidthsJS);
    }

    void notifySurfaceReady(SurfaceApi surfaceApi){
        int surfaceId = surfaceInstances.keyAt(surfaceInstances.indexOfValue(surfaceApi));
        execJSOnUI(String.format("androidui.native.NativeSurface.notifySurfaceReady(%d);", surfaceId));
    }

    void notifySurfaceSupportDirtyDraw(int surfaceId, boolean support){
        execJSOnUI(String.format("androidui.native.NativeSurface.notifySurfaceSupportDirtyDraw(%d, %b);", surfaceId, support));
    }

    void notifyImageLoadFinish(ImageApi imageApi, int width, int height){
        int imageId = imageInstances.keyAt(imageInstances.indexOfValue(imageApi));
        execJSOnUI(String.format("androidui.native.NativeImage.notifyLoadFinish(%d, %d, %d);", imageId, width, height));
    }
    void notifyImageLoadError(ImageApi imageApi){
        int imageId = imageInstances.keyAt(imageInstances.indexOfValue(imageApi));
        execJSOnUI(String.format("androidui.native.NativeImage.notifyLoadError(%d);", imageId));
    }
    void notifyImageGetPixels(int imageId, int callBackIndex, int[] data){
        execJSOnUI(String.format("androidui.native.NativeImage.notifyGetPixels(%d, %d, %s);", imageId, callBackIndex, Arrays.toString(data)));
    }

    void notifyCanvasCacheEnable(boolean enable){
        execJSOnUI(String.format("androidui.native.NativeCanvas.notifyCanvasCacheEnable(%b);", enable));
    }

    protected void execJSOnUI(final String js){
        if(DEBUG) Log.d(TAG, "execJS:"+js.substring(0, Math.min(js.length(), 200)));
        if(Looper.myLooper() == Looper.getMainLooper()){
            execJS(js);
        }else {
            View webView = getWebView();
            if(webView!=null){
                webView.post(new Runnable() {
                    @Override
                    public void run() {
                        execJS(js);
                    }
                });
            }
        }
    }

    protected void execJS(String js){
        WebView webView = getWebView();
        if(webView!=null && js!=null) {
            if(js.startsWith("javascript:")) js = js.substring("javascript:".length());

            if(Build.VERSION.SDK_INT>=19){
                webView.evaluateJavascript(js, null);
            }else{
                webView.loadUrl("javascript:" + js);
            }
        }
    }

    private long mFpsStartTime = -1;
    private long mFpsPrevTime = -1;
    private int mFpsNumFrames = 0;
    private TextView mFPSShowText;
    protected void trackFPS(){
        long nowTime = System.currentTimeMillis();
        if (this.mFpsStartTime < 0) {
            this.mFpsStartTime = this.mFpsPrevTime = nowTime;
            this.mFpsNumFrames = 0;
        } else {
            this.mFpsNumFrames++;
            long frameTime = nowTime - this.mFpsPrevTime;
            long totalTime = nowTime - this.mFpsStartTime;
            //Log.v(ViewRootImpl.TAG, "Frame time:\t" + frameTime);
            this.mFpsPrevTime = nowTime;
            if (totalTime > 1000) {
                long fps = this.mFpsNumFrames * 1000 / totalTime;
                Log.d("trachFPS", "FPS:\t" + fps);
                WebView webView = getWebView();
                if(mFPSShowText==null && webView!=null){
                    Context context = webView.getContext();
                    mFPSShowText = new TextView(context);
                    mFPSShowText.setBackgroundColor(Color.BLACK);
                    mFPSShowText.setTextColor(Color.WHITE);
                    mFPSShowText.setAlpha(0.7f);
                    ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams((int) (48 * context.getResources().getDisplayMetrics().density), -2);
                    webView.addView(mFPSShowText, layoutParams);
                }
                if(mFPSShowText!=null){
                    mFPSShowText.setText("FPS:"+(int)fps);
                }

                this.mFpsStartTime = nowTime;
                this.mFpsNumFrames = 0;
            }
        }
    }

    @JavascriptInterface
    public void batchCall(final String batchString){
        final View webView = getWebView();
        if(webView!=null){
            ViewCompat.postOnAnimation(webView, new Runnable() {
                @Override
                public void run() {
                    BatchCallHelper.parseAndRun(RuntimeBridge.this, batchString);
                    if(DEBUG_TRACK_FPS) trackFPS();
                }
            });
        }
    }

    //================= surface api =================
    @JavascriptInterface
    @BatchCallHelper.BatchMethod(value = "10", batchCantSkip = true)
    public void createSurface(final int surfaceId, final float left, final float top, final float right, final float bottom){
        if(DEBUG) Log.d(TAG, "createSurface, surfaceId:" + surfaceId + ", left:" + left + ", top:" + top + ", right:" + right + ", bottom:" + bottom);
        applyTextMeasure();
        final ViewGroup webView = getWebView();
        if(webView!=null) {
            //do on ui thread
            webView.post(new Runnable() {
                @Override
                public void run() {
                    int width = (int) (right - left);
                    int height = (int) (bottom - top);
                    if (width < 0 || right < 0) width = -1;
                    if (height < 0 || bottom < 0) height = -1;
                    final ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(width, height);
                    params.leftMargin = (int) left;
                    params.topMargin = (int) top;

                    SurfaceApi surfaceApi = createSurfaceApi(webView.getContext(), surfaceId);
                    webView.addView(surfaceApi.getSurfaceView(), params);

                    SurfaceApi oldApi = surfaceInstances.get(surfaceId);
                    if (oldApi != null) {
                        Log.e(TAG, "Create surface warn: there has a old surfaceId instance. Override it.");
                        View oldView = oldApi.getSurfaceView();
                        if (oldView != null) webView.removeView(oldView);
                    }

                    surfaceInstances.put(surfaceId, surfaceApi);
                }
            });
        }
    }


    @JavascriptInterface
    @BatchCallHelper.BatchMethod("11")
    public void onSurfaceBoundChange(int surfaceId, float left, float top, float right, float bottom){
        if(DEBUG) Log.d(TAG, "onSurfaceBoundChange, surfaceId:" + surfaceId + ", left:" + left + ", top:" + top + ", right:" + right + ", bottom:" + bottom);
        SurfaceApi surfaceApi = surfaceInstances.get(surfaceId);
        final View surfaceView = surfaceApi.getSurfaceView();
        if(surfaceView!=null){
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) surfaceView.getLayoutParams();
            int width = (int) (right-left);
            int height = (int) (bottom - top);
            if(width<0 || right<0) width = -1;
            if(height<0 || bottom<0) height = -1;
            params.width = width;
            params.height = height;
            params.leftMargin = (int) left;
            params.topMargin = (int) top;

            surfaceView.post(new Runnable() {
                @Override
                public void run() {
                    surfaceView.requestLayout();
                }
            });
        }
    }

    //================= canvas api =================
    @JavascriptInterface
    @BatchCallHelper.BatchMethod("31")
    public void lockCanvas(final int surfaceId, final int canvasId, final float left, final float top, final float right, final float bottom){
        if(DEBUG) Log.d(TAG, "lockCanvas, surfaceId:" + surfaceId + ", canvasId:" + canvasId + ", left:" + left + ", top:" + top + ", right:" + right + ", bottom:" + bottom);

        SurfaceApi surfaceApi = surfaceInstances.get(surfaceId);
        CanvasApi canvasApi = surfaceApi.lockCanvas(left, top, right, bottom);
        canvasInstances.put(canvasId, canvasApi);
    }

    @JavascriptInterface
    @BatchCallHelper.BatchMethod("32")
    public void unlockCanvasAndPost(final int surfaceId, final int canvasId){
        if(DEBUG) Log.d(TAG, "unlockCanvasAndPost, surfaceId:" + surfaceId + ", canvasId:" + canvasId);
        SurfaceApi surfaceApi = surfaceInstances.get(surfaceId);
        CanvasApi canvasApi = canvasInstances.get(canvasId);
        surfaceApi.unlockCanvasAndPost(canvasApi);

        //recycle canvas
        CanvasApi oldCanvasApi = canvasInstances.get(canvasId);
        if(oldCanvasApi!=null) oldCanvasApi.recycle();
        else{
            Log.e(TAG, "unlockCanvasAndPost recycle canvas warn: no canvas exist, id: " + canvasId);
        }
        canvasInstances.remove(canvasId);
    }

    //canvas api
    @JavascriptInterface
    @BatchCallHelper.BatchMethod(value = "33", batchCantSkip = true)
    public void createCanvas(final int canvasId, final float width, final float height){
        if(DEBUG) Log.d(TAG, "createCanvas, canvasId:" + canvasId + ", width:" + width + ", height:" + height);
        CanvasApi newCanvasApi = createCanvasApi((int)width, (int)height);
        CanvasApi oldCanvasApi = canvasInstances.get(canvasId);
        if(oldCanvasApi!=null){
            Log.e(TAG, "Create canvas warn: there has a old canvasId instance. Override it.");
            oldCanvasApi.recycle();
        }
        canvasInstances.put(canvasId, newCanvasApi);
    }
    @JavascriptInterface
    @BatchCallHelper.BatchMethod("34")
    public void recycleCanvas(final int canvasId){
        if(DEBUG) Log.d(TAG, "recycleCanvas, canvasId:" + canvasId);
        CanvasApi oldCanvasApi = canvasInstances.get(canvasId);
        if(oldCanvasApi!=null) oldCanvasApi.recycle();
        else{
            Log.e(TAG, "recycle canvas warn: no canvas exist, id: " + canvasId);
        }
        canvasInstances.remove(canvasId);
    }
    @JavascriptInterface
    @BatchCallHelper.BatchMethod("35")
    public void translate(int canvasId, float tx, float ty){
        if(DEBUG) Log.d(TAG, "translate, canvasId:" + canvasId + ", tx:" + tx + ", ty:" + ty);
        CanvasApi canvasApi = canvasInstances.get(canvasId);
        canvasApi.translate(tx, ty);
    }
    @JavascriptInterface
    @BatchCallHelper.BatchMethod("36")
    public void scale(int canvasId, float sx, float sy){
        if(DEBUG) Log.d(TAG, "scale, canvasId:" + canvasId + ", sx:" + sx + ", sy:" + sy);
        CanvasApi canvasApi = canvasInstances.get(canvasId);
        canvasApi.scale(sx, sy);
    }
    @JavascriptInterface
    @BatchCallHelper.BatchMethod("37")
    public void rotate(int canvasId, float degrees){
        if(DEBUG) Log.d(TAG, "rotate, canvasId:" + canvasId + ", degrees:" + degrees);
        CanvasApi canvasApi = canvasInstances.get(canvasId);
        canvasApi.rotate(degrees);
    }
    @JavascriptInterface
    @BatchCallHelper.BatchMethod("38")
    public void concat(int canvasId, float MSCALE_X, float MSKEW_X, float MTRANS_X, float MSKEW_Y, float MSCALE_Y, float MTRANS_Y){
        if(DEBUG) Log.d(TAG, "concat, canvasId:" + canvasId + ", MSCALE_X:" + MSCALE_X + ", MSKEW_X:" + MSKEW_X
                + ", MTRANS_X:" + MTRANS_X + ", MSKEW_Y:" + MSKEW_Y + ", MSCALE_Y:" + MSCALE_Y + ", MTRANS_Y:" + MTRANS_Y);
        CanvasApi canvasApi = canvasInstances.get(canvasId);
        canvasApi.concat(MSCALE_X, MSKEW_X, MTRANS_X, MSKEW_Y, MSCALE_Y, MTRANS_Y);
    }
    @JavascriptInterface
    @BatchCallHelper.BatchMethod("39")
    public void drawColor(int canvasId, long color){
        if(DEBUG) Log.d(TAG, "drawColor, canvasId:" + canvasId + ", color:" + Integer.toHexString((int) color));
        CanvasApi canvasApi = canvasInstances.get(canvasId);
        canvasApi.drawColor((int) color);
    }
    @JavascriptInterface
    @BatchCallHelper.BatchMethod("40")
    public void clearColor(int canvasId){
        if(DEBUG) Log.d(TAG, "clearColor, canvasId:" + canvasId);
        CanvasApi canvasApi = canvasInstances.get(canvasId);
        canvasApi.clearColor();
    }
    @JavascriptInterface
    @BatchCallHelper.BatchMethod("41")
    public void drawRect(int canvasId, float left, float top, float width, float height, int fillStyle){
        if(DEBUG) Log.d(TAG, "drawRect, canvasId:" + canvasId + ", left:" + left + ", top:" + top + ", width:" + width + ", height:" + height + ", fillStyle:" + fillStyle);
        CanvasApi canvasApi = canvasInstances.get(canvasId);
        canvasApi.drawRect(left, top, width, height, fillStyle);
    }
    @JavascriptInterface
    @BatchCallHelper.BatchMethod("42")
    public void clipRect(int canvasId, float left, float top, float width, float height){
        if(DEBUG) Log.d(TAG, "clipRect, canvasId:" + canvasId + ", left:" + left + ", top:" + top + ", width:" + width + ", height:" + height);
        CanvasApi canvasApi = canvasInstances.get(canvasId);
        canvasApi.clipRect(left, top, width, height);
    }
    @JavascriptInterface
    @BatchCallHelper.BatchMethod("43")
    public void save(int canvasId){
        if(DEBUG) Log.d(TAG, "save, canvasId:" + canvasId);
        CanvasApi canvasApi = canvasInstances.get(canvasId);
        canvasApi.save();
    }
    @JavascriptInterface
    @BatchCallHelper.BatchMethod("44")
    public void restore(int canvasId){
        if(DEBUG) Log.d(TAG, "restore, canvasId:" + canvasId);
        CanvasApi canvasApi = canvasInstances.get(canvasId);
        canvasApi.restore();
    }
    @JavascriptInterface
    @BatchCallHelper.BatchMethod("45")
    public void drawCanvas(int canvasId, int drawCanvasId, float offsetX, float offsetY){
        if(DEBUG) Log.d(TAG, "drawCanvas, canvasId:" + canvasId + ", drawCanvasId:" + drawCanvasId + ", offsetX:" + offsetX + ", offsetY:" + offsetY);
        CanvasApi canvasApi = canvasInstances.get(canvasId);
        if(canvasApi==null){
            Log.e(TAG, "canvas not found, id: " + canvasId);
            return;
        }
        CanvasApi drawCanvasApi = canvasInstances.get(drawCanvasId);
        if(drawCanvasApi==null){
            Log.e(TAG, "draw canvas not found, id: " + drawCanvasId);
            return;
        }
        canvasApi.drawCanvas(drawCanvasApi, offsetX, offsetY);
    }
    @JavascriptInterface
    @BatchCallHelper.BatchMethod("47")
    public void drawText(int canvasId, String text, float x, float y, int fillStyle){
        if(DEBUG) Log.d(TAG, "drawText, canvasId:" + canvasId + ", text:" + text + ", x:" + x + ", y:" + y + ", fillStyle:" + fillStyle);
        CanvasApi canvasApi = canvasInstances.get(canvasId);
        canvasApi.drawText(text, x, y, fillStyle);
    }

    private final Paint measureTextPaint = new Paint();
    @JavascriptInterface
    @BatchCallHelper.BatchMethod("48")
    public float measureText(String text, float textSize){
        if(DEBUG) Log.d(TAG, "measureText, text:" + text + ", textSize:" + textSize);
        synchronized (measureTextPaint) {
            measureTextPaint.setTextSize(textSize);
            return measureTextPaint.measureText(text);
        }
    }

    @JavascriptInterface
    @BatchCallHelper.BatchMethod("49")
    public void setFillColor(int canvasId, long color, int fillStyle){
        if(DEBUG) Log.d(TAG, "setFillColor, canvasId:" + canvasId + ", color:" + color + ", fillStyle:" + fillStyle);
        CanvasApi canvasApi = canvasInstances.get(canvasId);
        canvasApi.setColor((int) color, fillStyle);
    }
    @JavascriptInterface
    @BatchCallHelper.BatchMethod("50")
    public void multiplyAlpha(int canvasId, float alpha){
        if(DEBUG) Log.d(TAG, "multiplyAlpha, canvasId:" + canvasId + ", alpha:" + alpha);
        CanvasApi canvasApi = canvasInstances.get(canvasId);
        canvasApi.multiplyAlpha(alpha);
    }
    @JavascriptInterface
    @BatchCallHelper.BatchMethod("51")
    public void setAlpha(int canvasId, float alpha){
        if(DEBUG) Log.d(TAG, "setAlpha, canvasId:" + canvasId + ", alpha:" + alpha);
        CanvasApi canvasApi = canvasInstances.get(canvasId);
        canvasApi.setAlpha(alpha);
    }
    @JavascriptInterface
    @BatchCallHelper.BatchMethod("52")
    public void setTextAlign(int canvasId, String textAlign){
        if(DEBUG) Log.d(TAG, "setTextAlign, canvasId:" + canvasId + ", textAlign:" + textAlign);
        CanvasApi canvasApi = canvasInstances.get(canvasId);
        canvasApi.setTextAlign(textAlign);
    }
    @JavascriptInterface
    @BatchCallHelper.BatchMethod("53")
    public void setLineWidth(int canvasId, float lineWidth){
        if(DEBUG) Log.d(TAG, "setLineWidth, canvasId:" + canvasId + ", lineWidth:" + lineWidth);
        CanvasApi canvasApi = canvasInstances.get(canvasId);
        canvasApi.setLineWidth(lineWidth);
    }
    @JavascriptInterface
    @BatchCallHelper.BatchMethod("54")
    public void setLineCap(int canvasId, String cap){
        if(DEBUG) Log.d(TAG, "setLineCap, canvasId:" + canvasId + ", cap:" + cap);
        CanvasApi canvasApi = canvasInstances.get(canvasId);
        canvasApi.setLineCap(cap);
    }
    @JavascriptInterface
    @BatchCallHelper.BatchMethod("55")
    public void setLineJoin(int canvasId, String lineJoin){
        if(DEBUG) Log.d(TAG, "setLineJoin, canvasId:" + canvasId + ", lineJoin:" + lineJoin);
        CanvasApi canvasApi = canvasInstances.get(canvasId);
        canvasApi.setLineJoin(lineJoin);
    }
    @JavascriptInterface
    @BatchCallHelper.BatchMethod("56")
    public void setShadow(int canvasId, float radius, float dx, float dy, long color){
        if(DEBUG) Log.d(TAG, "setShadow, canvasId:" + canvasId + ", radius:" + radius + ", dx:" + dx + ", dy:" + dy + ", color:" + color);
        CanvasApi canvasApi = canvasInstances.get(canvasId);
        canvasApi.setShadow(radius, dx, dy, (int) color);
    }
    @JavascriptInterface
    @BatchCallHelper.BatchMethod("57")
    public void setFontSize(int canvasId, float textSize){
        if(DEBUG) Log.d(TAG, "setFontSize, canvasId:" + canvasId + ", textSize:" + textSize);
        CanvasApi canvasApi = canvasInstances.get(canvasId);
        canvasApi.setFontSize(textSize);
    }

    @JavascriptInterface
    @BatchCallHelper.BatchMethod("58")
    public void setFont(int canvasId, String fontName){
        if(DEBUG) Log.d(TAG, "setFont, canvasId:" + canvasId + ", fontName:" + fontName);
        CanvasApi canvasApi = canvasInstances.get(canvasId);
        canvasApi.setFont(fontName);
    }

    @JavascriptInterface
    @BatchCallHelper.BatchMethod("59")
    public void drawOval(int canvasId, float left, float top, float right, float bottom, int fillStyle){
        if(DEBUG) Log.d(TAG, "drawOval, canvasId:" + canvasId + ", left:" + left + ", top:" + top + ", right:" + right + ", bottom:" + bottom + ", fillStyle:" + fillStyle);
        CanvasApi canvasApi = canvasInstances.get(canvasId);
        canvasApi.drawOval(left, top, right, bottom, fillStyle);
    }

    @JavascriptInterface
    @BatchCallHelper.BatchMethod("60")
    public void drawCircle(int canvasId, float cx, float cy, float radius, int fillStyle){
        if(DEBUG) Log.d(TAG,"drawCircle, canvasId:"+ canvasId + ", cx:" + cx + ", cy:" + cy + ", radius:" + radius + ", fillStyle:" + fillStyle);
        CanvasApi canvasApi = canvasInstances.get(canvasId);
        canvasApi.drawCircle(cx, cy, radius, fillStyle);
    }

    @JavascriptInterface
    @BatchCallHelper.BatchMethod("61")
    public void drawArc(int canvasId, float left, float top, float right, float bottom, float startAngle, float sweepAngle, boolean useCenter, int fillStyle){
        if(DEBUG) Log.d(TAG,"drawArc, canvasId:"+ canvasId +", left:" + left + ", top:" + top + ", right:" + right + ", bottom:" + bottom
                + ", startAngle:" + startAngle + ", sweepAngle:" + sweepAngle + ", useCenter:" + useCenter + ", fillStyle:" + fillStyle);
        CanvasApi canvasApi = canvasInstances.get(canvasId);
        canvasApi.drawArc(left, top, right, bottom, startAngle, sweepAngle, useCenter, fillStyle);
    }

    @JavascriptInterface
    @BatchCallHelper.BatchMethod("62")
    public void drawRoundRect(int canvasId, float left, float top, float width, float height, float radiusTopLeft,
                              float radiusTopRight, float radiusBottomRight, float radiusBottomLeft, int fillStyle) {
        if (DEBUG) Log.d(TAG, "drawRoundRect, canvasId:"+ canvasId +", left:" + left + ", top:" + top + ", width:" + width + ", height:" + height
                    + ", radiusTopLeft:" + radiusTopLeft + ", radiusTopRight:" + radiusTopRight + ", radiusBottomRight:"
                    + radiusBottomRight + ", radiusBottomLeft:" + radiusBottomLeft + ", fillStyle:" + fillStyle);
        CanvasApi canvasApi = canvasInstances.get(canvasId);
        canvasApi.drawRoundRect(left, top, width, height, radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft, fillStyle);
    }

    //=================draw image=================

    @JavascriptInterface
    @BatchCallHelper.BatchMethod("70")
    public void drawImage(int canvasId, int drawImageId, float left, float top){
        if(DEBUG) Log.d(TAG, "drawImage, canvasId:" + canvasId + ", drawImageId:" + drawImageId + ", left:" + left + ", top:" + top);
        CanvasApi canvasApi = canvasInstances.get(canvasId);
        ImageApi imageApi = imageInstances.get(drawImageId);
        canvasApi.drawImage(imageApi, left, top);
    }
    @JavascriptInterface
    @BatchCallHelper.BatchMethod("71")
    public void drawImage(int canvasId, int drawImageId, float dstLeft, float dstTop, float dstRight, float dstBottom){
        if(DEBUG) Log.d(TAG, "drawImage, canvasId:" + canvasId + ", drawImageId:" + drawImageId
                + ", dstLeft:" + dstLeft + ", dstTop:" + dstTop + ", dstRight:" + dstRight + ", dstBottom:" + dstBottom);
        CanvasApi canvasApi = canvasInstances.get(canvasId);
        ImageApi imageApi = imageInstances.get(drawImageId);
        canvasApi.drawImage(imageApi, dstLeft, dstTop, dstRight, dstBottom);
    }
    @JavascriptInterface
    @BatchCallHelper.BatchMethod("72")
    public void drawImage(int canvasId, int drawImageId, float srcLeft, float srcTop, float srcRight, float srcBottom,
                          float dstLeft, float dstTop, float dstRight, float dstBottom){
        if(DEBUG) Log.d(TAG, "drawImage, canvasId:" + canvasId + ", drawImageId:" + drawImageId
                + ", srcLeft:" + srcLeft + ", srcTop:" + srcTop + ", srcRight:" + srcRight + ", srcBottom:" + srcBottom
                + ", dstLeft:" + dstLeft + ", dstTop:" + dstTop + ", dstRight:" + dstRight + ", dstBottom:" + dstBottom);
        CanvasApi canvasApi = canvasInstances.get(canvasId);
        ImageApi imageApi = imageInstances.get(drawImageId);
        canvasApi.drawImage(imageApi, srcLeft, srcTop, srcRight, srcBottom, dstLeft, dstTop, dstRight, dstBottom);
    }

    //=================image api==================

    @JavascriptInterface
    @BatchCallHelper.BatchMethod(value = "80", batchCantSkip = true)
    public void createImage(int imageId){
        if(DEBUG) Log.d(TAG, "createImage, imageId:" + imageId);
        ImageApi imageApi = new ImageApi(this);
        ImageApi oldImage = imageInstances.get(imageId);
        if(oldImage!=null){
            Log.e(TAG, "Create image warn: there has a old imageId instance. Override it.");
            oldImage.recycle();
        }
        imageInstances.put(imageId, imageApi);
    }
    @JavascriptInterface
    @BatchCallHelper.BatchMethod("81")
    public void loadImage(int imageId, String src){
        if(DEBUG) Log.d(TAG, "loadImage, imageId:" + imageId + ", src:" + src);
        ImageApi imageApi = imageInstances.get(imageId);
        imageApi.loadImage(src);
    }
    @JavascriptInterface
    @BatchCallHelper.BatchMethod("82")
    public void recycleImage(int imageId){
        if(DEBUG) Log.d(TAG, "recycleImage, imageId:" + imageId);
        ImageApi imageApi = imageInstances.get(imageId);
        imageApi.recycle();
    }
    @JavascriptInterface
    @BatchCallHelper.BatchMethod("83")
    public void getPixels(int imageId, int callBackIndex, float left, float top, float right, float bottom){
        if(DEBUG) Log.d(TAG, "getPixels, imageId:" + imageId + ", callBackIndex:" + callBackIndex
                + ", left:" + left+ ", top:" + top+ ", right:" + right+ ", bottom:" + bottom);
        ImageApi imageApi = imageInstances.get(imageId);
        Bitmap bitmap = imageApi.getBitmap();
        if(bitmap==null) notifyImageGetPixels(imageId, callBackIndex, new int[0]);
        else{
            int width = (int)right - (int)left;
            int height = (int)bottom - (int)top;
            int[] data = new int[width*height];
            bitmap.getPixels(data, 0, width, (int)left, (int)top, width, height);
            notifyImageGetPixels(imageId, callBackIndex, data);
        }
    }

}
