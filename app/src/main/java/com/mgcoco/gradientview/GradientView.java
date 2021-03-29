package com.mgcoco.gradientview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.IntRange;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class GradientView extends View {

    private ArrayList<ControlPoint> controlPoints = new ArrayList<>();

    private ArrayList<Point> tmpCurvPoints = new ArrayList<>();

    private ArrayList<Point> curvPoints = new ArrayList<>();

    private int viewWidth;

    private int viewHeight;

    public static final int HORIZONTAL = 0;

    public static final int VERTICAL = 1;

    private int orientation = VERTICAL;

    private int[] gradientColor = new int[]{Color.GRAY, Color.WHITE, Color.BLACK};

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
        }
    }

    public void setControlPoints(ArrayList<ControlPoint> points){
        controlPoints = points;
        if(orientation == VERTICAL){
            Collections.sort(controlPoints, (Comparator<ControlPoint>) (o1, o2) -> (int)(o1.getX() - o2.getX()));
        }
        else{
            Collections.sort(controlPoints, (Comparator<ControlPoint>) (o1, o2) -> (int)(o1.getY() - o2.getY()));
        }
        redraw();
    }

    public void getGradientColor(@Size(min=3) int[] colors){
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
        curvPoints.clear();
        tmpCurvPoints.clear();
        for (int i = 1; i < controlPoints.size(); i++) {
            if(i == 1) {
                setupCurvPoint(controlPoints.get(i), controlPoints.get(i - 1), controlPoints.get(i - 1));
            }
            else {
                setupCurvPoint(controlPoints.get(i), controlPoints.get(i - 1), controlPoints.get(i - 2));
            }
        }

        for (int i = 1; i < tmpCurvPoints.size(); i++) {
            Point pStart = tmpCurvPoints.get(i - 1);
            Point pEnd = tmpCurvPoints.get(i);
            curvPoints.add(new Point(pStart.x, pStart.y));
            float m = (float) (pEnd.y - pStart.y) / (float) (pEnd.x - pStart.x);

            if(orientation == VERTICAL){
                int oldX = pStart.x;
                if (pEnd.x - pStart.x > 1) {
                    for (int gi = pStart.x + 1; gi < pEnd.x; gi++) {
                        float y = m * (gi - pStart.x) + pStart.y;
                        if(gi > oldX) {
                            curvPoints.add(new Point(gi, (int) y));
                            oldX = gi;
                        }
                    }
                }
            }
            else{
                int oldY = pStart.y;
                if (pEnd.y - pStart.y > 1) {
                    for (int gi = pStart.y + 1; gi < pEnd.y; gi++) {
                        float x = ((gi - pEnd.y) / m) + pEnd.x;
                        if(gi > oldY) {
                            curvPoints.add(new Point((int)x, gi));
                            oldY = gi;
                        }
                    }
                }
            }
        }

        curvPoints.add(new Point(tmpCurvPoints.get(tmpCurvPoints.size() - 1).x, tmpCurvPoints.get(tmpCurvPoints.size() - 1).y));

        if(orientation == VERTICAL){
            int startX = curvPoints.get(0).x;
            if(startX > 0) {
                int y = curvPoints.get(0).y;
                for(int i = startX - 1; i >= 0; i--) {
                    curvPoints.add(0, new Point(i, y));
                }
            }

            int lastX = curvPoints.get(curvPoints.size() - 1).x;
            if(viewWidth - lastX > 0) {
                int y = curvPoints.get(curvPoints.size() - 1).y;
                for(int i = lastX + 1; i <= viewWidth; i++) {
                    curvPoints.add(new Point(i, y));
                }
            }
        }
        else{
            int startY = curvPoints.get(0).y;
            if(startY > 0) {
                int x = curvPoints.get(0).x;
                for(int i = startY - 1; i >= 0; i--) {
                    curvPoints.add(0, new Point(x, i));
                }
            }

            int lastY = curvPoints.get(curvPoints.size() - 1).y;
            if(viewHeight - lastY > 0) {
                int x = curvPoints.get(curvPoints.size() - 1).x;
                for(int i = lastY + 1; i <= viewHeight; i++) {
                    curvPoints.add(new Point(x, i));
                }
            }
        }
    }

    private void setupCurvPoint(ControlPoint cp2, ControlPoint cp1, ControlPoint cp){
        Point p2 = new Point((int)(cp2.getX() * viewWidth), (int)(cp2.getY() * viewHeight));
        Point p1 = new Point((int)(cp1.getX() * viewWidth), (int)(cp1.getY() * viewHeight));
        Point p = new Point((int)(cp.getX() * viewWidth), (int)(cp.getY() * viewHeight));

        if(orientation == VERTICAL){
            int oldX = -1;
            if(tmpCurvPoints.size() > 0){
                oldX = tmpCurvPoints.get(tmpCurvPoints.size() - 1).x;
            }
            for(float t = (float) 0.24; t <= 0.76; t = t + (float) (0.02)) {
                float deltaX = p.x * (1 - t) * (1 - t) * (1 - t) + 3 * p1.x * t * (1 - t) * (1 - t) + 3 * p1.x * t * t * (1 - t) + p2.x * t * t * t;
                float deltaY = p.y * (1 - t) * (1 - t) * (1 - t) + 3 * p1.y * t * (1 - t) * (1 - t) + 3 * p1.y * t * t * (1 - t) + p2.y * t * t * t;
                if(oldX != (int)deltaX) {
                    tmpCurvPoints.add(new Point((int) deltaX, (int) deltaY));
                    oldX = (int)deltaX;
                }
            }
        }
        else{
            int oldY = -1;
            if(tmpCurvPoints.size() > 0){
                oldY = tmpCurvPoints.get(tmpCurvPoints.size() - 1).y;
            }
            for(float t = (float) 0.24; t <= 0.76; t = t + (float) (0.02)) {
                float deltaX = p.x * (1 - t) * (1 - t) * (1 - t) + 3 * p1.x * t * (1 - t) * (1 - t) + 3 * p1.x * t * t * (1 - t) + p2.x * t * t * t;
                float deltaY = p.y * (1 - t) * (1 - t) * (1 - t) + 3 * p1.y * t * (1 - t) * (1 - t) + 3 * p1.y * t * t * (1 - t) + p2.y * t * t * t;
                if(oldY != (int)deltaY) {
                    tmpCurvPoints.add(new Point((int) deltaX, (int) deltaY));
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
        if(controlPoints.size() > 0 && viewWidth > 0 && viewHeight > 0) {
            checkAndCompensateEndpoint();
            resetCurvPoints();
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint paint = new Paint();

        if(curvPoints.size() > 0) {
            if(orientation == VERTICAL){
                for (Point p : curvPoints) {
                    float key = ((float) (p.y) / viewHeight);
                    float[] position = {0f, key, 1.0f};
                    LinearGradient linearGradient = new LinearGradient(0, 0, 0, viewHeight, gradientColor, position, Shader.TileMode.CLAMP);
                    paint.setShader(linearGradient);
                    canvas.drawLine(p.x, 0, p.x, viewHeight, paint);
                }
            }
            else{
                for (Point p : curvPoints) {
                    float key = ((float) (p.x) / viewWidth);
                    float[] position = {0f, key , 1.0f};
                    LinearGradient linearGradient = new LinearGradient(0, 0, viewWidth, 0, gradientColor, position, Shader.TileMode.CLAMP);
                    paint.setShader(linearGradient);
                    canvas.drawLine(0, p.y, viewWidth, p.y, paint);
                }
            }
        }
    }
}
