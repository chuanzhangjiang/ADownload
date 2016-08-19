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
整个创建过程只有url是必须的，如果不设置'downloadPath'文件将默认保存到android自带的download目录。如果不是设置'filename'将默认从链接
中截取文件名。如果不设置'interceptor'，整个下载过程将不使用任何拦截器。
* **使用DownloadTask**

如果你不需要在下载过程中执行任何监听操作，那你可以直接这样：
```
task.start();
```
执行'start()'之后DownloadTask将会开始执行下载操作。
在下载过程中你可以取消任务或是暂停任务，如下：
```
task.cancel();
task.pause();
```
在任务被暂停之后，如果想要让它继续的话：
```
task.continue();
```
任务将会继续执行（注意是'continue()'，不是'start()'）。<br>
如果任务是被取消了（'cancel()'）那么你将不能在调用'start()'或是'continue()'方法来让任务再次或是继续执行。<br>
如果你取消了任务，但是又想让任务重新执行，你不需要再次配置一个一模一样的DownloadTask, 这里提供了一个clone方法来提供一个新的，
并且跟之前一模一样的任务：
```
DownloadTask newTask = task.selfClone();
```
