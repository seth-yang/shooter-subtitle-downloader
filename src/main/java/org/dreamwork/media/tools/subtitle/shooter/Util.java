package org.dreamwork.media.tools.subtitle.shooter;

import org.dreamwork.util.StringUtil;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.NoSuchAlgorithmException;

/**
 * Created with IntelliJ IDEA.
 * User: seth.yang
 * Date: 15-4-6
 * Time: 下午9:03
 */
public class Util {
    public static String getFileHash (File movie) throws IOException, NoSuchAlgorithmException {
        RandomAccessFile raf = new RandomAccessFile (movie, "r");
        try {
            long length = movie.length ();
            int blockSize = 4096;

            long[] offsets = new long[4];
            byte[] buff = new byte[blockSize];

            offsets [3] = length - 8192;
            offsets [2] = length / 3;
            offsets [1] = length / 3 * 2;
            offsets [0] = blockSize;

            StringBuilder builder = new StringBuilder ();
            for (long offset : offsets) {
                raf.seek (offset);
                int len = raf.read (buff);
                byte[] hash = org.dreamwork.misc.AlgorithmUtil.md5 (buff);
                if (builder.length () > 0) {
                    builder.append (';');
                }
                builder.append (StringUtil.byte2hex (hash, false));
            }

            return builder.toString ();
        } finally {
            raf.close ();
        }
    }
}