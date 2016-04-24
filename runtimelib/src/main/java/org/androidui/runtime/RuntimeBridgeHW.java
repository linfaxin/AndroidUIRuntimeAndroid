package org.androidui.runtime;

import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import java.util.Vector;


/**
 * Created by linfaxin on 15/12/15.
 * Js Bridge
 */
public class RuntimeBridgeHW extends RuntimeBridge{

    private Vector<String> pendingBatchStrings = new Vector<>();
    private String currentBatchString;

    private Runnable drawBatchRun = new Runnable() {
        @Override
        public void run() {
            int size = pendingBatchStrings.size();
            String willCallBatchString;

            if(size==0){//no new draw batch call, draw last.
                willCallBatchString = currentBatchString;

            } else if(size==1){
                willCallBatchString = pendingBatchStrings.remove(0);

            }else{
                while(true){
                    String call = pendingBatchStrings.remove(0);
                    if(pendingBatchStrings.size() == 0 || BatchCallHelper.cantSkipBatchCall(call)) {
                        willCallBatchString = call;
                        break;
                    }
                }
            }

            currentBatchString = willCallBatchString;
            BatchCallHelper.parseAndRun(RuntimeBridgeHW.this, currentBatchString);
            if(DEBUG_TRACK_FPS) trackFPS();
        }
    };

    protected RuntimeBridgeHW(WebView webView) {
        super(webView);
    }

    @JavascriptInterface
    public void batchCall(final String batchString){
        final HWSurfaceApi surfaceApi = (HWSurfaceApi) surfaceInstances.valueAt(0);
        if(surfaceApi!=null){
            pendingBatchStrings.add(batchString);
            surfaceApi.postOnDraw(drawBatchRun);
        }
    }

    protected SurfaceApi createSurfaceApi(Context context){
        return new HWSurfaceApi(context, this);
    }

    protected void onSurfaceApiCreated(SurfaceApi api){
        notifySurfaceSupportDirtyDraw(api, false);
    }
}
