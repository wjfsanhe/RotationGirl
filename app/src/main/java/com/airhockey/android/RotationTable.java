package com.airhockey.android;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import framework.com.tools.math.MathUtils;
import framework.com.tools.math.Matrix4;
import framework.com.tools.math.Quaternion;
import framework.com.tools.math.Vector3;
import framework.com.tools.util.RotationUpdateDelegate;
import framework.com.tools.util.RotationVectorListener;

public class RotationTable extends Activity implements RotationUpdateDelegate{
    /**
     * Hold a reference to our GLSurfaceView
     */
    private final String TAG = "RotationTable";
    private final String DEBUG_TAG = "RotationTable";
    private GLSurfaceView glSurfaceView;
    private boolean rendererSet = false;
    private RotationVectorListener mRotationVector;
    private Matrix4 mRotationMatrixOffset = new Matrix4();
    float mRotateRads = 0f;
    private int mDisplayRotation;
    private RotationTableRender mRender;
    private SensorManager mSensorManager;
    private boolean mNeedRecenter = true;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRotationVector = new RotationVectorListener(this);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        final Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        mDisplayRotation = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) ? display.getRotation() : display.getOrientation();
        glSurfaceView = new GLSurfaceView(this);


        // Request an OpenGL ES 2.0 compatible context.
        glSurfaceView.setEGLContextClientVersion(2);

        // Assign our renderer.
        mRender = new RotationTableRender(this);
        glSurfaceView.setRenderer(mRender);
        rendererSet = true;

        setContentView(glSurfaceView);
    }
    private void updateCamera(float[] matrix){
        //set camera matrix
        mRender.updateCameraPosition(matrix);
    }


    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(mRotationVector);
        if (rendererSet) {
            glSurfaceView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mNeedRecenter = true;
        mSensorManager.registerListener(mRotationVector, mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_FASTEST);
        if (rendererSet) {
            glSurfaceView.onResume();
        }
    }

    @Override
    public void onRotationUpdate(float[] newMatrix, float[] newQuat) {
        // remap matrix values according to display rotation, as in
        // SensorManager documentation.
        //para newQuat order[0-3] :  w, x, y, z.
        //Quaternion order : x, y, z, w
        Quaternion curQuat = new Quaternion(newQuat[1], newQuat[2],newQuat[3], newQuat[0]);
        switch (mDisplayRotation) {
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                break;
            case Surface.ROTATION_90:
                SensorManager.remapCoordinateSystem(newMatrix, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, newMatrix);
                curQuat.mul(new Quaternion(new Vector3(0f,0f,1f),-90f));

                break;
            case Surface.ROTATION_270:
                SensorManager.remapCoordinateSystem(newMatrix, SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X, newMatrix);
                curQuat.mul(new Quaternion(new Vector3(0f,0f,1f),90f));
                break;
            default:
                break;
        }
        curQuat = new Quaternion(-curQuat.x, -curQuat.y,-curQuat.z, curQuat.w);

        if(true) {
            float[] displayMatrix = new float[16];
            curQuat.toMatrix(displayMatrix);
            Quaternion displayQuat = new Quaternion().setFromMatrix(new Matrix4(newMatrix));
            Log.d("WJF", " inputMatrix " + newMatrix[0] + ", " + newMatrix[5] + ", " +
                    newMatrix[10] + ", " + newMatrix[15]);
            Log.d("WJF", " displMatrix " + displayMatrix[0] + ", " + displayMatrix[5] + ", " +
                    displayMatrix[10] + ", " + displayMatrix[15]);
            Log.d("DEFG", " displQuat " + displayQuat.toString());
            Log.d("DEFG", " inputQuat " + curQuat.toString());

        }
        //rotate 90 degree with axisX;
        curQuat = curQuat.mul(new Quaternion(new Vector3(1f,0f,0f),90f));
        new Quaternion().setFromMatrix(new Matrix4(newMatrix))
                .mul(new Quaternion(new Vector3(1f,0f,0f),90f))
                .toMatrix(newMatrix);


        if (mNeedRecenter) {

            float[] IMatrix= new float[16];
            Quaternion IQuat = new Quaternion(0f,0f,0f,1);
            Vector3 Forward = new Vector3(0f,0f,1f);

            Log.d("WJF"," quat " + curQuat.toString());

            curQuat.transform(Forward);
            mRotateRads = MathUtils.atan2(Forward.x, Forward.z);
            mRotateRads *= 180f/Math.PI;
            IQuat.toMatrix(IMatrix);
            mRotationMatrixOffset = new Matrix4(IMatrix);
            mRotationMatrixOffset = mRotationMatrixOffset.rotate(new Vector3(0f,1f,0f),-mRotateRads);
            //Log.d("WJF","rotateRads " + -RotateRads + " sin(angle) "+ MathUtils.sin(-RotateRads) +
            //                                "Look dir " +  Forward.toString());
            Log.d("WJF","Look dir " +  Forward.toString());
            mNeedRecenter = false;
        }

        if(false) {
            float[] tmpMatrix = new float[16];
            curQuat = curQuat.mul(new Quaternion(new Vector3(0f, 1f, 0f), -mRotateRads));
            curQuat.toMatrix(tmpMatrix);
            updateCamera(tmpMatrix);
        } else {
            new Quaternion().setFromMatrix(new Matrix4(newMatrix))
                    .mul(new Quaternion(new Vector3(0f,1f,0f),-mRotateRads))
                    .toMatrix(newMatrix);
            updateCamera(newMatrix);
        }
    }

}