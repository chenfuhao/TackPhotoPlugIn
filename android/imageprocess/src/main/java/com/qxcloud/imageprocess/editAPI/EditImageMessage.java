package com.qxcloud.imageprocess.editAPI;

/**
 * Created by cfh on 2017-09-11.
 * 0、通知外界数据
 * 1、通知裁剪页面
 * 2、通知照相页面
 */

public class EditImageMessage {
    private int what;
    private Object object;
    public EditImageMessage(int what){
        setWhat(what);
    }
    public EditImageMessage(int what,Object object){
        setWhat(what);
        setObject(object);
    }
    public int getWhat() {
        return what;
    }

    public void setWhat(int what) {
        this.what = what;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }
}
