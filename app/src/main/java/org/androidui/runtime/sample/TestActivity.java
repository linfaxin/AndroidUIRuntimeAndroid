package org.androidui.runtime.sample;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
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

            float left = 100;
            float top = 100;
            float width = 300;
            float height = 300;
            float radiusTopLeft = 80;
            float radiusTopRight = 80;
            float radiusBottomRight = 80;
            float radiusBottomLeft = 80;

            Path path = new Path();
//            path.moveTo(left+radiusTopLeft, top);
//            path.arcTo(left+width-radiusTopRight, top, left+width, top+radiusTopRight, 0, 90, true);
//            path.close();


//            path.arcTo(left+width, top+height, left+width-radiusBottomRight, top+height, radiusBottomRight);
//            path.arcTo(left, top+height, left, top+height-radiusBottomLeft, radiusBottomLeft);
//            path.arcTo(left, top, left + radiusTopLeft, top, radiusTopLeft);

            float[] radii = new float[]{200, 200, 200, 200, 200, 200, 200, 200};
            path.addRoundRect(new RectF(left, top, left+width, top+height), radii, Path.Direction.CW);

            canvas.drawPath(path, mPaint);

        }
    }
}
