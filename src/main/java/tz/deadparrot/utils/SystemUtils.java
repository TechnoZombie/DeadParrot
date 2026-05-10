package tz.deadparrot.utils;

import lombok.extern.slf4j.Slf4j;
import tz.deadparrot.Constants;
import tz.deadparrot.Settings;

import java.awt.*;

@Slf4j
public class SystemUtils {

    public static void detectOS(Boolean showLogs) {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains(Constants.LINUX_OS)) {
            if (showLogs) log.info(Constants.RUNNING_LINUX);
            Constants.IS_LINUX = true;
            Settings.OUTPUT_DESKTOP_FOLDER_PATH = Constants.LINUX_DESKTOP_PATH;
        } else if (osName.contains(Constants.WINDOWS_OS)) {
            if (showLogs) log.info(Constants.RUNNING_WINDOWS);
            Constants.IS_WINDOWS = true;
            Settings.OUTPUT_DESKTOP_FOLDER_PATH = Constants.WINDOWS_DESKTOP_PATH;
        } else {
            Settings.SAVE_RECORDINGS_TO_DESKTOP = false;
        }
    }

    public static Dimension getScaledScreenSize(double scale) {
        Rectangle bounds = reportOnScreenSize();
        return new Dimension((int) (bounds.width * scale), (int) (bounds.height * scale));
    }

    private static Rectangle reportOnScreenSize() {
        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice primaryMonitor = ge.getDefaultScreenDevice();

        // Client area (excludes taskbar) — equivalent to monitor.getClientArea()
        final Rectangle rect = ge.getMaximumWindowBounds();

        // Or for full physical bounds — equivalent to monitor.getBounds()
        final Rectangle fullBounds = primaryMonitor.getDefaultConfiguration().getBounds();

        return fullBounds;
    }


}
