package com.qxcloud.imageprocess;

import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.qxcloud.imageprocess.activity.newCamera.NewTackPhotoActivity;

/**
 * Created by cfh on 2018-05-25.
 * TODO
 */

public class MyNativeModule extends ReactContextBaseJavaModule {
    public static final String REACTCLASSNAME = "MyNativeModule";

    public MyNativeModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return REACTCLASSNAME;
    }

    /**
     * 同原生交互数据方法和接口
     *
     * @param map     请求参数
     * @param promise 回调函数
     */
    @ReactMethod
    public void NativeMethod(ReadableMap map, Promise promise) {
        //调用Test类中的原生方法。
        Log.e("CFH", "获取的数据为：" + map.getInt("TYPE"));
        Toast.makeText(getReactApplicationContext(), "跳转原生", Toast.LENGTH_LONG).show();
        if (map.getInt("TYPE") == 1) {
//            MyNativeModule.promise=promise;
            Intent intent = new Intent(getCurrentActivity(), NewTackPhotoActivity.class);
            getCurrentActivity().startActivityForResult(intent,10000);
        }
    }

}
