package org.androidui.runtime.sample;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final EditText site_input = (EditText) findViewById(R.id.site_input);
        Button site_input_go_btn = (Button) findViewById(R.id.site_input_go_btn);
        site_input_go_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = site_input.getText().toString();
                Uri uri = Uri.parse(url);
                if(TextUtils.isEmpty(uri.getScheme())){
                    url = "http://" + url;
                }
                openWeb(url);
            }
        });
    }

    public void openWeb(View view) {
        openWeb(view.getTag().toString());
    }

    public void openWeb(String url){
        startActivity(new Intent(this, WebActivity.class).setData(Uri.parse(url)));
    }
}
