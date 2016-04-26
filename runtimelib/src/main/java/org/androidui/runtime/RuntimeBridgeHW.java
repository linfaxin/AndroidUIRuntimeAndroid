package org.androidui.runtime;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import java.util.Vector;


/**
 * Created by linfaxin on 15/12/15.
 * Js Bridge
 */
public class RuntimeBridgeHW extends RuntimeBridge{

    protected RuntimeBridgeHW(WebView webView) {
        super(webView);
    }

    @JavascriptInterface
    public void batchCall(final String batchString){
        final SurfaceApiHW surfaceApi = (SurfaceApiHW) surfaceInstances.valueAt(0);
        if(surfaceApi!=null){
            BatchCallHelper.BatchCallParseResult result = BatchCallHelper.parse(this, batchString);
            pendingBatchResult.add(result);
            surfaceApi.postOnDraw(queryPendingAndRun);
        }
    }

    @Override
    protected SurfaceApi createSurfaceApi(Context context, int surfaceId){
        notifySurfaceSupportDirtyDraw(surfaceId, false);
        notifyCanvasCacheEnable(false);
        return new SurfaceApiHW(context, this);
    }

    @Override
    protected CanvasApi createCanvasApi(int width, int height) {
        return new CanvasApiHW(width, height);
    }


}
