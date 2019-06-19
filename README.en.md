# shooter-subtitle-downloader

A java library (known as .jar) downloads subtitles from shooter automatically.

Quick Start:
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

The downloaded subtitles will be saved in the same directory as the movie file (
**if the directory is writable to the current user**) or saved in the system's temporary directory.

You can also customize the save path of the subtitle file by simply adding the
JVM option `-Domx.subtitles.dir=${PATH-TO-SUBTITLES}` or configuring it in the code.

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