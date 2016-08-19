package me.zjc.androidlib;

import java.io.File;
import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import okio.Okio;

/**
 * Created by ChuanZhangjiang on 2016/8/19.
 *
 */
public class TestInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        File file = new File("C:/Users/kobe/Desktop/接口优于抽象类.svg");
        BufferedSource source = Okio.buffer(Okio.source(file));
        ResponseBody body = ResponseBody.create(MediaType.parse("*/*"), file.length(), source);
        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .body(body).build();
    }
}
