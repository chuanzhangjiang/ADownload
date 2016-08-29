package me.zjc.androidlib;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.ResponseBody;

/**
 * Created by ChuanZhangjiang on 2016/8/18.
 *
 */
final class DownloadUtils {
    private DownloadUtils(){
        throw new IllegalAccessError();
    }

    static String generateRangeData(long start, long end) {
        return "bytes=" + start + "-" + end;
    }

    static void onDownloadStart(ResponseBody responseBody, DownloadHelper.DownloadListener listener) {
        listener.beforeDownload(responseBody.contentLength());
    }

    static void onDownload(ResponseBody responseBody
            , final DownloadHelper.DownloadListener listener
            , File file, ITask task, boolean isContinue) {
        InputStream is = responseBody.byteStream();
        BufferedInputStream bis = new BufferedInputStream(is);
        OutputStream fos = null;
        BufferedOutputStream bos = null;
        try {
            if (isContinue) {
                fos = new FileOutputStream(file, true);
            } else {
                fos = new FileOutputStream(file, false);
            }
            bos = new BufferedOutputStream(fos);
            byte[] buf = new byte[2048];
            for (long readLength;
                 !task.isCancel() && !task.isPause() && (readLength = bis.read(buf)) != -1; ) {
                bos.write(buf, 0, (int) readLength);
                listener.onDownload(new DownloadInfo(readLength, responseBody.contentLength()));
            }
        } catch (IOException e) {
            onDownloadError(listener, e);
        } finally {
            ReleaseUtils.flushAll(bos, fos);
            ReleaseUtils.closeAll(bos, fos, bis, is);

        }

    }

    static void onDownloadFinish(final DownloadHelper.DownloadListener listener) {
        listener.onDownloadFinish();
    }

    static void onDownloadError(final DownloadHelper.DownloadListener listener,final Throwable e) {
        listener.onDownloadFailure(e);
    }
}
