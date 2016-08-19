package me.zjc.androidlib;

import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Streaming;
import retrofit2.http.Url;
import rx.Observable;

/**
 * Created by ChuanZhangjiang on 2016/8/5.
 * 文件下载API
 * 用于根据文件的url获取文件资源
 */
interface DownloadApi {
    @Streaming
    @GET
    Observable<ResponseBody> download(@Url String url);

    @Streaming
    @GET
    Observable<ResponseBody> continueDownload(@Url String url, @Header("Range") String range);
}
