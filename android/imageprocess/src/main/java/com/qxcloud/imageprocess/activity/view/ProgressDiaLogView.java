package com.qxcloud.imageprocess.activity.view;

import android.app.AlertDialog;
import android.content.Context;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.qxcloud.imageprocess.R;

/**
 * Created by cfh on 2016/2/18 0018.
 * 对框框View
 */
public class ProgressDiaLogView {
    /**
     * GeneralUtil 实例
     */
    private static ProgressDiaLogView singletonGeneralUtil = null;
    /**
     * 请求数据进度条对话框
     */
    public AlertDialog dialogprogress = null;
    /**
     * alertDialog对话框
     */
    public AlertDialog alertDialog = null;
    /**
     * alertInputDialog
     */
    public AlertDialog alertInputDialog = null;

    private TextView tipTextView;

    /**
     * 单例模式
     */
    public static ProgressDiaLogView getInstance() {
        if (singletonGeneralUtil == null) {
            synchronized (ProgressDiaLogView.class) {
                if (singletonGeneralUtil == null) {
                    singletonGeneralUtil = new ProgressDiaLogView();
                }
            }
        }
        return singletonGeneralUtil;
    }

    public boolean progressDialogIsShowing(){
        if(dialogprogress!=null&&dialogprogress.isShowing()){
            return true;
        }
        return false;
    }



    public void showProgressDialog(Context context, String test) {

        try{
            dismissProgressDialog();
            dialogprogress = new AlertDialog.Builder(context,R.style.LoadingDialog).create();
            dialogprogress.show();
            dialogprogress.setCancelable(false);
            Window window = dialogprogress.getWindow();
            window.setContentView(R.layout.layout_frame_http_waitting_dialog);
            RelativeLayout layout = (RelativeLayout) window.findViewById(R.id.parent);// 加载布局
            ImageView spaceshipImage = (ImageView) window
                    .findViewById(R.id.iv);
            tipTextView = (TextView) window.findViewById(R.id.tv);// 提示文字
            // 加载动画
            Animation hyperspaceJumpAnimation = AnimationUtils.loadAnimation(
                    context, R.anim.loading);
            // 使用ImageView显示动画
            spaceshipImage.startAnimation(hyperspaceJumpAnimation);
            tipTextView.setText(test);// 设置加载信息
            dialogprogress.setContentView(layout, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT));// 设置布局
        }catch (Exception e){
            e.printStackTrace();
            //context销毁时调用show会报错
        }
    }

    public void updateDialogMessage(String message){
        if(tipTextView!=null){
            tipTextView.setText(message);
        }
    }




    public void dismissProgressDialog() {
        try {
            if (dialogprogress != null && dialogprogress.isShowing()) {
                dialogprogress.dismiss();
                dialogprogress = null;
            }
        } catch (Exception e) {
//            MyLog.log("SDK ProgressDialog dismiss exception:", e.toString());
        }
    }
}
