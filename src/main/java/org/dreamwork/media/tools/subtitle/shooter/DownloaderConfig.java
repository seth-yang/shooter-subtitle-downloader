package org.dreamwork.media.tools.subtitle.shooter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: seth.yang
 * Date: 15-4-6
 * Time: 下午9:11
 */
public class DownloaderConfig {
    public static final String KEY_DIR          = "omx.subtitles.dir";
    public static final String KEY_API          = "omx.subtitles.api";
    public static final String KEY_AGENT        = "omx.subtitles.shooter.user-agent";
    public static final String KEY_BOUNDARY     = "omx.subtitles.shooter.boundary";
    public static final String KEY_CONTENT_TYPE = "omx.subtitles.shooter.content-type";

    private static Properties props = null;

    public static void loadConfig (URL url) throws IOException {
        try (InputStream in = url.openStream ()) {
            props = new Properties ();
            props.load (in);
        }
    }

    public static void loadConfig (Properties config) {
        if (props == null) {
            loadDefaultValues ();
        }
        if (config.containsKey (KEY_DIR)) {
            props.setProperty (KEY_DIR, config.getProperty (KEY_DIR));
        }
        if (config.containsKey (KEY_API)) {
            props.setProperty (KEY_API, config.getProperty (KEY_API));
        }
        if (config.containsKey (KEY_AGENT)) {
            props.setProperty (KEY_AGENT, config.getProperty (KEY_AGENT));
        }
        if (config.containsKey (KEY_BOUNDARY)) {
            props.setProperty (KEY_BOUNDARY, config.getProperty (KEY_BOUNDARY));
        }
        if (config.containsKey (KEY_CONTENT_TYPE)) {
            props.setProperty (KEY_CONTENT_TYPE, config.getProperty (KEY_CONTENT_TYPE));
        }
    }

    public static String getString (String name) {
        if (props == null) {
            loadDefaultValues ();
        }
        return props.getProperty (name);
    }

    public static int getIntValue (String name, int defaultValue) {
        String value = getString (name);
        try {
            return Integer.parseInt (value);
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    public static long getLongValue (String name, long defaultValue) {
        try {
            return Long.parseLong (getString (name));
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    public static boolean getBooleanValue (String name, boolean defaultValue) {
        try {
            return Boolean.parseBoolean (getString (name));
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    public static boolean isConfigLoaded () {
        return props != null;
    }

    private static void loadDefaultValues () {
        URL url = DownloaderConfig.class.getClassLoader ().getResource ("jomxplayer-config.properties");
        if (url != null) try {
            loadConfig (url);
        } catch (IOException ex) {
            throw new RuntimeException (ex);
        }
    }
}