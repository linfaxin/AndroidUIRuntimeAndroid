package org.androidui.runtime.sample;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

/**
 * Created by linfaxin on 15/12/20.
 */
public class TestActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new MyView(this));
    }

    static class MyView extends View {
        Paint mPaint = new Paint();
        public MyView(Context context) {
            super(context);
            mPaint.setAntiAlias(true);
        }

        @Override
        public void draw(Canvas canvas) {
            super.draw(canvas);
//            canvas.save();
//            canvas.translate(0, 0);
//            canvas.clipRect(0, 0, 347, 114);

//            canvas.save();
//            mPaint.setColor(0xffcccccc);
//            canvas.drawRect(0, 0, 347, 116, mPaint);
//            canvas.restore();

//            canvas.save();
            canvas.clipRect(30, 0, 287, 116);
            canvas.translate(30, 30);

//            canvas.save();
            mPaint.setColor(Color.BLACK);
            mPaint.setTextSize(42);
            canvas.drawText("文本Padding10", 0, 45, mPaint);

//            canvas.restore();
//            canvas.restore();
//            canvas.restore();

        }
    }
}
