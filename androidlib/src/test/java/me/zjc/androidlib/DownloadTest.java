package me.zjc.androidlib;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import me.zjc.androidlib.utils.Md5Utils;
import me.zjc.androidlib.utils.RxUnitTestUtils;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import okio.Okio;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * DownloadTask unit test class
 */
public class DownloadTest {

    private DownloadTask task;
    private static final String DOWNLOAD_FILE_NAME = "download.svg";
    private static final String url = "http://www.someurl.com/file.svg";
    public static final String TEST_SRC_FILE_PATH = "testFile/src.svg";
    public static final String TEST_DOWNLOAD_FILE_PATH = "testFile/download/";

    @Mock
    private DownloadTask.DownloadFinishListener mockDownloadFinishListener;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        RxUnitTestUtils.openRxTools();
        task = new DownloadTask.DownloadBuilder(url)
                .setFileName(DOWNLOAD_FILE_NAME)
                .setPath(TEST_DOWNLOAD_FILE_PATH)
                .setInterceptors(new MockServer())
                .build();
    }

    @After
    public void tearDown() throws Exception {
        Observable.just(new File(TEST_DOWNLOAD_FILE_PATH + DOWNLOAD_FILE_NAME))
                .filter(new Func1<File, Boolean>() {
                    @Override
                    public Boolean call(File file) {
                        return file.exists();
                    }
                })
                .subscribe(new Action1<File>() {
                    @Override
                    public void call(File file) {
                        if (!file.delete())
                            throw new RuntimeException("delete file fail");
                    }
                });
    }

    /**
     * test most simple download
     * @throws Exception
     */
    @Test
    public void testDownload() throws Exception {
        task.start();
        checkedDownloadSuccess();
    }

    private void checkedDownloadSuccess() {
        File srcFile = new File(TEST_SRC_FILE_PATH);
        File downloadedFile =
                new File(TEST_DOWNLOAD_FILE_PATH + DOWNLOAD_FILE_NAME);
        String srcFileMd5 = Md5Utils.getFileMd5String(srcFile);
        String downloadedFileMd5 = Md5Utils.getFileMd5String(downloadedFile);
        assertEquals(srcFileMd5, downloadedFileMd5);
    }

    /**
     * test the method
     * {@link DownloadTask#onDownloadFinish(DownloadTask.DownloadFinishListener)}
     * @throws Exception
     */
    @Test
    public void testDownloadFinishListener() throws Exception {
        task.onDownloadFinish(mockDownloadFinishListener)
                .start();
        checkedDownloadSuccess();
        verify(mockDownloadFinishListener, times(1)).onFinish();

        try {
            task.onDownloadFinish(mockDownloadFinishListener)
                    .start();
            fail();
        } catch (Exception e) {
            assertTrue(e.getClass() == IllegalStateException.class);
            assertEquals("please invoke onDownloadFinish() before start()", e.getMessage());
        }
    }

    @Test
    public void testPause() throws Exception {
        Observable.timer(100, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.immediate())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        task.pause();
                    }
                });
        task.start();
        assertTrue(checkedDownloadedFileExists());
        assertFalse(checkFileEqual());
    }

    private boolean checkedDownloadedFileExists(){
        return new File(TEST_DOWNLOAD_FILE_PATH + DOWNLOAD_FILE_NAME).exists();
    }

    private boolean checkFileEqual() {
        File srcFile = new File(TEST_SRC_FILE_PATH);
        File downloadedFile =
                new File(TEST_DOWNLOAD_FILE_PATH + DOWNLOAD_FILE_NAME);
        String srcFileMd5 = Md5Utils.getFileMd5String(srcFile);
        String downloadedFileMd5 = Md5Utils.getFileMd5String(downloadedFile);
        if (srcFileMd5 == null)
            throw new IllegalStateException("src file in test folder is not exists");
        return srcFileMd5.equals(downloadedFileMd5);
    }

    @Test
    public void testContinue() throws Exception {
        testPause();
        task.continueDownload();
        checkedDownloadSuccess();
    }

    @Test
    public void testCancel() throws Exception {
        Observable.timer(100, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.immediate())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        task.cancel();
                    }
                });
        task.start();
        assertFalse(checkedDownloadedFileExists());
        try{
            task.start();
            fail();
        } catch (Exception e) {
            assertTrue(e.getClass() == IllegalStateException.class);
            assertEquals("only clear task can be start", e.getMessage());
        }
        try {
            task.continueDownload();
            fail();
        } catch (Exception e) {
            assertTrue(e.getClass() == IllegalStateException.class);
            assertEquals("only paused task can be continue", e.getMessage());
        }
    }

    @Test
    public void testSelfClone() throws Exception {
        DownloadTask oldTask = task;
        testCancel();
        task = task.selfClone();
        assertFalse(oldTask == task);
        testDownload();
    }

    @Test
    public void testToObservable() throws Exception {
        @SuppressWarnings("unchecked")
        Action1<DownloadInfo> mockOnNextAction = Mockito.mock(Action1.class);
        @SuppressWarnings("unchecked")
        Action1<Throwable> mockOnErrorAction = Mockito.mock(Action1.class);
        Action0 mockOnCompleteAction = Mockito.mock(Action0.class);
        ArgumentCaptor<DownloadInfo> downloadInfoCaptor = ArgumentCaptor.forClass(DownloadInfo.class);
        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);

        task.toObservable()
                .subscribe(mockOnNextAction, mockOnErrorAction, mockOnCompleteAction);
        task.start();
        verify(mockOnNextAction, atLeastOnce()).call(downloadInfoCaptor.capture());
        verify(mockOnCompleteAction, times(1)).call();
        verify(mockOnErrorAction, times(0)).call(throwableCaptor.capture());
        checkedDownloadSuccess();
    }

    @Test
    public void testToObservableData() throws Exception {
        final List<DownloadInfo> infoList = new LinkedList<>();
        task.toObservable()
                .subscribe(new Subscriber<DownloadInfo>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        fail();
                    }

                    @Override
                    public void onNext(DownloadInfo info) {
                        assertTrue(info.getContentLength() >= info.getProgress());
                        assertTrue(info.getContentLength() >= 0);
                        assertTrue(info.getProgress() >= 0);
                        infoList.add(info);
                    }
                });
        task.start();
        assertTrue(infoList.size() > 0);
        DownloadInfo lastInfo = infoList.get(infoList.size() - 1);
        assertTrue(lastInfo.getProgress() == lastInfo.getContentLength());
        checkedDownloadSuccess();
    }

    /**
     * mock server
     */
    private static class MockServer implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            String range = request.header("Range");
            if (range == null) {
                File file = new File(TEST_SRC_FILE_PATH);
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

                File file = new File(TEST_SRC_FILE_PATH);
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
}
