package me.zjc.androidlib;

import android.support.annotation.NonNull;

import net.jcip.annotations.GuardedBy;

import java.io.File;

import okhttp3.ResponseBody;
import rx.Subscription;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by ChuanZhangjiang on 2016/8/5.
 * 文件下载辅助类，用于辅助DownloadTask完成下载工作
 */
final class DownloadHelper {
    @GuardedBy("DownloadHelper.class")
    private static volatile DownloadHelper instance;

    private DownloadHelper() {
    }

    static DownloadHelper getInstance() {
        if (instance == null) {
            synchronized (DownloadHelper.class) {
                if (instance == null) {
                    instance = new DownloadHelper();
                }
            }
        }
        return instance;
    }

    Subscription baseDownload(@NonNull String url, final String filename, final String path
            , @NonNull final DownloadListener listener, @NonNull final ITask task
            , @NonNull DownloadApi api) {
        return api.download(url)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(new Action1<ResponseBody>() {
                    @Override
                    public void call(ResponseBody responseBody) {
                        DownloadUtils.onDownloadStart(responseBody, listener);
                        DownloadUtils.onDownload(responseBody, listener, new File(path + filename), task, false);
                        if (!task.isPause() && !task.isCancel()) {
                            DownloadUtils.onDownloadFinish(listener);
                        }
                        if (task.isCancel()) {
                            listener.onCancelSuccess();
                        } else if (task.isPause()) {
                            listener.onPauseSuccess();
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(final Throwable throwable) {
                        DownloadUtils.onDownloadError(listener, throwable);
                    }
                });
    }

    Subscription continueDownload(@NonNull String url, final String filename, final String path,
                                  @NonNull final DownloadListener listener,
                                  @NonNull final ITask task, @NonNull DownloadApi api,
                                  long position, long length) {
        return api.continueDownload(url, DownloadUtils.generateRangeData(position, length))
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(new Action1<ResponseBody>() {
                    @Override
                    public void call(ResponseBody responseBody) {
                        DownloadUtils.onDownload(responseBody, listener,
                                new File(path + filename), task, true);
                        if (!task.isPause() && !task.isCancel()) {
                            DownloadUtils.onDownloadFinish(listener);
                        }
                        if (task.isCancel()) {
                            listener.onCancelSuccess();
                        } else if (task.isPause()) {
                            listener.onPauseSuccess();
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        DownloadUtils.onDownloadError(listener, throwable);
                    }
                });
    }

    interface DownloadListener{
        void beforeDownload(long contentLength);
        void onDownload(DownloadInfo downloadInfo);
        void onDownloadFinish();
        void onDownloadFailure(Throwable e);
        void onCancelSuccess();
        void onPauseSuccess();
    }

}
