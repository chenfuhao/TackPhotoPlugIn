package com.qxcloud.imageprocess.activity.newCamera;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.qxcloud.imageprocess.R;
import com.qxcloud.imageprocess.activity.BitmapTransfer;
import com.qxcloud.imageprocess.activity.CropImgActivity;
import com.qxcloud.imageprocess.editAPI.EditImageAPI;
import com.qxcloud.imageprocess.editAPI.EditImageMessage;
import com.qxcloud.imageprocess.editAPI.EditImgInterface;
import com.qxcloud.imageprocess.utils.Logger;

/**
 * Created by cfh on 2017-11-03.
 * 基于SurfaceView 进行自定义相机处理
 */

public class NewTackPhotoActivity extends Activity implements OnCaptureCallbackNew, View.OnClickListener,EditImgInterface {
    private CheckBox m_photographs;//闪光灯
    private MaskSurfaceView m_surfaceView;//相机
    private TextView m_hint;//默认文字
    private ImageView m_takePhotoGuide;//引导图
    private ImageView m_btnRecapture;//关闭按钮
    private TextView m_tackphotoBtn;//拍照
    private RelativeLayout takePhotoLayout;

    private Activity activity;
    //获取保存的图片地址
    private String mSavedDir;
    private static final String EXTRA_DEFAULT_SAVE_DIR = "default_save_dir";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //设置全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        EditImageAPI.getInstance().registerEditImg(this);
        activity = this;
        setContentView(R.layout.new_tackphoto_activity_view);
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        m_tackphotoBtn.setEnabled(true);
        handler.sendEmptyMessageDelayed(6, 300);
    }

    /**
     * 初始化页面事件及其操作
     */
    private void initView() {
        mSavedDir = getIntent().getStringExtra(EXTRA_DEFAULT_SAVE_DIR);
        Logger.e("+++++++++++mSavedDir+++++++++++" + mSavedDir);
        m_photographs = (CheckBox) findViewById(R.id.photographs);
        m_surfaceView = (MaskSurfaceView) findViewById(R.id.surface_view);
        m_tackphotoBtn = (TextView) findViewById(R.id.tackphoto_btn);
        m_takePhotoGuide = (ImageView) findViewById(R.id.take_photo_guide);
        m_btnRecapture = (ImageView) findViewById(R.id.btn_recapture);
        takePhotoLayout = (RelativeLayout) findViewById(R.id.take_photo_layout);
        m_tackphotoBtn.setEnabled(true);
        m_tackphotoBtn.setOnClickListener(this);
        m_takePhotoGuide.setOnClickListener(this);
        m_btnRecapture.setOnClickListener(this);
        m_surfaceView.setOnClickListener(this);

        m_photographs.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                Logger.e("启动闪光灯");
                Message message = new Message();
                message.what = 4;
                message.obj = isChecked;
                handler.sendMessage(message);
            }
        });
    }
    @Override
    public void onCaptureNew(byte[] data) {
        String message = "拍照成功";
        if (null==data) {
            message = "拍照失败";
            CameraHelper.getInstance().startPreview();
            this.m_surfaceView.setVisibility(View.VISIBLE);
        } else {
            gotoNextPage(data);
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * 跳转下一个页面
     * @param data
     */
    private void gotoNextPage(byte[] data) {
        Intent intent = new Intent(this, CropImgActivity.class);
        intent.putExtra("FILE_PATH", mSavedDir);
        BitmapTransfer.transferBitmapData = data;
        startActivity(intent);
        m_tackphotoBtn.setEnabled(true);
    }

    /**
     * 相机开启监听
     */
    CameraOpenCallBack cameraOpenCallBack = new CameraOpenCallBack() {
        @Override
        public void onCameraOpen(boolean success, Object object) {
            m_tackphotoBtn.setEnabled(true);
            if (!success) {
                //相机打开失败
                handler.sendEmptyMessage(5);
            }
        }
    };
    /**
     * 消息机制
     */
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    //拍照
                    CameraHelper.getInstance().tackPictureNew(activity, NewTackPhotoActivity.this, cameraOpenCallBack);
                    break;
                case 2:
//                    //重拍
//                    deleteFile();
//                    CameraHelper.getInstance().startPreview();
                    //关闭
                    finish();
                    break;
                case 3:
                    //聚焦
                    CameraHelper.getInstance().autoFocus();
                    Logger.e("+++++++聚焦+++");
                    break;
                case 4:
                    //闪光灯
                    boolean isChecked = (boolean) msg.obj;
                    CameraHelper.getInstance().setIsOpenFlashMode(isChecked);
                    break;
                case 5:






//                    //相机打开失败
//                    ModuleInterface.getInstance().showAlertDialog(activity, "打开照相机失败\n\n请在上一个页面或手机系统设置中允许授权", null, "确定", new AlertDialogInterface() {
//                        @Override
//                        public void onLeftClickListener(String a1, Object object) {
//
//                        }
//
//                        @Override
//                        public void onRightClickListener(String a1, Object object) {
//                            finish();
//                        }
//                    });
                    break;
                case 6:
                    if (m_surfaceView != null) {
                        m_surfaceView.openCamera(cameraOpenCallBack);
                    }
                    break;
            }
        }
    };

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.surface_view) {//聚焦
            handler.sendEmptyMessage(3);
        } else if (i == R.id.take_photo_guide) {//引导
            m_takePhotoGuide.setVisibility(View.GONE);

        } else if (i == R.id.tackphoto_btn) {//拍照
            if(m_tackphotoBtn.isEnabled()){
                m_tackphotoBtn.setEnabled(false);
                handler.sendEmptyMessage(1);
            }
        } else if (i == R.id.btn_recapture) {//关闭
            handler.sendEmptyMessage(2);
        }
    }

    @Override
    public void onEditImgResult(int code, EditImageMessage editImageMessage) {
        if (code == 0 && editImageMessage.getWhat() == 0) {
            Logger.e("通知成功:" + code);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            EditImageAPI.getInstance().unRegisterEditImg(this);
            if (m_surfaceView != null)
                m_surfaceView.setVisibility(View.GONE);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}

