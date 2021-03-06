package com.mcivicm.media.helper;

import android.Manifest;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.view.Surface;

import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;

/**
 * 摄像头助手
 */

public class CameraOneHelper {
    /**
     * 存储权限
     *
     * @param activity
     * @return
     */
    public static Observable<Boolean> storagePermission(final Activity activity) {
        return new RxPermissions(activity)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .flatMap(new Function<Boolean, ObservableSource<Boolean>>() {
                    @Override
                    public ObservableSource<Boolean> apply(Boolean aBoolean) throws Exception {
                        if (aBoolean) {
                            return Observable.just(true);
                        } else {
                            return new RxPermissions(activity)
                                    .shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    .flatMap(new Function<Boolean, ObservableSource<Boolean>>() {
                                        @Override
                                        public ObservableSource<Boolean> apply(Boolean aBoolean) throws Exception {
                                            if (aBoolean) {//仅禁止
                                                return Observable.error(new Exception("亲，您需要授权【读写】权限才能打开摄像头哦"));
                                            } else {//禁止并且不再提醒
                                                return Observable.error(new Exception("亲，您拒绝了【读写】权限并且决定不再提醒，如需重新开启【读写】权限，请到【设置】-【权限管理】中手动授权"));
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    /**
     * 获取摄像头的权限
     *
     * @param activity
     * @return
     */
    public static Observable<Boolean> cameraPermission(final Activity activity) {
        return new RxPermissions(activity)
                .request(Manifest.permission.CAMERA)
                .flatMap(new Function<Boolean, ObservableSource<Boolean>>() {
                    @Override
                    public ObservableSource<Boolean> apply(Boolean aBoolean) throws Exception {
                        if (aBoolean) {
                            return Observable.just(true);
                        } else {
                            return new RxPermissions(activity)
                                    .shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
                                    .flatMap(new Function<Boolean, ObservableSource<Boolean>>() {
                                        @Override
                                        public ObservableSource<Boolean> apply(Boolean aBoolean) throws Exception {
                                            if (aBoolean) {
                                                return Observable.error(new Exception("您需要授权摄像头权限才能开始录音"));
                                            } else {
                                                return Observable.error(new Exception("您已拒绝了摄像头权限并选择不再提醒，如需重新打开摄像头权限，请在设置->权限里面打开"));
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    /**
     * @return 摄像头的总数
     */
    public static int cameraNumber() {
        return Camera.getNumberOfCameras();
    }

    /**
     * 获取摄像头的信息
     *
     * @param id
     * @return
     */
    public static Camera.CameraInfo getInfo(int id) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(id, info);
        return info;
    }

    /**
     * 打开编号为id的摄像头
     *
     * @param id
     * @return
     */
    public static Camera open(int id) {
        return Camera.open(id);
    }

    /**
     * 设置拍照参数
     *
     * @param camera
     */
    public static void pictureSetting(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        camera.setParameters(parameters);
    }

    /**
     * 设置录像参数
     *
     * @param camera
     */
    public static void videoSetting(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);//连续聚焦
        parameters.setRecordingHint(true);//可能有风险
        if (parameters.isVideoStabilizationSupported()) {
            parameters.setVideoStabilization(true);
        }
        camera.setParameters(parameters);
    }

    /**
     * 设置预览的旋转角度
     *
     * @param camera
     * @param orientation
     */
    public static void setPreviewOrientation(Camera camera, int orientation) {
        camera.setDisplayOrientation(orientation);//预览的旋转角度
    }

    /**
     * 设置图片旋转角
     *
     * @param camera
     * @param rotation
     */
    public static void setPictureRotation(Camera camera, int rotation) {
        Camera.Parameters parameters = camera.getParameters();
        parameters.setRotation(rotation);
        camera.setParameters(parameters);
    }

    /**
     * 适配屏幕和相机
     *
     * @param camera
     * @param width  大者
     * @param height 小者
     */
    public static void setPreviewAndPictureResolution(Camera camera, int width, int height) {
        if (width == 0 || height == 0) {//提出杂质
            return;
        }
        Camera.Parameters rawParameters = camera.getParameters();

        //查找【最大预览分辨率】并设置
        Camera.Size bestPreviewSize = findMaxSize(
                rawParameters.getSupportedPreviewSizes()
        );
        if (bestPreviewSize != null) {
            rawParameters.setPreviewSize(bestPreviewSize.width, bestPreviewSize.height);
        }
        //查找【最佳图片分辨率】并设置，不用最大分辨率是为了节省空间
        Camera.Size bestPictureSize = findEnoughSize(
                rawParameters.getSupportedPictureSizes(),
                width,
                height
        );
        if (bestPictureSize != null) {
            rawParameters.setPictureSize(bestPictureSize.width, bestPictureSize.height);
        }
        //最后一定记得重新作用到照相机
        camera.setParameters(rawParameters);
    }

    /**
     * 设置相机显示方向
     *
     * @param activity
     * @param cameraId
     */
    public static int getDisplayOrientation(Activity activity, int cameraId) {
        Camera.CameraInfo info = getInfo(cameraId);
        int rotation = activity
                .getWindowManager()
                .getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    /**
     * 获取图片的旋转角
     *
     * @param cameraId
     * @param orientation 手机的方位角，竖屏0，横屏90
     * @return
     */
    public static int getPictureRotation(int cameraId, int orientation) {
        Camera.CameraInfo info = getInfo(cameraId);
        orientation = (orientation + 45) / 90 * 90;
        int rotation;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            rotation = (info.orientation - orientation + 360) % 360;
        } else {  // back-facing camera
            rotation = (info.orientation + orientation) % 360;
        }
        return rotation;
    }

    /**
     * 分辨率最高
     *
     * @param list
     * @return
     */
    public static Camera.Size findMaxSize(List<Camera.Size> list) {
        if (list != null && list.size() > 0) {
            return Collections.max(list, new AreaComparator());
        }
        return null;
    }

    /**
     * 分辨率比视图大即可
     *
     * @param list
     * @param width  大者
     * @param height 小者
     * @return
     */
    public static Camera.Size findEnoughSize(List<Camera.Size> list, int width, int height) {
        if (list != null && list.size() > 0) {
            List<Camera.Size> bigEnough = new ArrayList<>();
            for (Camera.Size size : list) {
                if (size.width >= width && size.height >= height) {
                    bigEnough.add(size);
                }
            }
            if (bigEnough.size() == 0) {
                return Collections.max(list, new AreaComparator());
            } else {
                return Collections.min(bigEnough, new AreaComparator());
            }
        }
        return null;
    }

    private static class AreaComparator implements Comparator<Camera.Size> {

        @Override
        public int compare(Camera.Size o1, Camera.Size o2) {
            return o1.width * o1.height - o2.width * o2.height;
        }
    }
}
