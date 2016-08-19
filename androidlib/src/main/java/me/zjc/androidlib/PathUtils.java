package me.zjc.androidlib;

import android.os.Environment;

/**
 * Created by ChuanZhangjiang on 2016/8/7.
 * 路径工具，用于获取Android文件系统中的各种常用目录
 */
final class PathUtils {
    private PathUtils() {
        throw new IllegalAccessError();
    }

    /**
     * 获取文件默认下载路径
     * @return 文件的默认下载路径
     */
    public static String getDefaultDownloadPath() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .getAbsolutePath();
    }
}
