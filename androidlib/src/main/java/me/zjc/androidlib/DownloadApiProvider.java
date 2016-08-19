package me.zjc.androidlib;

import net.jcip.annotations.GuardedBy;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;

/**
 * Created by ChuanZhangjiang on 2016/8/5.
 * 用于提供DownloadApi的实现类
 */
final class DownloadApiProvider {
    @GuardedBy("DownloadApiProvider.class")
    private static volatile DownloadApi mDownloadApi = null;

    private DownloadApiProvider() {
        throw new IllegalAccessError();
    }

    static DownloadApi getDefaultDownLoadApi() {
        if (mDownloadApi == null) {
            synchronized (DownloadApiProvider.class) {
                if (mDownloadApi == null) {
                    mDownloadApi = createDownloadApi();
                }
            }
        }
        return mDownloadApi;
    }

    static DownloadApi newDownloadApi(Interceptor... interceptors) {
        return createDownloadApi(interceptors);
    }

    private static DownloadApi createDownloadApi(Interceptor... interceptors) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        for (Interceptor interceptor: interceptors){
            builder.addInterceptor(interceptor);
        }
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://www.github.com/")
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .client(builder.build())
                .build();
        return retrofit.create(DownloadApi.class);
    }
}
