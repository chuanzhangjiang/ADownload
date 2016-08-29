package me.zjc.androidlib;

/**
 * download info class
 * include download info
 */
public final class DownloadInfo {

    private final long progress;
    private final long contentLength;

    DownloadInfo(long progress, long contentLength) {
        this.progress = progress;
        this.contentLength = contentLength;
    }

    /**
     * get current download progress
     * @return current download progress
     */
    public long getProgress() {
        return progress;
    }

    /**
     * get contentLength of current source
     * @return contentLength of current source
     */
    public long getContentLength() {
        return contentLength;
    }

    @Override
    public String toString() {
        return "DownloadInfo{" +
                "progress=" + progress +
                ", contentLength=" + contentLength +
                '}';
    }
}
