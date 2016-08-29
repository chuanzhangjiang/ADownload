package me.zjc.androidlib;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.framed.Header;
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
        String range = request.header("Range");
        if (range == null) {
            File file = new File(Constants.TEST_SRC_FILE_PATH);
            BufferedSource source = Okio.buffer(Okio.source(file));
            ResponseBody body = ResponseBody.create(MediaType.parse("*/*"), file.length(), source);
            return new Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .body(body).build();
        } else {
            String rangeData = range.substring(range.indexOf("=") + 1);
            String[] data = rangeData.split("-");

            File file = new File(Constants.TEST_SRC_FILE_PATH);
            InputStream is = new FileInputStream(file);
            if (is.skip(Long.parseLong(data[0])) <= 0)
                throw new IllegalArgumentException("http header \"Rang\" is " + rangeData);
            BufferedSource source = Okio.buffer(Okio.source(is));
            ResponseBody body = ResponseBody.create(MediaType.parse("*/*"), file.length(), source);
            return new Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .body(body).build();
        }
    }
}
