package com.mgcoco.gradientview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.IntRange;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public class GradientView extends View {

    private ArrayList<ControlPoint> controlPoints = new ArrayList<>();

    private ArrayList<Integer> tmpX = new ArrayList<>();

    private ArrayList<Integer> tmpY = new ArrayList<>();

    private int[] fullPoints = new int[]{};

    private LinearGradient[] linearGradients = new LinearGradient[]{};

    private int viewWidth;

    private int viewHeight;

    public static final int HORIZONTAL = 0;

    public static final int VERTICAL = 1;

    private int orientation = VERTICAL;

    private int[] gradientColor = new int[]{Color.GRAY, Color.WHITE, Color.BLACK};

    private float[] colorPosition = {0f, 0.5f, 1.0f};

    private Paint paint = new Paint();

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
        redraw();
    }

    public void setGradientColor(@Size(min = 3) int[] colors){
        this.gradientColor = colors;
        redraw();
    }

    public void setOrientation(@IntRange(from = 0, to = 1)int orientation){
        this.orientation = orientation;
        redraw();
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

    private void resetCurvPoints(){
        if(orientation == VERTICAL){
            fullPoints = new int[viewWidth];
            linearGradients = new LinearGradient[viewWidth];
            Arrays.fill(linearGradients, new LinearGradient(0, 0, 0, viewHeight, gradientColor, colorPosition, Shader.TileMode.CLAMP));
        }
        else{
            fullPoints = new int[viewHeight];
            linearGradients = new LinearGradient[viewHeight];
            Arrays.fill(linearGradients, new LinearGradient(0, 0, viewWidth, 0, gradientColor, colorPosition, Shader.TileMode.CLAMP));
        }
        Arrays.fill(fullPoints, -1);



        for (int i = 1; i < controlPoints.size(); i++) {
            if(i == 1) {
                setupCurvPoint(controlPoints.get(i), controlPoints.get(i - 1), controlPoints.get(i - 1));
            }
            else {
                setupCurvPoint(controlPoints.get(i), controlPoints.get(i - 1), controlPoints.get(i - 2));
            }
        }

        for (int i = 1; i < tmpX.size(); i++) {
            int startX = tmpX.get(i - 1);
            int startY = tmpY.get(i - 1);

            int endX = tmpX.get(i);
            int endY = tmpY.get(i);
            float m = (float) (endY - startY) / (float)(endX - startX);

            if(orientation == VERTICAL){
                if(startX < fullPoints.length) {
                    fullPoints[startX] = startY;
                    colorPosition[1] = ((float) (fullPoints[startX]) / viewHeight);

                    int oldX = startX;
                    if (endX - startX > 1) {
                        for (int gi = startX + 1; gi < endX; gi++) {
                            float y = m * (gi - startX) + startY;
                            if (gi > oldX) {
                                fullPoints[gi] = (int) y;
                                colorPosition[1] = ((float) (fullPoints[gi]) / viewHeight);
                                oldX = gi;
                            }
                        }
                    }
                }
            }
            else{
                if(startY < fullPoints.length) {
                    int oldY = startY;
                    fullPoints[startY] = startX;
                    colorPosition[1] = ((float) (fullPoints[startY]) / viewWidth);

                    if (endY - startY > 1) {
                        for (int gi = startY + 1; gi < endY; gi++) {
                            float x = ((gi - endY) / m) + endX;
                            if (gi > oldY) {
                                fullPoints[gi] = (int) x;
                                colorPosition[1] = ((float) (fullPoints[gi]) / viewWidth);
                                oldY = gi;
                            }
                        }
                    }
                }
            }
        }

        int endX = tmpX.get(tmpX.size() - 1);
        int endY = tmpY.get(tmpY.size() - 1);
        if(orientation == VERTICAL){
            int startX = fullPoints[0];
            if(startX == -1) {
                int y = tmpY.get(0);
                for(int i = startX - 1; i >= 0; i--) {
                    fullPoints[i] = y;
                    colorPosition[1] = ((float) (fullPoints[i]) / viewHeight);
                }
            }

            int lastX = fullPoints[fullPoints.length - 1];
            if(lastX == -1) {
                for(int i = endX; i < viewWidth; i++) {
                    fullPoints[i] = endY;
                    colorPosition[1] = ((float) (fullPoints[i]) / viewHeight);
                }
            }
        }
        else{
            int startY = fullPoints[0];
            if(startY == -1) {
                int x = tmpX.get(0);
                for(int i = startY - 1; i >= 0; i--) {
                    fullPoints[i] = x;
                    colorPosition[1] = ((float) (fullPoints[i]) / viewWidth);
                }
            }

            int lastY = fullPoints[fullPoints.length - 1];
            if(lastY == -1) {
                for(int i = endY; i < viewHeight; i++) {
                    fullPoints[i] = endX;
                    colorPosition[1] = ((float) (fullPoints[i]) / viewWidth);
                }
            }
        }
    }

    private void setupCurvPoint(ControlPoint cp2, ControlPoint cp1, ControlPoint cp){
        int p2x = (int)(cp2.getX() * viewWidth);
        int p2y = (int)(cp2.getY() * viewHeight);

        int p1x = (int)(cp1.getX() * viewWidth);
        int p1y = (int)(cp1.getY() * viewHeight);

        int px = (int)(cp.getX() * viewWidth);
        int py = (int)(cp.getY() * viewHeight);

        if(orientation == VERTICAL){
            int oldX = -1;
            if(tmpX.size() > 0){
                oldX = tmpX.get(tmpX.size() - 1);
            }
            for(float t = (float) 0.24; t <= 0.76; t = t + (float) (0.02)) {
                int deltaX = (int) (px * (1 - t) * (1 - t) * (1 - t) + 3 * p1x * t * (1 - t) * (1 - t) + 3 * p1x * t * t * (1 - t) + p2x * t * t * t);
                int deltaY = (int) (py * (1 - t) * (1 - t) * (1 - t) + 3 * p1y * t * (1 - t) * (1 - t) + 3 * p1y * t * t * (1 - t) + p2y * t * t * t);
                if(oldX != deltaX && deltaX < viewWidth) {
                    tmpX.add(deltaX);
                    tmpY.add(deltaY);
                    oldX = (int)deltaX;
                }
            }
        }
        else{
            int oldY = -1;
            if(tmpY.size() > 0){
                oldY = tmpY.get(tmpY.size() - 1);
            }
            for(float t = (float) 0.24; t <= 0.76; t = t + (float) (0.02)) {
                int deltaX = (int) (px * (1 - t) * (1 - t) * (1 - t) + 3 * p1x * t * (1 - t) * (1 - t) + 3 * p1x * t * t * (1 - t) + p2x * t * t * t);
                int deltaY = (int) (py * (1 - t) * (1 - t) * (1 - t) + 3 * p1y * t * (1 - t) * (1 - t) + 3 * p1y * t * t * (1 - t) + p2y * t * t * t);
                if(deltaY > oldY && deltaY < viewHeight) {
                    tmpX.add(deltaX);
                    tmpY.add(deltaY);
                    oldY = (int)deltaY;
                }
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        viewWidth = getMeasuredWidth();
        viewHeight = getMeasuredHeight();
        redraw();
    }

    private void redraw(){
        if(controlPoints.size() > 0 && viewWidth > 0 && viewHeight > 0 && isShown()) {
            checkAndCompensateEndpoint();
            resetCurvPoints();
            invalidate();
        }
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(orientation == VERTICAL){
            for(int p = 0; p < fullPoints.length; p++){
                paint.setShader(linearGradients[p]);
                canvas.drawLine(p, 0, p, viewHeight, paint);
            }
        }
        else{
            for(int p = 0; p < fullPoints.length; p++){
                paint.setShader(linearGradients[p]);
                canvas.drawLine(0, p, viewWidth, p, paint);
            }
        }
    }
}