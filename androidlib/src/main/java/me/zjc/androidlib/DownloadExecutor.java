package me.zjc.androidlib;


import android.support.annotation.NonNull;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.ResponseBody;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;

/**
 * download executor
 * Execution really download operation
 */
final class DownloadExecutor {

    private final DownloadApi mApi;
    private final String mUrl;
    private final String mFilename;
    private final String mPath;
    private final Subject<DownloadInfo, DownloadInfo> mBus;

    private volatile boolean isPause = false;
    private volatile boolean isCancel = false;

    private long currentProgress = 0L;
    private long contentLength = -1L;

    private DownloadTask.DownloadFinishListener mDownloadFinishListener;

    DownloadExecutor(@NonNull DownloadApi api,
                     @NonNull String url, @NonNull String filename, @NonNull String path) {
        this.mApi = api;
        this.mUrl = url;
        this.mFilename = filename;
        this.mPath = path;
        this.mBus = new SerializedSubject<>(PublishSubject.<DownloadInfo>create());
    }

    void startDownload() {
        resetStatus();
        mApi.download(mUrl)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(new Subscriber<ResponseBody>() {
                    @Override
                    public void onCompleted() {
                        onDownloadFinish();
                    }

                    @Override
                    public void onError(Throwable e) {
                        onDownloadError(e);
                    }

                    @Override
                    public void onNext(ResponseBody body) {
                        onDownload(body, new OutputStreamMaker() {
                            @Override
                            public OutputStream make() throws FileNotFoundException {
                                return new FileOutputStream(new File(mPath + mFilename), false);
                            }
                        });
                    }
                });
    }

    void continueDownload() {
        resetStatus();
        mApi.continueDownload(mUrl, generateRangeData(currentProgress, contentLength))
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(new Subscriber<ResponseBody>() {
                    @Override
                    public void onCompleted() {
                        onDownloadFinish();
                    }

                    @Override
                    public void onError(Throwable e) {
                        onDownloadError(e);
                    }

                    @Override
                    public void onNext(ResponseBody body) {
                        onDownload(body, new OutputStreamMaker() {
                            @Override
                            public OutputStream make() throws FileNotFoundException {
                                return new FileOutputStream(new File(mPath + mFilename), true);
                            }
                        });
                    }
                });
    }

    private void resetStatus() {
        isPause = false;
        isCancel = false;
    }

    private void onDownloadFinish() {
        mBus.onCompleted();
        if (mDownloadFinishListener != null)
            mDownloadFinishListener.onFinish();
    }

    private void onDownloadError(Throwable e) {
        mBus.onError(e);
    }

    void pause() {
        this.isPause = true;
    }

    void cancel() {
        this.isCancel = true;
    }

    Observable<DownloadInfo> toObservable() {
        return mBus.asObservable();
    }

    void setDownloadFinishListener(DownloadTask.DownloadFinishListener listener) {
        this.mDownloadFinishListener = listener;
    }

    private String generateRangeData(long start, long end) {
        return "bytes=" + start + "-" + end;
    }

    private void onDownload(ResponseBody body, OutputStreamMaker maker) {
        InputStream is = body.byteStream();
        BufferedInputStream bis = new BufferedInputStream(is);
        OutputStream fos = null;
        BufferedOutputStream bos = null;
        try {
            fos = maker.make();
            bos = new BufferedOutputStream(fos);
            byte[] buf = new byte[2048];
            for (long readLength;
                 !isCancel && !isPause && (readLength = bis.read(buf)) != -1; ) {

                bos.write(buf, 0, (int) readLength);

                currentProgress += readLength;
                contentLength = body.contentLength();
                mBus.onNext(new DownloadInfo(currentProgress, contentLength));
            }
        } catch (IOException e) {
            mBus.onError(e);
        } finally {
            ReleaseUtils.flushAll(bos, fos);
            ReleaseUtils.closeAll(bos, fos, bis, is);
            if (isCancel) {
                Observable.just(new File(mPath + mFilename))
                        .filter(new Func1<File, Boolean>() {
                            @Override
                            public Boolean call(File file) {
                                return file.exists();
                            }
                        }).map(new Func1<File, Boolean>() {
                            @Override
                            public Boolean call(File file) {
                                return file.delete();
                            }
                        }).subscribe(new Action1<Boolean>() {
                            @Override
                            public void call(Boolean aBoolean) {
                                if (!aBoolean) {
                                    System.err.print("delete file failure");
                                }
                            }
                        });
            }
        }
    }

    interface OutputStreamMaker{
        OutputStream make() throws FileNotFoundException;
    }

}
