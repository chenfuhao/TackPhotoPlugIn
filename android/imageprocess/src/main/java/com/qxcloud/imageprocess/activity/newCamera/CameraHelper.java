package com.qxcloud.imageprocess.activity.newCamera;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;

import com.qxcloud.imageprocess.utils.ScreenUtils;
import com.qxcloud.imageprocess.utils.ToastUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by cfh on 2017-05-10.
 * 拍照工具类
 */

public class CameraHelper {
    private final String TAG = "CameraHelper";
    private ToneGenerator tone;
    private String filePath;// = "/carchecker/photo";
    private boolean isPreviewing;

    private static CameraHelper helper;
    private Camera camera;
    private MaskSurfaceView surfaceView;

    //	分辨率
    private Size resolution;

    //	照片质量
    private int picQuality = 100;

    //	照片尺寸
    private Size pictureSize;

    //	闪光灯模式(default：自动)
    private String flashlightStatus = Parameters.FLASH_MODE_OFF;
    private Activity activity;

    public enum Flashlight {
        AUTO, ON, OFF
    }

    public static final float MAX_ASPECT_RATIO = (float) (Math.round((1920F / 1080F) * 100)) / 100;
    public static final int MAX_WIDTH = 1920;
    public static final int MAX_HEIGHT = 1080;
    public static final int MAX_WIDTH_HEIGHT_ASPECT = 10;
    public static final float MAX_ASPECT = 0.05f;

    private CameraHelper() {
    }

    public static synchronized CameraHelper getInstance() {
        if (helper == null) {
            helper = new CameraHelper();
        }
        return helper;
    }

    /**
     * 设置照片质量
     *
     * @param picQuality
     * @return
     */
    public CameraHelper setPicQuality(int picQuality) {
        this.picQuality = picQuality;
        return helper;
    }

    /**
     * 设置闪光灯模式
     *
     * @param status
     * @return
     */
    public CameraHelper setFlashlight(Flashlight status) {
        switch (status) {
            case AUTO:
                this.flashlightStatus = Parameters.FLASH_MODE_AUTO;
                break;
            case ON:
                this.flashlightStatus = Parameters.FLASH_MODE_ON;
                break;
            case OFF:
                this.flashlightStatus = Parameters.FLASH_MODE_OFF;
                break;
            default:
                this.flashlightStatus = Parameters.FLASH_MODE_AUTO;
        }
        return helper;
    }

    /**
     * @param mIsOpenFlashMode
     * @Description: 设置开启闪光灯(重新预览)
     * @Since:2015-8-12
     * @Version:1.1.0
     */
    public void setIsOpenFlashMode(boolean mIsOpenFlashMode) {
        try {
            if (null == this.camera) {
                return;
            }
            if (!mIsOpenFlashMode) {
                //要求关闭
                if (null == this.camera.getParameters().getFlashMode() || this.camera.getParameters().getFlashMode().equals(Camera.Parameters.FLASH_MODE_OFF)) {
                    //不做处理
                    return;
                } else {
                    this.flashlightStatus = Camera.Parameters.FLASH_MODE_OFF;
                }
            } else {
                //要求打开
                this.flashlightStatus = Parameters.FLASH_MODE_TORCH;
            }
            Parameters p = this.camera.getParameters();
            p.setFlashMode(this.flashlightStatus);
            this.camera.setParameters(p);
            this.startPreview();
        } catch (Exception e) {

        }
    }

    /**
     * 设置文件保存路径(default: /mnt/sdcard/DICM)
     *
     * @param path
     * @return
     */
    public CameraHelper setPictureSaveDictionaryPath(String path) {
        this.filePath = path;
        return helper;
    }

    public CameraHelper setMaskSurfaceView(MaskSurfaceView surfaceView) {
        this.surfaceView = surfaceView;
        return helper;
    }

    /**
     * 打开相机并开启预览
     *
     * @param holder       SurfaceHolder
     * @param format       图片格式
     * @param width        SurfaceView宽度
     * @param height       SurfaceView高度
     * @param screenWidth  屏幕宽度
     * @param screenHeight 屏幕高度
     */
    public void openCamera(SurfaceHolder holder, int format, int width, int height, int screenWidth, int screenHeight, Context context, CameraOpenCallBack cameraOpenCallBack) {
        try {
            if (this.camera != null) {
                this.camera.setPreviewCallback(null);
                this.camera.release();
            }
            this.camera = Camera.open();
            this.initParameters(holder, format, width, height, screenWidth, screenHeight);
            this.startPreview();
        } catch (Exception e) {
//            ModuleInterface.getInstance().showToast(context,"相机授权失败，请授权");
            cameraOpenCallBack.onCameraOpen(false, null);
        }
    }

    /**
     * 照相
     */
    public void tackPicture(Activity activity, final OnCaptureCallback callback, CameraOpenCallBack cameraOpenCallBack) {
        try {
            this.activity = activity;
            if (null != this.camera) {
                this.camera.autoFocus(new AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean flag, Camera camera) {
                        camera.takePicture(new ShutterCallback() {
                            @Override
                            public void onShutter() {
                                if (tone == null) {
//						 发出提示用户的声音
                                    tone = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME);
                                }
                                tone.startTone(ToneGenerator.TONE_PROP_BEEP);
                            }
                        }, null, new PictureCallback() {
                            @Override
                            public void onPictureTaken(byte[] data, Camera camera) {
                                String filepath = savePicture(data);
                                boolean success = false;
                                if (filepath != null) {
                                    success = true;
                                }
                                stopPreview();
//                                releaseCamera();
                                callback.onCapture(success, filepath);
                            }
                        });
                    }
                });
            } else {
                cameraOpenCallBack.onCameraOpen(false, null);
            }
        } catch (Exception e) {
            cameraOpenCallBack.onCameraOpen(false, null);
        }
    }

    /**
     * 照相
     */
    public void tackPictureNew(Activity activity, final OnCaptureCallbackNew callback, CameraOpenCallBack cameraOpenCallBack) {
        try {
            this.activity = activity;
            if (null != this.camera) {
                this.camera.autoFocus(new AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean flag, Camera camera) {
                        camera.takePicture(new ShutterCallback() {
                            @Override
                            public void onShutter() {
                                if (tone == null) {
//						 发出提示用户的声音
                                    tone = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME);
                                }
                                tone.startTone(ToneGenerator.TONE_PROP_BEEP);
                            }
                        }, null, new PictureCallback() {
                            @Override
                            public void onPictureTaken(byte[] data, Camera camera) {
                                stopPreview();
                                callback.onCaptureNew(data);
                            }
                        });
                    }
                });
            } else {
                cameraOpenCallBack.onCameraOpen(false, null);
            }
        } catch (Exception e) {
            cameraOpenCallBack.onCameraOpen(false, null);
        }
    }

    /**
     * 相机聚焦
     */
    public void autoFocus() {
        try {
            if (null != this.camera) {
                this.camera.autoFocus(new AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        if (success) {
                            //聚焦成功
                        } else {
                            //聚焦失败
                        }
                    }
                });
            }
        } catch (Exception e) {

        }
    }

    /**
     * 裁剪并保存照片
     *
     * @param data
     * @return
     */
    private String savePicture(byte[] data) {
        File imgFileDir = getImageDir();
        if (!imgFileDir.exists() && !imgFileDir.mkdirs()) {
            return null;
        }
//		文件路径路径
        String imgFilePath = imgFileDir.getPath() + File.separator + this.generateFileName();
        Bitmap b = this.cutImage(data);
        File imgFile = new File(imgFilePath);
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        try {
            fos = new FileOutputStream(imgFile);
            bos = new BufferedOutputStream(fos);
            b.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        } catch (Exception error) {
            return null;
        } finally {
            try {
                if (fos != null) {
                    fos.flush();
                    fos.close();
                }
                if (bos != null) {
                    bos.flush();
                    bos.close();
                }
            } catch (IOException e) {
            }
        }
        return imgFilePath;
    }

    /**
     * 生成图片名称
     *
     * @return
     */
    private String generateFileName() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddhhmmss", Locale.getDefault());
        String strDate = dateFormat.format(new Date());
        return "img_" + strDate + ".jpg";
    }

    /**
     * @return
     */
    private File getImageDir() {
        String path = null;
        if (this.filePath == null || this.filePath.equals("")) {
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath();
        } else {
            path = Environment.getExternalStorageDirectory().getPath() + filePath;
        }
        File file = new File(path);
        if (!file.exists()) {
            file.mkdir();
        }
        return file;
    }

    /**
     * 初始化相机参数
     *
     * @param holder       SurfaceHolder
     * @param format       图片格式
     * @param width        SurfaceView宽度
     * @param height       SurfaceView高度
     * @param screenWidth  屏幕宽度
     * @param screenHeight 屏幕高度
     */
    private void initParameters(SurfaceHolder holder, int format, int width, int height, int screenWidth, int screenHeight) {
        try {
            Parameters p = this.camera.getParameters();

            this.camera.setPreviewDisplay(holder);

            if (width > height) {
//				横屏
                this.camera.setDisplayOrientation(0);
            } else {
//				竖屏
                this.camera.setDisplayOrientation(90);
            }

//			照片质量
            p.set("jpeg-quality", picQuality);

//			设置照片格式
            p.setPictureFormat(PixelFormat.JPEG);

//			设置闪光灯
            p.setFlashMode(this.flashlightStatus);

//			设置最佳预览尺寸
            List<Size> previewSizes = p.getSupportedPreviewSizes();


            Size previewSize = this.getResultSizeByMaxWidthAndHeight(previewSizes, MAX_WIDTH, MAX_HEIGHT);
            if (previewSize != null) {
                Log.e("cfh", "previewSize == " + previewSize.width + "x" + previewSize.height);
                p.setPreviewSize(previewSize.width, previewSize.height);
            }

            List<Size> pictureSizes = p.getSupportedPictureSizes();
            Size pictureSize = this.getResultSizeByMaxWidthAndHeight(pictureSizes, MAX_WIDTH, MAX_HEIGHT);
            if (pictureSize != null) {
                Log.e("cfh", "pictureSize == " + pictureSize.width + "x" + pictureSize.height);
                p.setPictureSize(pictureSize.width, pictureSize.height);
            }


//
//
////			设置预览分辨率
//            if (this.resolution == null) {
//                this.resolution = this.getOptimalPreviewSize(previewSizes, width, height);
//            }
//            try {
//                p.setPreviewSize(this.resolution.width, this.resolution.height);
//            } catch (Exception e) {
//                Log.e(TAG, "不支持的相机预览分辨率: " + this.resolution.width + " × " + this.resolution.height);
//            }
//
////            p.setPreviewSize(1920, 1080);
//
////			设置照片尺寸
//            List<Size> pictureSizes = p.getSupportedPictureSizes();
//            if (this.pictureSize == null) {
//                this.setPicutreSize(pictureSizes, width, height);
//            }
////            p.setPictureSize(1920, 1080);
//            if (pictureSizes.contains(resolution)) {
//                //包含尺寸
//                Log.e("cfh", "包含尺寸" + this.resolution.width + " X " + this.resolution.height);
//                try {
//                    p.setPictureSize(this.resolution.width, this.resolution.height);
//                } catch (Exception e) {
//                    Log.e(TAG, "不支持的照片尺寸: " + this.pictureSize.width + " × " + this.pictureSize.height);
//                }
//            } else {
//                //不包含尺寸
//                Log.e("cfh", "不包含尺寸" + this.pictureSize.width + " X " + this.pictureSize.height);
//                try {
//                    p.setPictureSize(this.pictureSize.width, this.pictureSize.height);
//                } catch (Exception e) {
//                    Log.e(TAG, "不支持的照片尺寸: " + this.pictureSize.width + " × " + this.pictureSize.height);
//                }
//            }
            this.camera.setParameters(p);
        } catch (Exception e) {
            Log.e(TAG, "相机参数设置错误");
        }
    }

    /**
     * 释放Camera
     */
    public void releaseCamera() {
        try {
            if (this.camera != null) {
                this.flashlightStatus = Parameters.FLASH_MODE_OFF;
                if (this.isPreviewing) {
                    this.camera.setPreviewCallback(null);
                    this.stopPreview();
                }
                isPreviewing = false;
                this.camera.release();
                this.camera = null;
            }
        } catch (Exception e) {
        }
    }

    /**
     * 停止预览
     */
    private void stopPreview() {
        try {
            if (this.camera != null && this.isPreviewing) {
                this.camera.setPreviewCallback(null);
                this.camera.stopPreview();
                this.isPreviewing = false;
            }
        } catch (Exception e) {
        }
    }

    /**
     * 开始预览
     */
    public void startPreview() {
        try {
            if (this.camera != null) {
                this.camera.startPreview();
                autoFocus();
                this.isPreviewing = true;
            }
        } catch (Exception e) {

        }
    }

    /**
     * 裁剪照片
     *
     * @param data
     * @return
     */
    private Bitmap cutImage(byte[] data) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        try {
            if (this.surfaceView.getWidth() < this.surfaceView.getHeight()) {
//			竖屏旋转照片
                Matrix matrix = new Matrix();
                matrix.reset();
                matrix.setRotate(90);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }
            if (this.surfaceView == null) {
                return bitmap;
            } else {
                int[] sizes = this.surfaceView.getMaskSize();
                if (sizes[0] == 0 || sizes[1] == 0) {
                    return bitmap;
                }
                int w = bitmap.getWidth();
                int h = bitmap.getHeight();
                int offset = (int) (6 * ScreenUtils.getDensity(this.activity));
                int hh = sizes[1] + offset;
                int x = (w - sizes[0]) / 2;
                int y = (h - hh) / 2;
                return Bitmap.createBitmap(bitmap, x, y, sizes[0], hh);
            }
        } catch (Exception e) {
            return bitmap;
        }
    }

    /**
     * 获取最佳预览尺寸
     *
     * @param sizes            预览列表
     * @param maxAllowedWidth  SurfaceView宽度
     * @param maxAllowedHeight SurfaceView高度
     * @return
     */
    private Size getOptimalPreviewSize(List<Size> sizes, int maxAllowedWidth, int maxAllowedHeight) {
        if (sizes.size() <= 0) {
            ToastUtils.showToast(activity, "相机加载错误");
            return null;
        }
        Size optimalSize = null;
        for (Size size : sizes) {
            Log.e("cfh_f", size.width + "+++++++++" + size.height);
        }
        try {

            Log.e("cfh", "++++++++++++View预览图+++++++++++" + maxAllowedWidth + " X " + maxAllowedHeight);
            final double ASPECT_TOLERANCE = 0.05;
            double targetRatio = (double) maxAllowedWidth / maxAllowedHeight;
            if (sizes == null)
                return null;
            double minDiff = Double.MAX_VALUE;
            int targetHeight = maxAllowedHeight;
            for (Size size : sizes) {
                double r = size.width * 1.0 / size.height * 1.0;
                if (r != 4 / 3 || r != 3 / 4 || r != 16 / 9 || r != 9 / 16) {
                    continue;
                }

                double ratio = (double) size.width / size.height;
                if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                    continue;
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
            // Cannot find the one match the aspect ratio, ignore the requirement
            if (optimalSize == null) {
                minDiff = Double.MAX_VALUE;
                for (Size size : sizes) {
                    if (Math.abs(size.height - targetHeight) < minDiff) {
                        optimalSize = size;
                        minDiff = Math.abs(size.height - targetHeight);
                    }
                }
            }


//*************************************************************************************************************


//            int calcWidth = 0;
//            int calcHeight = 0;
//            Log.e("cfh", "**********View预览图************:" + maxAllowedWidth + "X" + maxAllowedHeight);
//            float aspectRatio = (float) (Math.round(((float) maxAllowedWidth / (float) maxAllowedHeight) * 100)) / 100;//获取预览比例
//
//            for (Size size : sizes) {
//                int width = size.width;//循环列表数据中的宽
//                int height = size.height;//循环列表数据中的高
//                float sizeAspectRatio = (float) (Math.round(((float) width / (float) height) * 100)) / 100;////循环列表数据中的比例
//                Log.e("cfh", "width -- " + width + " height = " + height + " sizeAspectRatio " + sizeAspectRatio + " aspectRatio = " + aspectRatio);
//                if (width <= maxAllowedWidth && height <= maxAllowedHeight) {
//                    if (width >= calcWidth && height >= calcHeight) {
//                        calcWidth = (int) width;
//                        calcHeight = (int) height;
//                        optimalSize = size;
//                    }
//                }
//            }
            Log.e("cfh", "**********最终预览图*************:" + optimalSize.height + "X" + optimalSize.width);
            return optimalSize;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("cfh", e.toString());
        }
        return optimalSize;
    }

    /**
     * 设置照片尺寸为最接近屏幕尺寸
     *
     * @param list    拍照尺寸列表
     * @param width_
     * @param height_
     */
    private void setPicutreSize(List<Size> list, int width_, int height_) {
        if (list.size() <= 0) {
            ToastUtils.showToast(activity, "相机加载错误");
            return;
        }
//        int approach = Integer.MAX_VALUE;
//        for (Size size : list) {
//            int temp = Math.abs(size.width - screenWidth + size.height - screenHeight);
//            System.out.println("approach: " + approach + ", temp: " + temp + ", size.width: " + size.width + ", size.height: " + size.height);
//            if (approach > temp) {
//                approach = temp;
//                this.pictureSize = size;
//                Log.e("cfh","***********pictureSize************:"+pictureSize.height+"X"+pictureSize.width);
//            }
//        }
//
////		//降序
////		if(list.get(0).width>list.get(list.size()-1).width){
////			int len = list.size();
////			list = list.subList(0, len/2==0? len/2 : (len+1)/2);
////			this.pictureSize = list.get(list.size()-1);
////		}else{
////			int len = list.size();
////			list = list.subList(len/2==0? len/2 : (len-1)/2, len-1);
////			this.pictureSize = list.get(0);
////		}
        //***************************方法二********************************
////        for (int i = 0; i <list.size() ; i++) {
////            Log.e("cfh_","+++++sizes.toString++++Take+++："+list.get(i).width+" X "+list.get(i).height);
////        }
//        Collections.sort(list, new Comparator<Size>(){
//            /*
//             * int compare(Size o1, Size o2) 返回一个基本类型的整型，
//             * 返回负数表示：o1 小于o2，
//             * 返回0 表示：o1和o2相等，
//             * 返回正数表示：o1大于o2。
//             */
//            public int compare(Size o1, Size o2) {
//
//                //按照宽度降序排序
//                if(o1.width < o2.width){
//                    return 1;
//                }
//                if(o1.width == o2.width){
//                    return 0;
//                }
//                return -1;
//            }
//        });
//        int nearNum = this.resolution.width+this.resolution.height;//接近的数值
//        this.pictureSize = list.get(0);
//        for (Size size : list){
//            int diffNumTemp =size.width+size.height;
//            Log.e("cfh","***************当前值******:"+size.height+"X"+size.width);
//            if (diffNumTemp <= nearNum){
//                this.pictureSize = size;
//                Log.e("cfh","***********pictureSize*****符合*******:"+size.height+"X"+size.width);
//                break;
//            }else{
//                Log.e("cfh","***********pictureSize******不符合******:"+size.height+"X"+size.width);
//            }
//        }
        //******************************方法三**********************************

//        for (int i = 0; i < list.size(); i++) {
//            Log.e("cfh_", "+++++sizes.toString++++Take+++：" + list.get(i).width + " X " + list.get(i).height);
//        }
        try {
            int calcWidth = 0;
            int calcHeight = 0;
            int maxAllowedWidth = this.resolution.width;//获取预览宽
            int maxAllowedHeight = this.resolution.height;//获取预览高
            Log.e("cfh", "**********最终预览图************:" + resolution.height + "X" + resolution.width);
            float aspectRatio = (float) (Math.round(((float) maxAllowedWidth / (float) maxAllowedHeight) * 100)) / 100;//获取预览比例

            for (Size size : list) {
                int width = size.width;//循环列表数据中的宽
                int height = size.height;//循环列表数据中的高
                float sizeAspectRatio = (float) (Math.round(((float) width / (float) height) * 100)) / 100;////循环列表数据中的比例
                Log.e("cfh", "width -- " + width + " height = " + height + " sizeAspectRatio " + sizeAspectRatio + " aspectRatio = " + aspectRatio);
                if (width <= maxAllowedWidth && height <= maxAllowedHeight) {
                    if (width >= calcWidth && height >= calcHeight) {
                        calcWidth = (int) width;
                        calcHeight = (int) height;
                        this.pictureSize = size;
                        Log.e("cfh", "**********最终拍照pictureSize*************:" + pictureSize.height + "X" + pictureSize.width);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("cfh", e.toString());
        }
    }

    /**
     * 设置照片尺寸为最接近屏幕尺寸 适配型
     *
     * @param list             拍照尺寸列表
     * @param maxAllowedWidth
     * @param maxAllowedHeight
     */
    private Size getResultSizeByMaxWidthAndHeight(List<Size> list, int maxAllowedWidth, int maxAllowedHeight) {
        Size resultSize = null;
        int caclWidth = 0;
        int caclHeight = 0;
        try {
            for (Size size : list) {
                int width = size.width;//循环列表数据中的宽
                int height = size.height;//循环列表数据中的高
                float sizeAspectRatio = (float) (Math.round(((float) width / (float) height) * 100)) / 100;////循环列表数据中的比例

                int maxWidth = maxAllowedWidth + MAX_WIDTH_HEIGHT_ASPECT;
                int maxHeight = maxAllowedHeight + MAX_WIDTH_HEIGHT_ASPECT;
                float ratioDiff = Math.abs(sizeAspectRatio - MAX_ASPECT_RATIO);

                Log.e("cfh", "list width - " + width + " height - " + height + " ratioDiff - " + ratioDiff);

                if (ratioDiff <= MAX_ASPECT) {
                    if (width <= maxWidth && height <= maxHeight) {
                        if (width >= caclWidth && height >= caclHeight) {
                            caclWidth = width;
                            caclHeight = height;
                            resultSize = size;
                        }
                    }
                }


            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("cfh", e.toString());
        }
        return resultSize;
    }
}

