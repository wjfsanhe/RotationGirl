package com.airhockey.android.util;


import android.annotation.SuppressLint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * Created by wangjf on 17-11-11.
 */

public class RotationVectorListener implements SensorEventListener {
    private float[] mRotationM = new float[16];
    private RotationUpdateDelegate mRotationUpdateDelegate;

    public RotationVectorListener(RotationUpdateDelegate rotationUpdateDelegate) {
        mRotationUpdateDelegate = rotationUpdateDelegate;
    }

    @SuppressLint("NewApi")
    @Override
    public void onSensorChanged(SensorEvent event) {
        SensorManager.getRotationMatrixFromVector(mRotationM, event.values);
        Log.d("WJF","rotation changed " + mRotationM.toString());
        mRotationUpdateDelegate.onRotationUpdate(mRotationM);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
