package org.androidui.runtime;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.webkit.WebView;

/**
 * Created by linfaxin on 15/12/18.
 *
 */
public class SurfaceApiHW extends SurfaceApi{
    private CanvasApi drawingCanvas = new CanvasApi(null);
    private Runnable postOnDrawRun;
    private static final int MaxAutoRedrawFlag = 10;
    private int autoRedrawFlag = 0;

    public SurfaceApiHW(Context context, RuntimeBridge runtimeBridge) {
        super(context, runtimeBridge);
    }

    public View createSurfaceView(Context context){
        return new DrawView(context);
    }

    @Nullable
    public CanvasApi lockCanvas(float left, float top, float right, float bottom){
        return drawingCanvas;
    }

    public void unlockCanvasAndPost(CanvasApi canvasApi){
        //do nothing
    }

    public void postOnDraw(Runnable run){
        postOnDrawRun = run;
        if(autoRedrawFlag > MaxAutoRedrawFlag){
            View drawView = getSurfaceView();
            if(drawView!=null){
                ViewCompat.postInvalidateOnAnimation(drawView);
            }
        }
        autoRedrawFlag = 0;
    }

    //draw on this view, use HW canvas to draw
    private class DrawView extends View {
        public DrawView(Context context) {
            super(context);
        }

        private boolean skipDrawFlag = false;
        @Override
        public void draw(Canvas canvas) {
            if(skipDrawFlag) return;
            super.draw(canvas);
            drawingCanvas.reset(canvas);
            if(postOnDrawRun!=null) postOnDrawRun.run();

            skipDrawFlag = true;
            runtimeBridge.drawHTMLBoundToCanvas(canvas);
            skipDrawFlag = false;


            if(autoRedrawFlag++ < MaxAutoRedrawFlag){
                ViewCompat.postInvalidateOnAnimation(this);
            }
        }
    }
}
