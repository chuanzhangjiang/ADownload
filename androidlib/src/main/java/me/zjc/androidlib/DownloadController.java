package me.zjc.androidlib;

import android.support.annotation.NonNull;

import net.jcip.annotations.GuardedBy;

import rx.Observable;

/**
 * download controller
 * is a controller
 * control all download logic
 */
final class DownloadController implements IController {

    private final DownloadExecutor mExecutor;
    private DownloadTask.DownloadBuilder mBuilder;

    @GuardedBy("this")
    private Status mStatus;

    DownloadController(DownloadExecutor executor) {
        this.mExecutor = executor;
        mStatus = Status.READY;
    }

    @Override
    public void start() {
        synchronized (this) {
            if (mStatus != Status.READY)
                throw new IllegalStateException("only clear task can be start");
            mStatus = Status.START;
        }
        mExecutor.startDownload();
    }

    @Override
    public void continueDownload() {
        synchronized (this) {
            if (mStatus != Status.PAUSE)
                throw new IllegalStateException("only paused task can be continue");
            mStatus = Status.START;
        }
        mExecutor.continueDownload();
    }

    @Override
    public void pause() {
        synchronized (this) {
            if (mStatus != Status.START)
                throw new IllegalStateException("only executed task can be pause");
            mStatus = Status.PAUSE;
        }
        mExecutor.pause();
    }

    @Override
    public void cancel() {
        synchronized (this) {
            if (mStatus != Status.START)
                throw new IllegalStateException("only executed task can be cancel");
            mStatus = Status.CANCEL;
        }
        mExecutor.cancel();
    }

    @Override
    public Observable<DownloadInfo> toObservable() {
        return mExecutor.toObservable();
    }

    @Override
    public void setDownloadFinishListener(DownloadTask.DownloadFinishListener listener) {
        if (mStatus != Status.READY)
            throw new IllegalStateException("please invoke onDownloadFinish() before start()");
        mExecutor.setDownloadFinishListener(listener);
    }

    @Override
    public DownloadTask cloneTask() {
        return mBuilder.build();
    }

    @Override
    public void setBuilder(@NonNull DownloadTask.DownloadBuilder builder) {
        this.mBuilder = builder;
    }

    private enum Status{
        READY, START, PAUSE, CANCEL
    }
}
