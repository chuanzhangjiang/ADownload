package me.zjc.androidlib;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

/**
 * Created by ChuanZhangjiang on 2016/8/7.
 * 资源释放工具，用于IO操作结束的时候释放资源
 */
final class ReleaseUtils {
    private ReleaseUtils() {
        throw new IllegalAccessError();
    }

    /**
     * 执行刷新操作
     * 刷新顺序：从传入的第一个参数开始
     * @param flushes 被刷新的对象
     */
    public static void flushAll(Flushable... flushes) {
        if (flushes.length <= 0)
            return;

        for (Flushable flush : flushes) {
            if (flush == null)
                continue;
            try {
                flush.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 执行关闭操作
     * 刷新顺序：从传入的第一个参数开始
     * @param closeables 被关闭的对象
     */
    public static void closeAll(Closeable... closeables) {
        if (closeables.length <= 0)
            return;

        for (Closeable closeable : closeables) {
            if (closeable == null)
                continue;
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
