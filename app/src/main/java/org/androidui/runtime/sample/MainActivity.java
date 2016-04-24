package org.androidui.runtime.sample;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;

import org.androidui.runtime.RuntimeInit;


public class MainActivity extends Activity {
    WebView webView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        setProgressBarIndeterminateVisibility(true);


        setContentView(R.layout.activity_main);

        webView = (WebView) findViewById(R.id.webView);
        RuntimeInit.initWebView(webView);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
//                startActivity(new Intent(view.getContext(), MainActivity.class).setData(Uri.parse(url)));
//                return true;
                return false;
            }
        });

        if(getIntent().getData()!=null){
            performLoad(getIntent().getDataString());

        }else{
            final EditText editText = new EditText(this);
            editText.setText("http://172.16.3.240:8765/sample/main.html");
//            editText.setText("http://192.168.31.107:8000/sample/main.html");
            new AlertDialog.Builder(this)
                    .setTitle("输入网址")
                    .setView(editText)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            performLoad(editText.getText().toString());
                        }
                    })
//                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            startActivity(new Intent(MainActivity.this, TestActivity.class));
//                            finish();
//                        }
//                    })
                    .setCancelable(false)
                    .show();
        }
    }

    private void performLoad(final String url){
        webView.loadUrl(url);
    }

    @Override
    public void onBackPressed() {
        if(webView.canGoBack()){
            webView.goBack();
            return;
        }
        super.onBackPressed();
    }
}
