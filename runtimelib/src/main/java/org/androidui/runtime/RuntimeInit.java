package org.androidui.runtime;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import java.util.Arrays;
import java.util.WeakHashMap;

/**
 * Created by linfaxin on 16/4/3.
 * Init
 */
public class RuntimeInit {
    private static boolean initGlobal = false;
    public static synchronized void init(Context context){
        if(!initGlobal) {
            initGlobal = true;
            ImageApi.initImageLoader(context, false);
            BatchCallHelper.initBatchMethodCache();

            new Thread(){
                @Override
                public void run() {
                    initMeasureWidthData();
                }
            }.start();
        }
    }

    protected static WeakHashMap<ViewGroup, RuntimeBridge> weakInstanceMap = new WeakHashMap<ViewGroup, RuntimeBridge>();
    public static RuntimeBridge initWebView(WebView webView){
        init(webView.getContext().getApplicationContext());

        //for editText/htmlView overlay draw.
        webView.setBackgroundColor(Color.TRANSPARENT);

        RuntimeBridge bridge = weakInstanceMap.get(webView);
        if(bridge==null){
            bridge = new RuntimeBridgeHW(webView);
            weakInstanceMap.put(webView, bridge);
        }

        return bridge;
    }

    private static final Paint measureTextPaint = new Paint();
    static String MeasureWidthsJS;
    static void initMeasureWidthData(){
        synchronized (measureTextPaint) {
            if(MeasureWidthsJS!=null) return;
            int length = 0x3400;
            char[] chars = new char[length];
            float[] widths = new float[length];
            for(int i=0; i<length; i++){
                chars[i] = (char) i;
            }

            int measureTextSize = 1000;
            measureTextPaint.setTextSize(measureTextSize);
            measureTextPaint.getTextWidths(chars, 0, length, widths);
            int[] widthsInt = new int[length];
            for(int i=0; i<length; i++){
                widthsInt[i] = (int) widths[i];
            }

            float[] defaultWidths = new float[1];
            measureTextPaint.getTextWidths("阿", defaultWidths);

            MeasureWidthsJS = "androidui.native.NativeCanvas.applyTextMeasure("
                    + measureTextSize + "," + (int)(defaultWidths[0]) + "," + Arrays.toString(widthsInt) + ")";
        }

    }

}
