# ADownload
这是一个实现Android文件下载的库，它基于[Retrofit2.0](http://square.github.io/retrofit/)和[RxJava](https://github.com/ReactiveX/RxJava)。
## 介绍
ADownload支持多文件同时下载，支持文件断点续传，并且使用方便。
## 使用
暂时没有将项目发布到jcenter，所以需要使用的话可以将项目下载或是clone到你的电脑上，然后导入Module。
* **创建DownloadTask**<br>

由于整个项目只导出了一个类（DownloadTask），所以只需要掌握这个类的使用。
DownloadTask使用建造者模式来创建，如下：
```
DownloadTask task = new DownloadTask
                .DownloadBuilder(url)
                .setPath(downloadPath)
                .setInterceptors(interceptor)
                .setFileName(filename)
                .build();
```
整个创建过程只有url是必须的，如果不设置downloadPath文件将默认保存到android自带的download目录。如果不是设置filename将默认从链接
中截取文件名。如果不设置interceptor，整个下载过程将不使用任何拦截器。
* **使用DownloadTask**

如果你不需要在下载过程中执行任何监听操作，那你可以直接这样：
```
task.start();
```
执行start()之后DownloadTask将会开始执行下载操作。
在下载过程中你可以取消任务或是暂停任务，如下：
```
task.cancel();
task.pause();
```
在任务被暂停之后，如果想要让它继续的话：
```
task.continueDownload();
```
任务将会继续执行（continueDownload()，不是start()）。<br>
如果任务是被取消了（cancel()）那么你将不能在调用start()或continueDownload()方法来让任务再次或是继续执行。<br>
如果你取消了任务，但是又想让任务重新执行，你不需要再次配置一个一模一样的DownloadTask, 这里提供了一个clone方法来提供一个新的，
并且跟之前一模一样的任务：
```
DownloadTask newTask = task.selfClone();
```
* **对下载过程进行监听**

下载进度是可以被监听的，DownloadTask中有一个toObservable()方法，具体操作如下：
```
task.start()
          .toObservable()
          .observeOn(Schedulers.immediate())
          .subscribe(new Action1<Long>() {
                @Override
                public void call(Long aLong) {
                    //监听下载进度
                    //aLong是当前进度
                }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                });
```
获取下载内容大小：
```
//方法1
task.getContentLength(); //注意这个方法只能在下载开始之后才能被调用。

//方法2
task.onGetContentLength(new DownloadTask.ContentLengthListener() {
                    @Override
                    public void onGet(long contentLength) {
                        //contentLength就是下载内容的大小
                        //第一时间得到下载内容的大小
                    }
                })
```
所有这些可以串起来用：
```
new DownloadTask
                .DownloadBuilder(url)
                .setPath(path)
                .setInterceptors(interceptor)
                .setFileName(filename)
                .build()
                .start()
                .onGetContentLength(new DownloadTask.ContentLengthListener() {
                    @Override
                    public void onGet(long contentLength) {

                    }
                })
                .toObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Long>() {
                    @Override
                    public void onCompleted() {
                        
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Long aLong) {

                    }
                });
```
* **设置拦截器**

这里的拦截器使用的是Okhttp中的拦截器（目前只能使用应用拦截器，暂不支持网络拦截器），设置拦截器的方式如下：
```
new DownloadTask
                .DownloadBuilder(url)
                .setInterceptors(interceptor);//设置拦截器的方法
```
你也可以同时使用多个拦截器：
```
new DownloadTask
                .DownloadBuilder(url)
                .setInterceptors(interceptor0, interceptor1, interceptor2);//设置多个拦截器
```

## TODO
* 使DownloadTask可以被序列化，实现应用退出之后还可以继续上次的任务下载
* 暴露一个下载管理器，辅助实现序列化和下载任务的管理

## License
> Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  > http://www.apache.org/licenses/LICENSE-2.0

> Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
