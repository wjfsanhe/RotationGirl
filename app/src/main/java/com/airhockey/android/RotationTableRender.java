package com.airhockey.android;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_LINES;
import static android.opengl.GLES20.GL_POINTS;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TRIANGLE_FAN;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;
import static android.opengl.Matrix.multiplyMM;
import static android.opengl.Matrix.rotateM;
import static android.opengl.Matrix.setIdentityM;
import static android.opengl.Matrix.translateM;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;
import android.os.Handler;
import android.util.Log;

import framework.com.tools.math.Matrix4;
import framework.com.tools.util.LoggerConfig;
import framework.com.tools.util.MatrixHelper;
import framework.com.tools.util.ShaderHelper;
import framework.com.tools.util.TextResourceReader;
import framework.com.tools.util.TextureHelper;

public class RotationTableRender implements Renderer {
    private static final String U_MATRIX = "u_Matrix";
    private static final String A_POSITION = "a_Position";
    private static final String A_TEXTURE = "a_TextureCoordinates";
    private static final String A_TEXTURE_UNIT = "a_TextureUnit";

    private static final int POSITION_COMPONENT_COUNT = 2;
               
    private static final int UV_COMPONENT_COUNT = 2;
    
    private static final int BYTES_PER_FLOAT = 4;
    
    private static final int STRIDE = 
        (POSITION_COMPONENT_COUNT + UV_COMPONENT_COUNT) * BYTES_PER_FLOAT;
    
    private final FloatBuffer vertexData;
    private final Context context;
    
    private final float[] projectionMatrix = new float[16];
    private final float[] cameraMatrix = new float[16];

    
    private final float[] modelMatrix = new float[16];

    private int program;
    private int uMatrixLocation;
    private int aPositionLocation;
    private int aColorLocation;
    private int aTextureLocation;
    private int aTextureUnit;
    private float modelAngle = 0f;
    private int mWidth = 0;
    private int mHeight = 0;
    private int mTexture = 0;

    public RotationTableRender(Context context) {
        this.context = context;

        

        float[] tableVerticesWithTriangles = {   
                // Triangle Fan
                0f, 0f, 0.5f,0.5f,
                -0.5f, -0.8f, 0f, 1f,
                0.5f, -0.8f, 1f, 1f,
                0.5f, 0.8f, 1f, 0f,
                -0.5f, 0.8f, 0f, 0f,
                -0.5f, -0.8f, 0f, 1f
        };        

        vertexData = ByteBuffer
            .allocateDirect(tableVerticesWithTriangles.length * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();

        vertexData.put(tableVerticesWithTriangles);

    }
    public boolean updateCameraPosition(float[] matrix){
        System.arraycopy(matrix,0,cameraMatrix,0,cameraMatrix.length);

        updateSceneMatrix();
        return true;
    }
    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        String vertexShaderSource = TextResourceReader
            .readTextFileFromResource(context, R.raw.simple_vertex_shader);
        String fragmentShaderSource = TextResourceReader
            .readTextFileFromResource(context, R.raw.simple_fragment_shader);

        int vertexShader = ShaderHelper.compileVertexShader(vertexShaderSource);
        int fragmentShader = ShaderHelper
            .compileFragmentShader(fragmentShaderSource);

        program = ShaderHelper.linkProgram(vertexShader, fragmentShader);

        if (LoggerConfig.ON) {
            ShaderHelper.validateProgram(program);
        }

        glUseProgram(program);
        mTexture = TextureHelper.loadTexture(context,R.drawable.girl); //load texture can't be too early.

        uMatrixLocation = glGetUniformLocation(program, U_MATRIX);
        aTextureUnit = glGetUniformLocation(program, A_TEXTURE_UNIT);

        aPositionLocation = glGetAttribLocation(program, A_POSITION);
        aTextureLocation = glGetAttribLocation(program, A_TEXTURE);

        // Bind our data, specified by the variable vertexData, to the vertex
        // attribute at location A_POSITION_LOCATION.
        vertexData.position(0);
        glVertexAttribPointer(aPositionLocation,
            POSITION_COMPONENT_COUNT, GL_FLOAT, false, STRIDE, vertexData);

        glEnableVertexAttribArray(aPositionLocation);     
                
        // Bind our data, specified by the variable vertexData, to the vertex
        // attribute at location A_COLOR_LOCATION.
        vertexData.position(POSITION_COMPONENT_COUNT);        
        glVertexAttribPointer(aTextureLocation,
                UV_COMPONENT_COUNT, GL_FLOAT, false, STRIDE, vertexData);

        glEnableVertexAttribArray(aTextureLocation);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D,mTexture);
        glUniform1i(aTextureUnit,0);
    }

    void  updateSceneMatrix(){

        float[] tmpProjectionMatrix = new float[16];

        MatrixHelper.perspectiveM(tmpProjectionMatrix, 45, (float) mWidth/2
                / (float) mHeight, 1f, 10f);

        /*
        setIdentityM(modelMatrix, 0);
        translateM(modelMatrix, 0, 0f, 0f, -2f);
        */

        setIdentityM(modelMatrix, 0);

        translateM(modelMatrix, 0, 0f, 0f, -6.5f);
        rotateM(modelMatrix, 0, modelAngle, 1f, 0f, 0f);
        //rotateM(modelMatrix, 0, -90f, 0f, 0f, 1f);

        //rotateM(cameraMatrix, 0, 180f, 1f, 0f, 0f);

        final float[] temp = new float[16];
        multiplyMM(temp, 0, cameraMatrix, 0, modelMatrix, 0);
        System.arraycopy(temp, 0, cameraMatrix, 0, temp.length);
        multiplyMM(temp, 0, tmpProjectionMatrix, 0, cameraMatrix, 0);
        System.arraycopy(temp, 0, projectionMatrix, 0, temp.length);
    }
    /**
     * onSurfaceChanged is called whenever the surface has changed. This is
     * called at least once when the surface is initialized. Keep in mind that
     * Android normally restarts an Activity on rotation, and in that case, the
     * renderer will be destroyed and a new one created.
     * 
     * @param width
     *            The new width, in pixels.
     * @param height
     *            The new height, in pixels.
     */
    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        // Set the OpenGL viewport to fill the entire surface.
        glViewport(0, 0, width, height);
        mWidth = width;
        mHeight = height;
        setIdentityM(cameraMatrix,0);
        updateSceneMatrix();
        Log.d("WJF", "surface changed");
    }

    /**
     * OnDrawFrame is called whenever a new frame needs to be drawn. Normally,
     * this is done at the refresh rate of the screen.
     */
    @Override
    public void onDrawFrame(GL10 glUnused) {
        // Clear the rendering surface.
        //glViewport(0, 0, mWidth, mHeight);
        glClear(GL_COLOR_BUFFER_BIT);
                        
        // Assign the matrix
        glUniformMatrix4fv(uMatrixLocation, 1, false, projectionMatrix, 0);

        // Draw the table.
        glViewport(0, 0, mWidth/2, mHeight);
        glDrawArrays(GL_TRIANGLE_FAN, 0, 6);

        glViewport(mWidth/2, 0, mWidth/2, mHeight);
        glDrawArrays(GL_TRIANGLE_FAN, 0, 6);

    }
}