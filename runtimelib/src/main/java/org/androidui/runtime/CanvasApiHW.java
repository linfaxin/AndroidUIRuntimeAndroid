package org.androidui.runtime;

import android.graphics.Canvas;
import android.support.annotation.Nullable;

import java.util.ArrayList;

/**
 * Created by linfaxin on 16/4/24.
 */
public class CanvasApiHW extends CanvasApi{
    private int width, height;
    private ArrayList<Runnable> drawRecords = new ArrayList<>();

    public CanvasApiHW(int width, int height) {
        super(null);
        this.width = width;
        this.height = height;
    }

    @Nullable
    @Override
    public Canvas getCanvas() {
        return CanvasApiHW.super.getCanvas();
    }

    @Override
    protected void drawFromParentCanvas(CanvasApi parentCanvasApi, float offsetX, float offsetY) {
        Canvas parentCanvas = parentCanvasApi.getCanvas();
        if(parentCanvas==null) return;
        if(drawRecords.size()==0) return;

        this.canvas = parentCanvas;
        int saveCount = parentCanvas.save();
        canvas.translate(offsetX, offsetY);
        parentCanvas.clipRect(0, 0, width, height);//clip width & height

        this.mPaint.setAlpha(parentCanvasApi.mPaint.getAlpha());//apply parent canvas's current alpha
        //run records
        for(Runnable run : drawRecords){
            run.run();
        }

        parentCanvas.restoreToCount(saveCount);
        this.canvas = null;
    }

    @Override
    public void recycle() {
        CanvasApiHW.super.recycle();
        drawRecords.clear();
    }

    @Override
    public void translate(final float tx, final float ty) {
        Runnable runnable = new Runnable() {
            public void run() {
                CanvasApiHW.super.translate(tx, ty);
            }
        };
        drawRecords.add(runnable);
    }

    @Override
    public void scale(final float sx, final float sy) {
        Runnable runnable = new Runnable() {
            public void run() {
                CanvasApiHW.super.scale(sx, sy);
            }
        };
        drawRecords.add(runnable);
    }

    @Override
    public void rotate(final float degrees) {
        Runnable runnable = new Runnable() {
            public void run() {
                CanvasApiHW.super.rotate(degrees);
            }
        };
        drawRecords.add(runnable);
    }

    @Override
    public void concat(final float MSCALE_X, final float MSKEW_X, final float MTRANS_X, final float MSKEW_Y, final float MSCALE_Y, final float MTRANS_Y) {
        Runnable runnable = new Runnable() {
            public void run() {
                CanvasApiHW.super.concat(MSCALE_X, MSKEW_X, MTRANS_X, MSKEW_Y, MSCALE_Y, MTRANS_Y);
            }
        };
        drawRecords.add(runnable);
    }

    @Override
    public void drawColor(final int color) {
        Runnable runnable = new Runnable() {
            public void run() {
                CanvasApiHW.super.drawColor(color);
            }
        };
        drawRecords.add(runnable);
    }

    @Override
    public void clearColor() {
//        Runnable runnable = new Runnable() {
//            public void run() {
//                CanvasApiHW.super.clearColor();
//            }
//        };
//        drawRecords.add(runnable);
        drawRecords.clear();//FIXME clear on current clip
    }

    @Override
    public void drawRect(final float left, final float top, final float width, final float height, final int fillStyle) {
        Runnable runnable = new Runnable() {
            public void run() {
                CanvasApiHW.super.drawRect(left, top, width, height, fillStyle);
            }
        };
        drawRecords.add(runnable);
    }

    @Override
    public void clipRect(final float left, final float top, final float width, final float height) {
        Runnable runnable = new Runnable() {
            public void run() {
                CanvasApiHW.super.clipRect(left, top, width, height);
            }
        };
        drawRecords.add(runnable);
    }

    @Override
    public void save() {
        Runnable runnable = new Runnable() {
            public void run() {
                CanvasApiHW.super.save();
            }
        };
        drawRecords.add(runnable);
    }

    @Override
    public void restore() {
        Runnable runnable = new Runnable() {
            public void run() {
                CanvasApiHW.super.restore();
            }
        };
        drawRecords.add(runnable);
    }

    @Override
    public void drawCanvas(final CanvasApi drawCanvasApi, final float offsetX, final float offsetY) {
        Runnable runnable = new Runnable() {
            public void run() {
                CanvasApiHW.super.drawCanvas(drawCanvasApi, offsetX, offsetY);
            }
        };
        drawRecords.add(runnable);
    }

    @Override
    public void drawImage(final ImageApi imageApi, final float left, final float top) {
        Runnable runnable = new Runnable() {
            public void run() {
                CanvasApiHW.super.drawImage(imageApi, left, top);
            }
        };
        drawRecords.add(runnable);
    }

    @Override
    public void drawImage(final ImageApi imageApi, final float dstLeft, final float dstTop, final float dstRight, final float dstBottom) {
        Runnable runnable = new Runnable() {
            public void run() {
                CanvasApiHW.super.drawImage(imageApi, dstLeft, dstTop, dstRight, dstBottom);
            }
        };
        drawRecords.add(runnable);
    }

    @Override
    public void drawImage(final ImageApi imageApi, final float srcLeft, final float srcTop, final float srcRight, final float srcBottom, final float dstLeft, final float dstTop, final float dstRight, final float dstBottom) {
        Runnable runnable = new Runnable() {
            public void run() {
                CanvasApiHW.super.drawImage(imageApi, srcLeft, srcTop, srcRight, srcBottom, dstLeft, dstTop, dstRight, dstBottom);
            }
        };
        drawRecords.add(runnable);
    }

    @Override
    public void drawText(final String text, final float x, final float y, final int fillStyle) {
        Runnable runnable = new Runnable() {
            public void run() {
                CanvasApiHW.super.drawText(text, x, y, fillStyle);
            }
        };
        drawRecords.add(runnable);
    }

    @Override
    public void setColor(final int color, final int fillStyle) {
        Runnable runnable = new Runnable() {
            public void run() {
                CanvasApiHW.super.setColor(color, fillStyle);
            }
        };
        drawRecords.add(runnable);
    }

    @Override
    public void multiplyAlpha(final float alpha) {
        Runnable runnable = new Runnable() {
            public void run() {
                CanvasApiHW.super.multiplyAlpha(alpha);
            }
        };
        drawRecords.add(runnable);
    }

    @Override
    public void setAlpha(final float alpha) {
        Runnable runnable = new Runnable() {
            public void run() {
                CanvasApiHW.super.setAlpha(alpha);
            }
        };
        drawRecords.add(runnable);
    }

    @Override
    public void setTextAlign(final String textAlign) {
        Runnable runnable = new Runnable() {
            public void run() {
                CanvasApiHW.super.setTextAlign(textAlign);
            }
        };
        drawRecords.add(runnable);
    }

    @Override
    public void setLineWidth(final float lineWidth) {
        Runnable runnable = new Runnable() {
            public void run() {
                CanvasApiHW.super.setLineWidth(lineWidth);
            }
        };
        drawRecords.add(runnable);
    }

    @Override
    public void setLineCap(final String cap) {
        Runnable runnable = new Runnable() {
            public void run() {
                CanvasApiHW.super.setLineCap(cap);
            }
        };
        drawRecords.add(runnable);
    }

    @Override
    public void setLineJoin(final String lineJoin) {
        Runnable runnable = new Runnable() {
            public void run() {
                CanvasApiHW.super.setLineJoin(lineJoin);
            }
        };
        drawRecords.add(runnable);
    }

    @Override
    public void setShadow(final float radius, final float dx, final float dy, final int color) {
        Runnable runnable = new Runnable() {
            public void run() {
                CanvasApiHW.super.setShadow(radius, dx, dy, color);
            }
        };
        drawRecords.add(runnable);
    }

    @Override
    public void setFontSize(final float textSize) {
        Runnable runnable = new Runnable() {
            public void run() {
                CanvasApiHW.super.setFontSize(textSize);
            }
        };
        drawRecords.add(runnable);
    }

    @Override
    public void drawOval(final float left, final float top, final float right, final float bottom, final int fillStyle) {
        Runnable runnable = new Runnable() {
            public void run() {
                CanvasApiHW.super.drawOval(left, top, right, bottom, fillStyle);
            }
        };
        drawRecords.add(runnable);
    }

    @Override
    public void drawCircle(final float cx, final float cy, final float radius, final int fillStyle) {
        Runnable runnable = new Runnable() {
            public void run() {
                CanvasApiHW.super.drawCircle(cx, cy, radius, fillStyle);
            }
        };
        drawRecords.add(runnable);
    }

    @Override
    public void drawArc(final float left, final float top, final float right, final float bottom, final float startAngle, final float sweepAngle, final boolean useCenter, final int fillStyle) {
        Runnable runnable = new Runnable() {
            public void run() {
                CanvasApiHW.super.drawArc(left, top, right, bottom, startAngle, sweepAngle, useCenter, fillStyle);
            }
        };
        drawRecords.add(runnable);
    }

    @Override
    public void drawRoundRect(final float left, final float top, final float width, final float height, final float radiusTopLeft, final float radiusTopRight, final float radiusBottomRight, final float radiusBottomLeft, final int fillStyle) {
        Runnable runnable = new Runnable() {
            public void run() {
                CanvasApiHW.super.drawRoundRect(left, top, width, height, radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft, fillStyle);
            }
        };
        drawRecords.add(runnable);
    }

    @Override
    public void setFont(final String fontName) {
        Runnable runnable = new Runnable() {
            public void run() {
                CanvasApiHW.super.setFont(fontName);
            }
        };
        drawRecords.add(runnable);
    }
}
