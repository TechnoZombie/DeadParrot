package tz.deadparrot.utils;

import lombok.extern.slf4j.Slf4j;
import tz.deadparrot.Constants;
import tz.deadparrot.Settings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class FileUtils {
    public static void verifyAndCreateOutputFolder(String dirPath) {
        try {
            Path path = Paths.get(dirPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info(Constants.CREATED_FOLDER);
            } else {
                log.info(Constants.FOLDER_EXISTS);
            }
        } catch (Exception e) {
            log.error(Constants.ERROR_CREATING_FOLDER + e.getMessage());
        }
    }

    public static void detectOS() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains(Constants.LINUX_OS)) {
            log.info(Constants.RUNNING_LINUX);
            Constants.IS_LINUX = true;
            Constants.OUTPUT_DESKTOP_FOLDER_PATH = Constants.LINUX_DESKTOP_PATH;
        } else if (osName.contains(Constants.WINDOWS_OS)) {
            log.info(Constants.RUNNING_WINDOWS);
            Constants.IS_WINDOWS = true;
            Constants.OUTPUT_DESKTOP_FOLDER_PATH = Constants.WINDOWS_DESKTOP_PATH;
        } else {
            Settings.SAVE_RECORDINGS_TO_DESKTOP = false;
        }
    }
}