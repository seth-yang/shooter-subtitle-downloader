# shooter-subtitle-downloader

#### 介绍
射手网中文字幕下载工具

简单上手：
```java
public class DownloaderTest {
    public static void main (String[] args) throws IOException, NoSuchAlgorithmException {
        String file = "/movies/Frozen.2013.1080p.BluRay.DTS-HD.MA.7.1.x264-PublicHD.mkv";
        ShooterDownloader downloader = new ShooterDownloader ();
        downloader.addSubtitlesDownloadListener (System.err::println);
        downloader.download (file);
    }
}
```
下载的字幕文件将保存在和电影文件相同的目录下(**如果该目录对当前用户可写**)，或保存在系统的临时目录下。

您也可以自定义字幕文件的保存路径，只要简单的在运行程序是加入系统参数 `-Domx.subtitles.dir=${PATH-TO-SUBTITLES}` 即可，或在代码中进行配置

```java
public class DownloaderTest {
    public static void main (String[] args) throws IOException, NoSuchAlgorithmException {
        System.setProperty ("omx.subtitles.dir", "../subtitles/");
        String file = "/movies/Frozen.2013.1080p.BluRay.DTS-HD.MA.7.1.x264-PublicHD.mkv";
        ShooterDownloader downloader = new ShooterDownloader ();
        List<Path> subtitles = downloader.download (file);
        for (Path subtitle : subtitles) {
            // do something to process the subtitles
        }
    }
}
```