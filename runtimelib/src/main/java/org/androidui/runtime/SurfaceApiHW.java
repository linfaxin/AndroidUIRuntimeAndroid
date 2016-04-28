package org.androidui.runtime;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.View;

import java.lang.ref.WeakReference;

/**
 * Created by linfaxin on 15/12/18.
 *
 */
public class SurfaceApiHW extends SurfaceApi{

    public SurfaceApiHW(Context context, RuntimeBridge runtimeBridge) {
        super(context, runtimeBridge);
    }

    public View createSurfaceView(Context context){
        return new DrawView(context);
    }
    @Nullable
    public DrawView getSurfaceView(){
        return (DrawView) super.getSurfaceView();
    }

    @Nullable
    public CanvasApi lockCanvas(float left, float top, float right, float bottom){
        DrawView drawView = getSurfaceView();
        if(drawView!=null){
            return drawView.drawingCanvas;
        }
        return null;
    }

    public void unlockCanvasAndPost(CanvasApi canvasApi){
        //do nothing
    }

    public void postOnDraw(Runnable run){
        DrawView drawView = getSurfaceView();
        if(drawView!=null){
            drawView.onDrawRun = run;
            ViewCompat.postInvalidateOnAnimation(drawView);
        }
    }

    //draw on this view, use HW canvas to draw
    private static class DrawView extends View {
        CanvasApi drawingCanvas;
        Runnable onDrawRun;
        public DrawView(Context context) {
            super(context);
        }

        @Override
        public void draw(Canvas canvas) {
            super.draw(canvas);
            if(drawingCanvas==null){
                drawingCanvas = new CanvasApi(canvas);
            }else{
                drawingCanvas.reset(canvas);
            }
            if(onDrawRun!=null) onDrawRun.run();
        }
    }
}
