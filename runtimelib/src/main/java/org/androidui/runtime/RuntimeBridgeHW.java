package org.androidui.runtime;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;


/**
 * Created by linfaxin on 15/12/15.
 * Js Bridge
 */
public class RuntimeBridgeHW extends RuntimeBridge{

    protected RuntimeBridgeHW(WebView webView) {
        super(webView);
    }

    @Override
    @JavascriptInterface
    public void batchCall(final String batchString){
        final SurfaceApiHW surfaceApi = (SurfaceApiHW) surfaceInstances.valueAt(0);
        if(surfaceApi!=null){
            BatchCallHelper.BatchCallResult result = BatchCallHelper.BatchCallResult.obtain(this, batchString);
            pendingBatchResult.add(result);
            surfaceApi.postOnDraw(queryPendingAndRun);
        }
    }

    @Override
    protected SurfaceApi createSurfaceApi(Context context, int surfaceId){
        notifySurfaceSupportDirtyDraw(surfaceId, false);
        return new SurfaceApiHW(context, this);
    }

    @Override
    protected CanvasApi createCanvasApi(int width, int height) {
        return new CanvasApiHW(width, height);
    }

    @Override
    @JavascriptInterface
    public void showDrawHTMLBound(int viewHash, float left, float top, float right, float bottom) {
        super.showDrawHTMLBound(viewHash, left, top, right, bottom);
        WebView webView = getWebView();
        if(webView!=null) ViewCompat.postInvalidateOnAnimation(webView);
    }

    @Override
    @JavascriptInterface
    public void hideDrawHTMLBound(int viewHash) {
        super.hideDrawHTMLBound(viewHash);
        WebView webView = getWebView();
        if(webView!=null) ViewCompat.postInvalidateOnAnimation(webView);
    }
}
