package com.yunxi.camera1preview;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity implements Camera.PreviewCallback {

    private static final String TAG = "YX_Main";
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    @SuppressWarnings("deprecation")
    private Camera.Size mPreviewSize;
    @SuppressWarnings("deprecation")
    private Camera mCamera;
    private byte[] PreviewBuffer = null;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void initView() {
        mSurfaceView = findViewById(R.id.surface);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(new SurfaceHolderCallback());
    }

    /**
     * SurfaceView Holder 回调, {@link SurfaceHolder.Callback}的实现类
     */
    private class SurfaceHolderCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            openCamera();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            startCamera();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            closeCamera();
        }
    }


    @SuppressWarnings("deprecation")
    private void closeCamera() {
        if (null != mCamera) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    @SuppressWarnings("deprecation")
    private void startCamera() {
        if (null != mCamera) {
            mCamera.startPreview();
        }
    }

    @SuppressWarnings("deprecation")
    private void openCamera() {
        int numberOfCams = Camera.getNumberOfCameras();
        if (numberOfCams > 0) {
            Log.d(TAG, "发现相机数量: " + numberOfCams);
        } else {
            Log.d(TAG, "相机设备不存在");
        }
        try {
            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        } catch (Exception e) {
            Log.d(TAG, "打开相机异常: " + e.getMessage());
        }
        if (null != mCamera) {
            CameraSizeComparator comparator = new CameraSizeComparator();
            List<Camera.Size> picSizes = mCamera.getParameters().getSupportedPictureSizes();
            Collections.sort(picSizes, comparator);
            List<Camera.Size> preSizes = mCamera.getParameters().getSupportedPreviewSizes();
            Collections.sort(preSizes, comparator);
            for (Camera.Size s: preSizes) {
                Log.d(TAG, "预览: "  + s.width + " x " + s.height);
            }
            Camera.Size minPictureSize = picSizes.get(0);
            // 1920 * 1080
            mPreviewSize = preSizes.get(preSizes.size() - 1);
            // 144 * 176
            //Camera.Size maxPreviewSize = preSizes.get(0);
            Log.d(TAG, "最小拍照尺寸: " + minPictureSize.width + "x" + minPictureSize.height);
            Log.d(TAG, "最大预览尺寸: " + mPreviewSize.width + "x" + mPreviewSize.height);
            Camera.Parameters param = mCamera.getParameters();
            //param.setRotation(0);

            param.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
            // 初始化YUV数据转换器
            //NV21toBitmap.init(this, mPreviewSize.width, mPreviewSize.height);
            param.setPreviewFormat(ImageFormat.NV21);
            param.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            mCamera.setParameters(param);

            int size = mPreviewSize.width * mPreviewSize.height * ImageFormat.getBitsPerPixel(ImageFormat.NV21) / 8;
            PreviewBuffer = new byte[size];
            mCamera.addCallbackBuffer(PreviewBuffer);
            mCamera.setPreviewCallbackWithBuffer(this);
            try {
                mCamera.setPreviewDisplay(mSurfaceHolder);
            } catch (IOException e) {
                Log.d(TAG, "摄像头预览失败: " + e.getMessage());
            }
        }
    }

    private long lastRenderTime = System.currentTimeMillis();
    private int frameCounter = 0;

    @SuppressWarnings("deprecation")
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        mCamera.addCallbackBuffer(data);
        // 调试模式开启FPS log
        if (BuildConfig.DEBUG) {
            if (System.currentTimeMillis() - lastRenderTime > 10000) {
                lastRenderTime = System.currentTimeMillis();
                Log.d(TAG, "FPS: " + frameCounter / 10.0f);
                frameCounter = 0;
            }
            frameCounter++;
        }
    }

    //camera size 升序
    @SuppressWarnings("deprecation")
    private class CameraSizeComparator implements Comparator<Camera.Size> {
        @Override
        public int compare(Camera.Size l, Camera.Size r) {
            if (l.width == r.width && l.height == r.height) {
                return 0;
            } else if (l.width * l.height > r.width * l.height) {
                return 1;
            } else {
                return -1;
            }
        }
    }
}
