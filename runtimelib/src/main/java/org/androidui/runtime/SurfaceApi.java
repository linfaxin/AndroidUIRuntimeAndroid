package org.androidui.runtime;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.lang.ref.WeakReference;


/**
 * Created by linfaxin on 15/12/14.
 *
 */
public class SurfaceApi implements SurfaceHolder.Callback {
    private WeakReference<View> surfaceRef;
    protected final Rect lockRectTemp = new Rect();
    protected RuntimeBridge runtimeBridge;

    public SurfaceApi(Context context, RuntimeBridge runtimeBridge){
        View surfaceView = createSurfaceView(context);
        this.surfaceRef = new WeakReference<>(surfaceView);
        this.runtimeBridge = runtimeBridge;
    }

    public View createSurfaceView(Context context){
        SurfaceView surfaceView = new SurfaceView(context);
        surfaceView.getHolder().addCallback(this);
        surfaceView.setZOrderOnTop(true);
//        surfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        return surfaceView;
    }

    @Nullable
    public View getSurfaceView(){
        return surfaceRef.get();
    }


    @Nullable
    public CanvasApi lockCanvas(float left, float top, float right, float bottom){
        SurfaceView surfaceView = (SurfaceView) getSurfaceView();
        Canvas canvas = null;
        if(surfaceView!=null){

            lockRectTemp.set((int) left, (int) top, (int) right, (int) bottom);
            canvas = surfaceView.getHolder().lockCanvas(lockRectTemp);
            if(canvas==null){
                throw new RuntimeException("lock canvas fail, area:" + new RectF(left, top, right, bottom).toShortString());
            }
        }
        CanvasApi canvasApi = new CanvasApi(canvas);
        //canvasApi.clearRect(left, top, right - left, bottom - top);//lock with clear
        return canvasApi;
    }

    public void unlockCanvasAndPost(CanvasApi canvasApi){
        SurfaceView surfaceView = (SurfaceView) getSurfaceView();
        Canvas canvas = canvasApi.getCanvas();
        if(surfaceView!=null && canvas!=null){
            surfaceView.getHolder().unlockCanvasAndPost(canvas);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        this.runtimeBridge.notifySurfaceReady(this);
    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }
}
