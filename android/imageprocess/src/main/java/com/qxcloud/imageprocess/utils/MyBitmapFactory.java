package com.qxcloud.imageprocess.utils;


import android.graphics.Bitmap;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import java.io.FileOutputStream;

import java.io.InputStream;


/**
 * @Title:MyBitmapFactory
 * @Description: 自定义位图类
 * @Author: chenfuhao
 * @Since:2013-6-22 下午2:23:12
 */
public class MyBitmapFactory {

    /**
     * 图片质量的压缩，符合大小
     *
     * @param bm
     * @return
     */
    private static ByteArrayOutputStream getByteArrayOutputStream(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        //根据质量进行图片压缩
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);

        Logger.e("bmp size = "+baos.toByteArray().length/1024+"KB");

        int options = 100;
        // if (baos.toByteArray().length / 1024 > 1024) {//图片大于1M时进行压缩
        //获取最长的图片的边的长度
        int m = bm.getWidth() > bm.getHeight() ? bm.getWidth() : bm.getHeight();
        //如果大于1280的，1.5倍意思，压缩质量小于150K,2倍以上5倍以下，压缩质量小于400K,5倍以上压缩到600K，10倍以上1M
        int maxBitmapSize = 0;
        if (m <= 1280) {
            maxBitmapSize = 100;//100K
        } else if (m > 1280 && m <= 1280 * 1.5f) {
            maxBitmapSize = 120;//150K
        } else if (m > 1280 * 1.5f && m <= 1280 * 2f) {
            maxBitmapSize = 150;//150K
        } else if (m > 1280 * 2 && m <= 1280 * 5) {
            maxBitmapSize = 300;//300K
        } else if (m > 1280 * 5 && m <= 1280 * 10f) {
            maxBitmapSize = 600;//600K
        } else if (m > 1280 * 10) {
            maxBitmapSize = 1024;//1024K
        }
        while (baos.toByteArray().length / 1024 > maxBitmapSize) {
            //循环判断如果压缩后图片是否大于指定的大小,大于继续压缩
            baos.reset();//重置baos即清空baos
            bm.compress(Bitmap.CompressFormat.JPEG, options, baos);//这里压缩到options%，把压缩后的数据存放到baos中
            Logger.e("bmp size = "+baos.toByteArray().length/1024+"KB");
            options -= 5;//每次都减少5
            if (options <= 0)
                break;
        }
        //}
        Logger.e("bmp size = "+baos.toByteArray().length/1024+"KB");
        return baos;
    }

    /**
     * 将InputStream写入本地文件
     * @param destination 写入本地目录
     * @param input 输入流
     */
    public static boolean writeToLocal(String destination, InputStream input) {
        boolean isSuccess=false;
        int index;
        byte[] bytes = new byte[1024];
        FileOutputStream downloadFile = null;
        try {
            downloadFile = new FileOutputStream(destination);
            while ((index = input.read(bytes)) != -1) {
                downloadFile.write(bytes, 0, index);
                downloadFile.flush();
            }
            downloadFile.close();
            input.close();
            isSuccess=true;
        } catch (Exception e) {
            e.printStackTrace();
            isSuccess=false;
        }
      return isSuccess;
    }

    /**
     * 根据BitMap获取压缩后的图片 InputStream
     * @param bm
     * @return
     */
    public static InputStream getBitmapInputStreamForBitMap(Bitmap bm) {
        InputStream f = null;
        try {
            //第二步：图片质量压缩
            ByteArrayOutputStream baos = getByteArrayOutputStream(bm);
            f = new ByteArrayInputStream(baos.toByteArray());//把压缩后的数据baos存放到ByteArrayInputStream
        } catch (Exception e) {
            e.printStackTrace();
        }catch (Error error){
            error.printStackTrace();
        }
        return f;
    }
}
