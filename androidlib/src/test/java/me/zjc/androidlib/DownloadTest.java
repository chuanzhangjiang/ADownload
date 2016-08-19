package me.zjc.androidlib;

import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by ChuanZhangjiang on 2016/8/5.
 * 下载测试类(非标准单元测试)
 */
public class DownloadTest {

    @Test
    public void TestCancel() throws Exception {
        final AtomicInteger counter = new AtomicInteger(0);
        final DownloadTask task = new DownloadTask
                .DownloadBuilder("http://ww2.sinaimg.cn/large/610dc034jw1f6vyy5a99ej20u011gq87.jpg")
                .setPath("C:/Users/kobe/Desktop/")
                .build();

        task.start()
                .toObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.immediate())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        if (counter.get() == 6) {
                            task.cancel();
                        }
                        System.out.println("progress ==> " + aLong + " contentLength ==> "
                                + task.getContentLength());
                        counter.incrementAndGet();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                });

        Thread.sleep(1000*1000);
    }

    @Test
    public void testPause() throws Exception {
        final AtomicInteger counter = new AtomicInteger(0);
        final DownloadTask task = new DownloadTask
                .DownloadBuilder("http://ww2.sinaimg.cn/large/610dc034jw1f6vyy5a99ej20u011gq87.jpg")
                .setPath("C:/Users/kobe/Desktop/")
                .build();

        task.start()
                .toObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.immediate())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        if (counter.get() == 6) {
                            task.pause();
                        }
                        System.out.println("progress ==> " + aLong + " contentLength ==> "
                                + task.getContentLength());
                        counter.incrementAndGet();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                });

        Observable.timer(3, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.immediate())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        task.continueDownload();
                    }
                });

        Thread.sleep(1000*1000);
    }

    @Test
    public void testInterceptor() throws Exception {
        final DownloadTask task = new DownloadTask
                .DownloadBuilder("https://www.dcxg.com/apps/1.0/android/dcxg.apk")
                .setPath("C:/Users/kobe/Desktop/")
                .setInterceptors(new TestInterceptor())
                .setFileName("抽象.svg")
                .build();
        task.start()
                .toObservable()
                .observeOn(Schedulers.immediate())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        System.out.println("progress ==> " + aLong + " contentLength ==> "
                                + task.getContentLength());
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                });
        Thread.sleep(1000*1000);
    }

}
