package com.qxcloud.imageprocess.activity.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;

import android.view.View;

import com.qxcloud.imageprocess.utils.CameraUtils;

/**
 * Created by cfh on 2017-09-13.
 * 自定义相机选景框
 */

public class RectMackView extends View {
    private Paint linePaint;//绘制透明取景框
    private Paint rectPaint;//绘制周边阴影
    private int width,height;
    private double screenWidth;
    private double screenHeight;

    public RectMackView(Context context) {
        super(context);
        init();
    }

    public RectMackView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RectMackView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        //绘制中间透明区域矩形边界的Paint
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.BLUE);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(3f);
        linePaint.setAlpha(80);//取值范围为0~255，数值越小越透明。

        //绘制四周矩形阴影区域
        rectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rectPaint.setColor(Color.GRAY);//绘制周边的色值
        rectPaint.setStyle(Paint.Style.FILL);
        rectPaint.setAlpha(80);//取值范围为0~255，数值越小越透明。
    }




    @Override
    protected void onDraw(Canvas canvas) {
         screenWidth = CameraUtils.getScreenWH(getContext()).widthPixels;//屏幕宽
         screenHeight = CameraUtils.getScreenWH(getContext()).heightPixels;//屏幕高
        if (screenWidth == 0 && screenHeight == 0) {
            return;
        }
        double width_ = screenWidth / 8;
        double height_ = screenHeight / 4;
        width = (int) Math.ceil(width_);
        height = (int) Math.ceil(height_);
        int screenWidth_= (int) screenWidth;
        int screenHeight_= (int) screenHeight;
//			上
        canvas.drawRect(0, 0, screenWidth_,height, this.rectPaint);
//			右
        canvas.drawRect(screenWidth_ - width*2, height, screenWidth_, screenHeight_, this.rectPaint);
//			下
        canvas.drawRect(0, screenHeight_ - height, screenWidth_ - width*2, screenHeight_, this.rectPaint);
//			左
        canvas.drawRect(0,  height, width,  screenHeight_ - height, this.rectPaint);

        canvas.drawRect(width, height, screenWidth_ - width*2, screenHeight_ - height, this.linePaint);
        super.onDraw(canvas);
    }

    public int getSplitWidth() {
        return width;
    }


    public int getSplitHeight() {
        return height;
    }

    public double getScreenWidth() {
        return screenWidth;
    }

    public double getScreenHeight() {
        return screenHeight;
    }

}
