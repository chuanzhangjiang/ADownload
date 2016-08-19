package me.zjc.androidlib;

/**
 * Created by ChuanZhangjiang on 2016/8/18.
 * 取消接口，用于标记可被取消的类型
 */
interface Cancelable {
    void cancel();
    boolean isCancel();
}
