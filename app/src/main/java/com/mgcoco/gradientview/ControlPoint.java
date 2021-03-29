package com.mgcoco.gradientview;


import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.FloatRange;

public class ControlPoint implements Parcelable {

    @FloatRange(from = 0, to = 1)
    private float x;

    @FloatRange(from = 0, to = 1)
    private float y;

    public ControlPoint(@FloatRange(from = 0, to = 1) float x, @FloatRange(from = 0, to = 1)float y) {
        this.x = x;
        this.y = y;
    }

    public void readFromParcel(Parcel source) {
        this.x = source.readFloat();
        this.y = source.readFloat();
    }

    public ControlPoint() {
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(this.x);
        dest.writeFloat(this.y);
    }


    protected ControlPoint(Parcel in) {
        this.x = in.readFloat();
        this.y = in.readFloat();
    }

    public static final Creator<ControlPoint> CREATOR = new Creator<ControlPoint>() {
        @Override
        public ControlPoint createFromParcel(Parcel source) {
            return new ControlPoint(source);
        }

        @Override
        public ControlPoint[] newArray(int size) {
            return new ControlPoint[size];
        }
    };
}
