package me.zjc.androidlib;

import android.support.annotation.NonNull;

import rx.Observable;

/**
 * download controller interface
 */
interface IController {
    /**
     * start download
     * condition of start download is "status == READY"
     */
    void start();

    /**
     * continue download
     * condition of continue is "status == TaskStatus.PAUSE_SUCCESS"
     */
    void continueDownload();

    /**
     * pause download
     * condition of pause is "status == TaskStatus.START || status == TaskStatus.CONTINUE"
     */
    void pause();

    /**
     * cancel download
     * condition of cancel is "status == TaskStatus.START || status == TaskStatus.CONTINUE"
     */
    void cancel();

    Observable<DownloadInfo> toObservable();

    /**
     * set call back when download is finish
     * condition is "status == TaskStatus.READY"
     * @param listener call back
     */
    void setDownloadFinishListener(DownloadTask.DownloadFinishListener listener);

    DownloadTask cloneTask();

    void setBuilder(@NonNull DownloadTask.DownloadBuilder builder);

}
