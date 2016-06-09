package org.androidui.runtime.cordova;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.webkit.WebView;

import org.androidui.runtime.RuntimeInit;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;

/**
 * Created by linfaxin on 16/6/9.
 * Cordova's Plugin
 */
public class CordovaPluginFile extends CordovaPlugin {
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        View view = webView.getView();
        if(view instanceof WebView){
            RuntimeInit.initWebView((WebView)view);
        }
        ((Activity)view.getContext()).getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
    }

}
