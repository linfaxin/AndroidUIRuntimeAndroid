package org.androidui.runtime.sample;

import android.app.Application;
import android.os.Build;
import android.webkit.WebView;

import org.androidui.runtime.*;

/**
 * Created by linfaxin on 16/4/3.
 */
public class MyApp extends Application{
    @Override
    public void onCreate() {
        super.onCreate();
        RuntimeInit.init(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);
        }
        RuntimeBridge.DEBUG_TRACK_FPS = true;
    }
}
