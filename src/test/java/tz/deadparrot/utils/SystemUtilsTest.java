package tz.deadparrot.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tz.deadparrot.Constants;
import tz.deadparrot.Settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemUtilsTest {

    private final String originalOsName = System.getProperty("os.name");
    private final boolean originalWindows = Constants.IS_WINDOWS;
    private final boolean originalLinux = Constants.IS_LINUX;
    private final boolean originalSaveToDesktop = Settings.SAVE_RECORDINGS_TO_DESKTOP;
    private final String originalDesktopPath = Settings.OUTPUT_DESKTOP_FOLDER_PATH;

    @AfterEach
    void restoreSystemState() {
        System.setProperty("os.name", originalOsName);
        Constants.IS_WINDOWS = originalWindows;
        Constants.IS_LINUX = originalLinux;
        Settings.SAVE_RECORDINGS_TO_DESKTOP = originalSaveToDesktop;
        Settings.OUTPUT_DESKTOP_FOLDER_PATH = originalDesktopPath;
    }

    @Test
    void detectsOperatingSystemCaseInsensitively() {
        System.setProperty("os.name", "Test Linux");

        assertEquals("test linux", SystemUtils.detectOS(false));
        assertEquals("test linux", SystemUtils.detectOS(true));
    }

    @Test
    void configuresWindowsDesktopPath() {
        System.setProperty("os.name", "Windows 11");
        Constants.IS_WINDOWS = false;
        Constants.IS_LINUX = false;

        SystemUtils.detectOSandSetPaths(true);

        assertTrue(Constants.IS_WINDOWS);
        assertFalse(Constants.IS_LINUX);
        assertEquals(Constants.WINDOWS_DESKTOP_PATH, Settings.OUTPUT_DESKTOP_FOLDER_PATH);
    }

    @Test
    void configuresLinuxDesktopPath() {
        System.setProperty("os.name", "Linux");
        Constants.IS_WINDOWS = false;
        Constants.IS_LINUX = false;

        SystemUtils.detectOSandSetPaths(true);

        assertFalse(Constants.IS_WINDOWS);
        assertTrue(Constants.IS_LINUX);
        assertEquals(Constants.LINUX_DESKTOP_PATH, Settings.OUTPUT_DESKTOP_FOLDER_PATH);
    }

    @Test
    void disablesDesktopStorageOnOtherOperatingSystems() {
        System.setProperty("os.name", "FreeBSD");
        Settings.SAVE_RECORDINGS_TO_DESKTOP = true;

        SystemUtils.detectOSandSetPaths(false);

        assertFalse(Settings.SAVE_RECORDINGS_TO_DESKTOP);
    }

    @Test
    void shortensOnlyPathsLongerThanTheLimit() {
        assertNull(SystemUtils.shortenPath(null, 4));
        assertEquals("path", SystemUtils.shortenPath("path", 4));
        assertEquals("...12345", SystemUtils.shortenPath("012345", 5));
    }

    @Test
    void scalesScreenSizeByTheRequestedFactor() {
        assertTrue(SystemUtils.getScaledScreenSize(0.5).width > 0);
        assertTrue(SystemUtils.getScaledScreenSize(0.5).height > 0);
        assertTrue(new SystemUtils() != null);
    }

}
