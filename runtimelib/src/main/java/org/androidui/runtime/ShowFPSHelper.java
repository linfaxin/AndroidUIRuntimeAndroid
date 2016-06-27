package org.androidui.runtime;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

/**
 * Created by linfaxin on 16/6/27.
 */
public class ShowFPSHelper {
    public static boolean DEBUG_TRACK_FPS = false;

    ViewGroup viewGroup;

    private int mUIFPS = 0;
    private int mJSFPS = 0;

    private long mFpsStartTime = -1;
    private long mFpsPrevTime = -1;
    private int mFpsNumFrames = 0;
    private TextView mFPSShowText;

    public ShowFPSHelper(ViewGroup viewGroup) {
        this.viewGroup = viewGroup;
    }

    public void showJSFPS(float fps){
        DEBUG_TRACK_FPS = true;
        mJSFPS = (int) fps;
    }

    public void trackUIFPS(){
        if(!DEBUG_TRACK_FPS) return;
        long nowTime = System.currentTimeMillis();
        if (this.mFpsStartTime < 0) {
            this.mFpsStartTime = this.mFpsPrevTime = nowTime;
            this.mFpsNumFrames = 0;
        } else {
            this.mFpsNumFrames++;
//            long frameTime = nowTime - this.mFpsPrevTime;
            long totalTime = nowTime - this.mFpsStartTime;
            //Log.v(ViewRootImpl.TAG, "Frame time:\t" + frameTime);
            this.mFpsPrevTime = nowTime;
            if (totalTime > 1000) {
                mUIFPS = (int) (this.mFpsNumFrames * 1000 / totalTime);
                Log.d("trachFPS", "UIFPS:\t" + mUIFPS);
                showFpsToTextView();

                this.mFpsStartTime = nowTime;
                this.mFpsNumFrames = 0;
            }
        }
    }

    private void showFpsToTextView(){
        ViewGroup viewGroup = this.viewGroup;
        if( (mFPSShowText==null || mFPSShowText.getParent()==null) && viewGroup!=null){
            Context context = viewGroup.getContext();
            mFPSShowText = new TextView(context);
            mFPSShowText.setTextSize(12);
            mFPSShowText.setBackgroundColor(Color.BLACK);
            mFPSShowText.setTextColor(Color.WHITE);
            mFPSShowText.setAlpha(0.7f);
            ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(-2, -2);
            viewGroup.addView(mFPSShowText, layoutParams);
        }
        if(mFPSShowText!=null){
            mFPSShowText.setText("UIFPS:"+mUIFPS + "\nJSFPS:" + mJSFPS);
        }
    }
}
