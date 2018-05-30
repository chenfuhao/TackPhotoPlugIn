package com.qxcloud.imageprocess.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.qxcloud.imageprocess.R;
import com.qxcloud.imageprocess.activity.view.ProgressDiaLogView;
import com.qxcloud.imageprocess.crop.CropImageType;
import com.qxcloud.imageprocess.crop.CropImageView;
import com.qxcloud.imageprocess.editAPI.EditImageAPI;
import com.qxcloud.imageprocess.editAPI.EditImageMessage;
import com.qxcloud.imageprocess.editAPI.EditImgInterface;
import com.qxcloud.imageprocess.utils.FileUtils;
import com.qxcloud.imageprocess.utils.Logger;
import com.qxcloud.imageprocess.utils.MyBitmapFactory;
import com.qxcloud.imageprocess.utils.ToastUtils;

import java.io.File;
import java.io.InputStream;

/**
 * Created by cfh on 2017-09-05.
 * 图片编辑 裁剪
 * <p>
 * Edge 裁剪
 */

public class CropImgActivity extends Activity implements View.OnClickListener, EditImgInterface {
    private CropImageView m_cropmageView;//图片
    private RelativeLayout m_layout_return;//返回
    private RelativeLayout m_layout_preservation;
    private RelativeLayout m_layout_rotate;
    private Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //设置全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_editimg_view);
        activity = this;
        initView();
        EditImageAPI.getInstance().registerEditImg(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        m_layout_preservation.setEnabled(true);
    }

    /**
     * 初始化View
     */
    private void initView() {
        m_cropmageView = (CropImageView) findViewById(R.id.cropmageView);
        m_layout_return = (RelativeLayout) findViewById(R.id.tv_return);
        m_layout_preservation = (RelativeLayout) findViewById(R.id.tv_preservation);
        m_layout_rotate = (RelativeLayout) findViewById(R.id.tv_rotate);
        m_layout_return.setOnClickListener(this);
        m_layout_preservation.setOnClickListener(this);
        m_layout_rotate.setOnClickListener(this);
        ProgressDiaLogView.getInstance().showProgressDialog(activity, "图片加载……");
        handler.sendEmptyMessageDelayed(2, 100);
    }

    private void initBitMap() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //压缩图片并加载
                try {
                    if (null != BitmapTransfer.transferBitmapData) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(BitmapTransfer.transferBitmapData, 0, BitmapTransfer.transferBitmapData.length);
                        Logger.e("initBitmap ----- " + bitmap.getByteCount() + " w = " + bitmap.getWidth() + " h = " + bitmap.getHeight());
                        Message message = new Message();
                        message.what = 3;
                        message.obj = bitmap;
                        handler.sendMessageDelayed(message, 200);
                    }
                } catch (Exception e) {
                    Logger.e("Exception = " + e.getLocalizedMessage());
                    e.printStackTrace();
                }
            }

        }).start();
    }

    /**
     * 初始化裁剪網格
     *
     * @param bitmap
     */
    private void initCropImageView(Bitmap bitmap) {
        Bitmap hh = BitmapFactory.decodeResource(this.getResources(),
                R.drawable.crop_button);
        m_cropmageView.setCropOverlayCornerBitmap(hh);
        m_cropmageView.setImageBitmap(bitmap);
        m_cropmageView.setGuidelines(CropImageType.CROPIMAGE_GRID_ON_TOUCH);// 触摸时显示网格
        m_cropmageView.setFixedAspectRatio(false);// 自由剪切
        handler.sendEmptyMessage(1);
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.tv_return) {
            //返回
            finish();
        } else if (i == R.id.tv_preservation) {
            //确定
            try {
                handler.sendEmptyMessage(0);
                BitmapTransfer.transferBitmap = m_cropmageView.getCroppedImage();
                cropImg();

            } catch (Exception e) {
                handler.sendEmptyMessage(1);
            }
        } else if (i == R.id.tv_rotate) {
            //选转
            m_cropmageView.rotateImage(-90);
            Logger.e("EditImg**************> 旋转:RotatePic ");
        }
    }

    /**
     * 消息机制
     * 0：显示 进度条
     * 1：关闭进度条
     * 2：加載拍照圖片
     * 3：加載裁剪網格
     */
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    Logger.e("+++++++crop++++++++++图片裁剪中++++++++++++++++++++++++");
                    ProgressDiaLogView.getInstance().showProgressDialog(activity, "图片裁剪中……");
                    break;
                case 1:
                    Logger.e("+++++++crop++++++++++取消进度条++++++++++++++++++++++++");
                    ProgressDiaLogView.getInstance().dismissProgressDialog();
                    break;
                case 2:
                    initBitMap();
                    break;
                case 3:
                    Bitmap bitmap = (Bitmap) msg.obj;
                    initCropImageView(bitmap);
                    break;
            }
        }
    };

    /**
     * 裁剪并跳转新的页面
     */
    private void cropImg() {
        m_layout_preservation.setEnabled(false);
        if (BitmapTransfer.transferBitmap != null && !BitmapTransfer.transferBitmap.isRecycled()) {
            String path = saveBitmap(this, BitmapTransfer.transferBitmap);
            Logger.e("文件处理結果:" + path);
            m_layout_preservation.setEnabled(true);
            if (TextUtils.isEmpty(path)) {
                ToastUtils.showToast(activity, "图片保存失败");
            } else {
                EditImageAPI.getInstance().post(0, new EditImageMessage(0));

                // TODO: 2018-05-24
                finish();
            }
        }
    }


    @Override
    public void onEditImgResult(int code, EditImageMessage editImageMessage) {
        if (code == 0 && editImageMessage.getWhat() == 1) {
            Logger.e("通知成功:" + code);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EditImageAPI.getInstance().unRegisterEditImg(this);
        BitmapTransfer.transferBitmap = null;
        BitmapTransfer.transferBitmapData = null;
        m_cropmageView = null;
    }

    /**
     * 保存bitmap到本地
     *
     * @param context
     * @param mBitmap
     * @return
     */
    public String saveBitmap(Context context, Bitmap mBitmap) {
        try {
            String path = FileUtils.getSDCardFilePath();
            if (!TextUtils.isEmpty(path)) {
                File file = new File(path, "/Camera/");
                if (!file.exists()) {
                    file.mkdirs();
                }
                String savedDir = file.getAbsolutePath();
                String name = System.currentTimeMillis() + "";
                String saveName = "temp_cropped" + name + ".jpg";
                File filePic = new File(savedDir, saveName);
                if (!filePic.exists()) {
                    filePic.getParentFile().mkdirs();
                    filePic.createNewFile();
                }
                String newFilePath = filePic.getAbsolutePath();
                InputStream inputStream = MyBitmapFactory.getBitmapInputStreamForBitMap(mBitmap);
                boolean isSucess = MyBitmapFactory.writeToLocal(newFilePath, inputStream);
                handler.sendEmptyMessage(1);
                if (isSucess) {
                    return newFilePath;
                } else {
                    return null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        handler.sendEmptyMessage(1);
        return null;
    }
}
