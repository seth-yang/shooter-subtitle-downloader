package org.dreamwork.media.tools.subtitle.shooter;

import org.dreamwork.util.FileInfo;

import java.io.File;
import java.io.FileFilter;

/**
 * Created with IntelliJ IDEA.
 * User: seth.yang
 * Date: 15-4-8
 * Time: 下午11:56
 */
public class SubtitlesFilter implements FileFilter {
    private String baseName;

    public SubtitlesFilter (File movie) {
        this (movie.getName ());
    }

    public SubtitlesFilter (String movieName) {
        baseName = FileInfo.getFileNameWithoutExtension (movieName);
    }

    @Override
    public boolean accept (File file) {
        String fileName = FileInfo.getFileNameWithoutExtension (file.getName ());
        if (!fileName.startsWith (baseName)) {
            return false;
        }

        fileName = fileName.toLowerCase ();
        return fileName.endsWith (".srt") || fileName.endsWith (".ass");
    }
}