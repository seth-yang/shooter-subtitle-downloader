package org.dreamwork.media.tools.subtitle.shooter;

import org.dreamwork.util.FileInfo;
import org.dreamwork.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static org.dreamwork.media.tools.subtitle.shooter.DownloaderConfig.*;
import static java.nio.file.StandardOpenOption.*;

/**
 * Created with IntelliJ IDEA.
 * User: seth.yang
 * Date: 15-4-6
 * Time: 下午9:02
 */
public class ShooterDownloader {
    private static final Logger logger = LoggerFactory.getLogger (ShooterDownloader.class);
    private static final String KEY_TMP_DIR = "java.io.tmpdir";

    private List<ISubtitlesDownloadListener> listeners
            = Collections.synchronizedList (new ArrayList<> ());

    private static String CONTENT_TYPE, BOUNDARY, USER_AGENT, API_URL;
    private static Path SUBTITLES_DIR;

    public void addSubtitlesDownloadListener (ISubtitlesDownloadListener listener) {
        listeners.add (listener);
    }

    public boolean ass2srt (File ass) throws IOException, InterruptedException {
        Process process = new ProcessBuilder (
                "asstosrt",
                "-n", "-f",
                "-o", SUBTITLES_DIR.toRealPath ().toString (),
                ass.getCanonicalPath ()
        ).start ();
        return process.waitFor () == 0;
    }

    public List<Path> download (String movie) throws IOException, NoSuchAlgorithmException {
        FileSystem fs = FileSystems.getDefault ();
        Path path = fs.getPath (movie);
        if (Files.notExists (path)) {
            logger.error ("movie {} not found!", movie);
            throw new IllegalArgumentException ("movie not found!");
        }

        patchWorkEnv (path);

        URL url = new URL (API_URL);
        if (logger.isTraceEnabled ()) {
            logger.trace ("connecting to " + url);
        }
        HttpURLConnection conn = (HttpURLConnection) url.openConnection ();
        try {
            conn.setDoInput (true);
            conn.setDoOutput (true);

            conn.setRequestMethod ("POST");
            conn.addRequestProperty ("User-Agent", USER_AGENT);
            conn.addRequestProperty ("Content-Type", CONTENT_TYPE);

            OutputStream out = conn.getOutputStream ();
            PrintWriter pw = new PrintWriter (out, true);

            String hash = Util.getFileHash (path.toFile ());

            pw.printf ("--%s\r\n", BOUNDARY);
            pw.printf ("Content-Disposition: form-data; name=\"filehash\"\r\n\r\n%s\r\n", hash);
            pw.printf ("--%s--\r\n", BOUNDARY);
            pw.flush ();
            out.flush ();

            if (logger.isTraceEnabled ()) {
                logger.trace ("movie hash: {}", hash);
            }

            InputStream in = conn.getInputStream ();
            byte count = (byte) in.read ();
            if (logger.isTraceEnabled ()) {
                logger.trace ("there's " + count + " subtitle packages");
            }

            if (count == -1) {
                System.err.println ("subtitles not found");
            } else if (count < 0) {
                System.err.println ("error");
            } else {
                List<Path> subtitles = new ArrayList<> ();

                for (int i = 0; i < count; i ++) {
                    handle (path, in, subtitles, i);
                }

                return subtitles;
            }

            return null;
        } finally {
            conn.disconnect ();
        }
    }

    private void patchWorkEnv (Path movie) throws IOException {
        if (API_URL == null) {
            DownloaderConfig.loadConfig (System.getProperties ());

            API_URL         = DownloaderConfig.getString (KEY_API);
            USER_AGENT      = DownloaderConfig.getString (DownloaderConfig.KEY_AGENT);
            BOUNDARY        = DownloaderConfig.getString (DownloaderConfig.KEY_BOUNDARY);
            CONTENT_TYPE    = DownloaderConfig.getString (DownloaderConfig.KEY_CONTENT_TYPE);
        }

        String workDir = DownloaderConfig.getString (DownloaderConfig.KEY_DIR);
        Path path = null;
        FileSystem fs = FileSystems.getDefault ();

        if (!StringUtil.isEmpty (workDir)) {
            path = fs.getPath (workDir);
            if (Files.notExists (path)) {
                try {
                    Files.createDirectories (path);
                } catch (IOException ex) {
                    logger.warn ("can't create directory {}", path.toString ());
                    
                    path = null;
                }
            }
        }

        if (path == null) {
            if (logger.isTraceEnabled ()) {
                logger.trace ("the work dir not set, using the same dir to the movie.");
            }

            path = movie.getParent ();
            if (!Files.isWritable (path)) {
                logger.warn ("the dir {} can not write.", path.toString ());

                path = null;
            }
        }

        if (path == null) {
            if (logger.isTraceEnabled ()) {
                logger.trace ("using system tmp dir to store downloaded subtitles");
            }
            path = fs.getPath (System.getProperty (KEY_TMP_DIR));
        }

        SUBTITLES_DIR = path;

        if (logger.isTraceEnabled ()) {
            logger.trace ("API_URL = " + API_URL);
            logger.trace ("USER_AGENT = " + USER_AGENT);
            logger.trace ("BOUNDARY = " + BOUNDARY);
            logger.trace ("CONTENT_TYPE = " + CONTENT_TYPE);
            logger.trace ("SUBTITLES_DIR = " + SUBTITLES_DIR.toRealPath ());
        }
    }

    private void handle (Path movie, InputStream in, List<Path> subtitles, int index) throws IOException {
        DataInputStream dis = new DataInputStream (in);
        int packageLength = dis.readInt ();
        int descLength = dis.readInt ();
        byte[] buff = new byte[descLength];
        int length = in.read (buff);
        if (length != buff.length) {
            throw new IOException ("expect for " + descLength + " bytes, but read " + length);
        }
        int fileDataLength = dis.readInt ();
        byte fileCount = (byte) in.read ();

        if (logger.isDebugEnabled ()) {
            logger.debug ("package #" + index);
            logger.debug ("\t      length: " + packageLength);
            logger.debug ("\t        desc: " + new String (buff));
            logger.debug ("\t file length: " + fileDataLength);
            logger.debug ("\t  file count: " + fileCount);
        }

        for (int i = 0; i < fileCount; i ++) {
            processFile (movie, dis, index, subtitles, i);
        }
    }

    private void processFile (Path movie, DataInputStream in, int packageIndex, List<Path> subtitles, int index) throws IOException {
        int singlePackageLength = in.readInt ();
        int extLength = in.readInt ();
        byte[] tmp = new byte[extLength];
        int length = in.read (tmp);
        String ext = new String (tmp, 0, length, StandardCharsets.UTF_8);

        int fileLength = in.readInt (), LENGTH = 4096, leftToRead = fileLength, read, needToRead;
        byte[] buff = new byte[LENGTH];

        String fileName = String.valueOf (Math.random ()).substring (2);
        Path file = movie.getFileSystem ().getPath (SUBTITLES_DIR.toString (), fileName);
        OutputStream fos = Files.newOutputStream (file, CREATE, WRITE, TRUNCATE_EXISTING);

        if (logger.isTraceEnabled ()) {
            logger.trace (" package length: {}", singlePackageLength);
            logger.trace ("    file length: {}", fileLength);
            logger.trace ("           type: {}", ext);
            logger.trace ("     file index: {}.{}", packageIndex, index);
            logger.trace (" temp file name: {}", file.toAbsolutePath ());
        }

        boolean first = true, zipped = false;
        try {
            do {
                needToRead = Math.min (LENGTH, leftToRead);
                read = in.read (buff, 0, needToRead);
                if (read <= 0) break;

                if (first) {
                    int a = buff [0] & 0xff,
                            b = buff [1] & 0xff,
                            c = buff [2] & 0xff;
                    zipped = (a == 0x1f) && (b == 0x8b) && (c == 0x08);
                    if (logger.isTraceEnabled ()) {
                        logger.trace ("        zipped : {}", zipped);
                    }
                }

                fos.write (buff, 0, read);
                fos.flush ();
                leftToRead -= read;

                first = false;
            } while (leftToRead > 0);
        } finally {
            fos.flush ();
            fos.close ();
        }

        if (zipped) {
            file = unGzip (file);
        }

        fileName = FileInfo.getFileNameWithoutExtension (movie.toString ());
        Path target = FileSystems.getDefault ().getPath (SUBTITLES_DIR.toString (), fileName + "." + ext);
        int count = 1;
        while (Files.exists (target)) {
            target = FileSystems.getDefault ().getPath (SUBTITLES_DIR.toString (), fileName + "." + count + "." + ext);
        }

        Files.move (file, target);
        SubtitlesDownloadEvent event = new SubtitlesDownloadEvent (target);
        EncodingTranslator translator = new EncodingTranslator ();
        target = translator.translate (target, "UTF-8");
        subtitles.add (target);
        event.setSubtitle (target);
        for (ISubtitlesDownloadListener listener : listeners) {
            listener.onSubtitlesDownloaded (event);
        }
    }

    private Path unGzip (Path file) throws IOException {
        String fileName = String.valueOf (Math.random ()).substring (2);
        try (InputStream fis = Files.newInputStream (file, READ)) {
            Path parent = file.getParent ();
            Path tmp = FileSystems.getDefault ().getPath (parent.toString (), fileName);
            try (OutputStream fos = Files.newOutputStream (tmp, CREATE, WRITE)) {
                GZIPInputStream zip = new GZIPInputStream (fis);
                byte[] buff = new byte[(int) Math.min (file.toFile ().length (), 40960)];
                int length;
                while ((length = zip.read (buff)) != -1) {
                    fos.write (buff, 0, length);
                    fos.flush ();
                }

                Files.delete (file);
            }

            return tmp;
        }
    }
}
