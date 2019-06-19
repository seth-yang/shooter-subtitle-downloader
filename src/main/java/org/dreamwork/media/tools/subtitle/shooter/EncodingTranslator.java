package org.dreamwork.media.tools.subtitle.shooter;

import org.dreamwork.util.FileInfo;
import org.mozilla.intl.chardet.nsDetector;
import org.mozilla.intl.chardet.nsICharsetDetectionObserver;
import org.mozilla.intl.chardet.nsPSMDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Created with IntelliJ IDEA.
 * User: seth.yang
 * Date: 15-4-10
 * Time: 上午12:20
 */
public class EncodingTranslator implements nsICharsetDetectionObserver {
    private boolean found;
    private String charset;

    private final Logger logger = LoggerFactory.getLogger (EncodingTranslator.class);

    @Override
    public void Notify (String charset) {
        if (logger.isTraceEnabled ()) {
            logger.trace ("******** notified charset = {}", charset);
        }
        this.charset = charset;
    }

    public void detectEncoding (InputStream in) throws IOException {
        nsDetector detector = new nsDetector (nsPSMDetector.ALL);
        detector.Init (this);
        try {
            byte[] buff = new byte[1024];
            boolean isAscii = true;
            int length;
            while ((length = in.read (buff)) != -1) {
                if (isAscii)
                    isAscii = detector.isAscii (buff, length);
                if (!isAscii)
                    if (detector.DoIt (buff, length, false)) {
                        break;
                    }
            }
            if (isAscii) {
                found = true;
                charset = "ISO-8859-1";
            } else if (!found) {
                String[] a = detector.getProbableCharsets ();
                if (logger.isTraceEnabled ()) {
                    logger.trace ("detect charset count = {}", a.length);
                }

                if (a.length > 0) {
                    if (logger.isTraceEnabled ()) {
                        for (int i = 0; i < a.length; i++) {
                            logger.trace ("\t probable charsets [{}] = {}", i, a[i]);
                        }
                    }

                    charset = a[0];
                }
            }
        } finally {
            in.close ();
        }
    }

    public void detectEncoding (Path path) throws IOException {
        detectEncoding (Files.newInputStream (path));
    }

    public void detectEncoding (File file) throws IOException {
        detectEncoding (new FileInputStream (file));
    }

    public void detectEncoding (URL url) throws IOException {
        detectEncoding (url.openStream ());
    }

    public void detectEncoding (byte[] buff) throws IOException {
        detectEncoding (new ByteArrayInputStream (buff));
    }

    public void translate (File src, OutputStream out, String targetEncoding) throws IOException {
        this.detectEncoding (src);
        translate (new FileInputStream (src), charset, out, targetEncoding);
    }

    public void translate (byte[] buff, OutputStream out, String targetEncoding) throws IOException {
        this.detectEncoding (buff);
        translate (new ByteArrayInputStream (buff), charset, out, targetEncoding);
    }

    public void translate (InputStream in, String origEncoding, OutputStream out, String targetEncoding) throws IOException {
        Reader reader = new InputStreamReader (in, origEncoding);
        Writer writer = new OutputStreamWriter (out, targetEncoding);
        try {
            char[] buff = new char[2048];
            int length;
            while ((length = reader.read (buff)) != -1) {
                writer.write (buff, 0, length);
                writer.flush ();
                out.flush ();
            }
        } finally {
            in.close ();
            out.flush ();
            out.close ();
        }
    }

    public void translate (InputStream in, Charset origCharset, OutputStream out, Charset targetCharset) throws IOException {
        translate (in, origCharset.toString (), out, targetCharset.toString ());
    }

    public void translate (File src, File to, String encoding) throws IOException {
        OutputStream fos = new FileOutputStream (to);
        translate (src, fos, encoding);
    }

    public Path translate (Path src, StandardCharsets charset) throws IOException {
        return translate (src, charset.toString ());
    }

    public Path translate (Path src, String encoding) throws IOException {
        String fileName = src.toString ();
        String name = FileInfo.getFileNameWithoutExtension (fileName);
        String ext = FileInfo.getExtension (fileName);
        detectEncoding (src);
        if (logger.isTraceEnabled ()) {
            logger.trace ("source file: {} with encoding {} will be translate to {}", src.toAbsolutePath (), charset, encoding);
        }

        Path target = FileSystems.getDefault ().getPath (src.getParent ().toString (), name + "." + charset + "." + ext);
        translate (Files.newInputStream (src), charset, Files.newOutputStream (target, StandardOpenOption.CREATE), encoding);
        Files.delete (src);
        return target;
    }
}
