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

    /**
     * 下载监听器
     */
    interface DownloadListener{
        /**
         * 在获取到资源的时候调用
         * @param contentLength 资源的长度，单位byte
         */
        void beforeDownload(long contentLength);

        /**
         * 在下载的时候调用，传入下载进度增量
         * @param progress 下载进度增量，单位byte
         */
        void onDownload(long progress);

        /**
         * 在下载结束的时候调用
         */
        void onDownloadFinish();

        /**
         * 在下载失败的时候回调
         * @param e 失败时抛出的异常
         */
        void onDownloadFailure(Throwable e);

        /**
         * 在取消成功之后调用
         */
        void onCancelSuccess();

        /**
         * 在暂停成功之后调用
         */
        void onPauseSuccess();
    }

}
