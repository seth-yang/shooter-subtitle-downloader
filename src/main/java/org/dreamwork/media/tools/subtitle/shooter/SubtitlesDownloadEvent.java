package org.dreamwork.media.tools.subtitle.shooter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EventObject;

/**
 * Created with IntelliJ IDEA.
 * User: seth.yang
 * Date: 15-4-6
 * Time: 下午9:35
 */
public class SubtitlesDownloadEvent extends EventObject {
    private Path subtitle;

    public SubtitlesDownloadEvent (Path subtitle) {
        super (subtitle);
        this.subtitle = subtitle;
    }

    public Path getSubtitle () {
        return subtitle;
    }

    void setSubtitle (Path subtitle) {
        this.subtitle = subtitle;
    }

    @Override
    public String toString () {
        try {
            return subtitle == null ? "<null>" : subtitle.toRealPath ().toString ();
        } catch (IOException ex) {
            return "";
        }
    }
}