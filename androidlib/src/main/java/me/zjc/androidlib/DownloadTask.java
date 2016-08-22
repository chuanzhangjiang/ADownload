package me.zjc.androidlib;

import android.support.annotation.NonNull;

import net.jcip.annotations.GuardedBy;

import java.io.File;

import okhttp3.Interceptor;
import rx.Observable;
import rx.Subscription;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;

/**
 * Created by ChuanZhangjiang on 2016/8/7.
 * 下载任务
 */
public final class DownloadTask implements ITask {
    private final String url;
    private final String fileName;
    private final String path;
    private final DownloadHelper.DownloadListener downloadListener;
    private final Subject<Object, Object> bus;
    private final DownloadBuilder builder;
    private final DownloadApi mDownloadApi;
    private ContentLengthListener contentLengthListener;
    private long contentLength;
    private long finishProgress = 0L;
    private Subscription subscription;

    @GuardedBy("this")
    private TaskStatus status = TaskStatus.READY;

    private DownloadTask(DownloadBuilder builder) {
        this.builder = builder;
        this.url = builder.url;
        this.fileName = builder.fileName;
        this.path = builder.path;
        this.downloadListener = initDownloadListener();
        this.bus = new SerializedSubject<>(PublishSubject.create());
        this.mDownloadApi = builder.api;
    }

    private DownloadHelper.DownloadListener initDownloadListener() {
        return new DownloadHelper.DownloadListener() {
            @Override
            public void beforeDownload(long contentLength) {
                DownloadTask.this.contentLength = contentLength;
                if (contentLengthListener != null){
                    contentLengthListener.onGet(contentLength);
                }
            }

            @Override
            public void onDownload(long progress) {
                finishProgress += progress;
                bus.onNext(finishProgress);
            }

            @Override
            public void onDownloadFinish() {
                status = TaskStatus.FINISH;
                bus.onCompleted();
            }

            @Override
            public void onDownloadFailure(Throwable e) {
                bus.onError(e);
            }

            @Override
            public void onCancelSuccess() {
                doCancel();
            }

            @Override
            public void onPauseSuccess() {
                doPause();
            }
        };
    }

    /**
     * 开始下载
     * @throws IllegalStateException
     * @return DownloadTask 自身对象
     */
    public DownloadTask start() {
        synchronized (this) {
            if (status != TaskStatus.READY)
                throw new IllegalStateException("dirty task can`t be start");
            status = TaskStatus.START;
        }
        subscription = DownloadHelper.getInstance().baseDownload(url, fileName, path, downloadListener
                , this, mDownloadApi);
        return this;
    }

    /**
     * 继续下载
     * @return 自生
     */
    public DownloadTask continueDownload() {
        synchronized (this) {
            if (status != TaskStatus.PAUSE_SUCCESS)
                throw new IllegalStateException("only paused successful task can be continue");
            status = TaskStatus.CONTINUE;
        }
        subscription = DownloadHelper.getInstance()
                .continueDownload(url, fileName, path, downloadListener, this, mDownloadApi,
                        finishProgress, contentLength);
        return this;
    }

    /**
     * 第一时间获取下载内容长度
     * @param listener 监听器
     * @return DownloadTask自生
     */
    public DownloadTask onGetContentLength(@NonNull ContentLengthListener listener) {
        this.contentLengthListener = listener;
        return this;
    }

    /**
     * 查看任务状态是否<b>正在</b>暂停
     * @return 是否暂停，true是，false不是
     */
    @Override
    public synchronized boolean isPause() {
        return status == TaskStatus.PAUSE;
    }

    /**
     * 暂停方法
     */
    @Override
    public void pause() {
        synchronized (this) {
            if (status != TaskStatus.START && status != TaskStatus.CONTINUE)
                throw new IllegalStateException("only executed task can be pause");
            status = TaskStatus.PAUSE;
        }
    }

    /**
     * 获取下载内容大小的监听器
     */
    public interface ContentLengthListener {
        void onGet(long contentLength);
    }

    /**
     * 取消任务
     * @throws IllegalStateException
     */
    @Override
    public void cancel() {
        synchronized (this) {
            if (status != TaskStatus.START && status != TaskStatus.CONTINUE)
                throw new IllegalStateException("only executed task can be canceled");
            status = TaskStatus.CANCEL;
        }
    }

    /**
     * 克隆一个与当前任务具有相同配置的任务
     * @return 克隆之后的任务
     */
    public DownloadTask selfClone() {
        return new DownloadTask(builder);
    }

    private void doCancel() {
        subscription.unsubscribe();
        File file = new File(path + fileName);
        if (file.exists())
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        status = TaskStatus.CANCEL_SUCCESS;
    }

    private void doPause() {
        subscription.unsubscribe();
        status = TaskStatus.PAUSE_SUCCESS;
    }

    /**
     * 返回一个可被观察的对象，实现监视任务进度
     * @return 可被观察的对象
     * @see Observable;
     */
    public Observable<Long> toObservable() {
        return bus.ofType(Long.class);
    }

    /**
     * 获取任务是否<b>正在</b>取消
     * @return 任务是否<b>正在</b>取消
     */
    @Override
    public synchronized boolean isCancel() {
        return status == TaskStatus.CANCEL;
    }

    private enum TaskStatus {
        READY, START, PAUSE, PAUSE_SUCCESS, CONTINUE, CANCEL, CANCEL_SUCCESS, FINISH
    }

    /**
     * 获取下载内容的大小
     * 只有在下载开始的时候才能被获取到
     * @return 下载内容的大小；
     */
    public long getContentLength() {
        if (status == TaskStatus.READY)
            throw new IllegalStateException("task do not connected source, can not get content length!");
        return contentLength;
    }

    /**
     * 下载任务构建器
     * 如果没有设置文件名，则默认使用url中的后缀
     * 如果没有设置路径则默认下载到“/storage/emulated/0/Download”
     * 如果没有设置监听，则不回调
     */
    public static class DownloadBuilder {
        private final String url;
        private String fileName;
        private String path;
        private DownloadApi api;

        /**
         * 构造器
         * @param url 文件下载的网络路径
         */
        public DownloadBuilder(@NonNull String url) {
            this.url = url;
        }

        /**
         * 设置下载到本地之后的文件名
         * @param fileName 文件名
         * @return 设置了文件名的构建器
         */
        public DownloadBuilder setFileName(@NonNull String fileName) {
            this.fileName = fileName;
            return this;
        }

        /**
         * 设置下载到本地的某个目录
         * @param path 某个目录
         * @return 设置了目录的构建器
         */
        public DownloadBuilder setPath(@NonNull String path) {
            this.path = path;
            return this;
        }

        /**
         * 设置拦截器
         * @param interceptors 拦截器{@link Interceptor}
         * @return 自生
         */
        public DownloadBuilder setInterceptors(@NonNull Interceptor... interceptors) {
            this.api = DownloadApiProvider.newDownloadApi(interceptors);
            return this;
        }

        /**
         * 构建下载任务
         * @return 下载任务
         * @see DownloadTask
         */
        public DownloadTask build() {
            if (TextUtils.isEmpty(fileName)) {
                int index = url.lastIndexOf("/");
                fileName = url.substring(index + 1);
            }

            if (TextUtils.isEmpty(path)) {
                path = PathUtils.getDefaultDownloadPath();
            }

            if (api == null) {
                api = DownloadApiProvider.getDefaultDownLoadApi();
            }
            return new DownloadTask(this);
        }
    }
}
