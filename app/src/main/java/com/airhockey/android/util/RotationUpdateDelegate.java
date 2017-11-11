package com.airhockey.android.util;

/**
 * Created by wangjf on 17-11-11.
 */

public interface RotationUpdateDelegate {
    /**
     * @param newMatrix - 4x4 matrix
     */
    public void onRotationUpdate(float newMatrix[]);
}
