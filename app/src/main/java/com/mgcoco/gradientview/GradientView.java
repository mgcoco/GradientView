package com.mgcoco.gradientview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.IntRange;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class GradientView extends View {

    static {
        System.loadLibrary("native-lib");
    }

    public native void setup(Object[] controlPoints, int orientation, int width, int viewHeight, int[] colors, int[] bitmap);

    private ArrayList<ControlPoint> controlPoints = new ArrayList<>();

    private int viewWidth;

    private int viewHeight;

    public static final int HORIZONTAL = 0;

    public static final int VERTICAL = 1;

    private int orientation = VERTICAL;

    private int[] gradientColor = new int[]{Color.GRAY, Color.WHITE, Color.BLACK};

    private Paint paint = new Paint();

    private Bitmap gradientBmp;

    public GradientView(Context context) {
        super(context);
        init(context, null);
    }

    public GradientView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public GradientView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs){
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.GradientView);
        if(ta != null) {
            orientation = ta.getInteger(R.styleable.GradientView_orientation, VERTICAL);

            int id = ta.getResourceId(R.styleable.GradientView_gradientColor, 0);
            if (id != 0) {
                gradientColor = getResources().getIntArray(id);
            }

            id = ta.getResourceId(R.styleable.GradientView_controlPoints, 0);
            if(id != 0) {
                ArrayList<ControlPoint> tmpControlPoints = new ArrayList<>();
                String[] controlPointsArray = getResources().getStringArray(id);
                for(String controlPoint: controlPointsArray) {
                    try{
                        String[] currentPoint = controlPoint.split(",");
                        tmpControlPoints.add(new ControlPoint(Float.valueOf(currentPoint[0]), Float.valueOf(currentPoint[1])));
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                }
                if(tmpControlPoints.size() > 1){
                    setControlPoints(tmpControlPoints);
                }
            }
        }
    }

    public void setControlPoints(@Size(min = 2) ArrayList<ControlPoint> points){
        controlPoints = points;
        if(orientation == VERTICAL){
            Collections.sort(controlPoints, (Comparator<ControlPoint>) (o1, o2) -> (int)(o1.getX() - o2.getX()));
        }
        else{
            Collections.sort(controlPoints, (Comparator<ControlPoint>) (o1, o2) -> (int)(o1.getY() - o2.getY()));
        }
        syncDraw();
    }

    public void setGradientColor(@Size(min = 3) int[] colors){
        this.gradientColor = colors;
        syncDraw();
    }

    public void setOrientation(@IntRange(from = 0, to = 1)int orientation){
        this.orientation = orientation;
        syncDraw();
    }

    private void checkAndCompensateEndpoint(){
        ControlPoint lastPoint = controlPoints.get(controlPoints.size() - 1);
        if(lastPoint.getX() != 1f || lastPoint.getY() != 0 && lastPoint.getY() != 1f) {
            ControlPoint previousPoint = controlPoints.get(controlPoints.size() - 2);
            float m = (float)(lastPoint.getY() - previousPoint.getY()) / (float)(lastPoint.getX() - previousPoint.getX());
            float y = m * (1 - previousPoint.getX()) + previousPoint.getY();
            controlPoints.add(new ControlPoint(1f, y));
        }
    }

    private void resetCurvPoints() {
        if (!isShown())
            return;
        int[] result = new int[viewWidth * viewHeight];
        setup(controlPoints.toArray(), orientation, viewWidth, viewHeight, gradientColor, result);
        gradientBmp = Bitmap.createBitmap(result, viewWidth, viewHeight, Bitmap.Config.ARGB_8888);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        viewWidth = getMeasuredWidth();
        viewHeight = getMeasuredHeight();
        syncDraw();
    }

    private Handler handler = new Handler(Looper.myLooper());

    private Runnable runnable = () -> {
        redraw();
    };

    private void syncDraw(){
        handler.removeCallbacks(runnable);
        handler.post(runnable);
    }

    private synchronized void redraw(){
        if(controlPoints.size() > 0 && viewWidth > 0 && viewHeight > 0 && isShown()) {
            checkAndCompensateEndpoint();
            resetCurvPoints();
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(gradientBmp != null && !gradientBmp.isRecycled())
            canvas.drawBitmap(gradientBmp, 0, 0, paint);
    }


}