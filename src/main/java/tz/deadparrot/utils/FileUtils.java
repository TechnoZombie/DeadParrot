package tz.deadparrot.utils;

import lombok.extern.slf4j.Slf4j;
import tz.deadparrot.Constants;
import tz.deadparrot.Settings;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

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
            log.error(Constants.ERROR_CREATING_FOLDER + "{}", e.getMessage());
        }
    }

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

    public static File generateOutputFile() {
        File outputFile;

        if (Settings.KEEP_RECORDINGS) {
            outputFile = FileUtils.checkSaveDestinationAndReturnFile();
        } else {
            // Needed for temp audio recording for playback
            outputFile = new File(Constants.OUTPUT_TEMP_FILE_NAME);
        }
        return outputFile;
    }

    private static File checkSaveDestinationAndReturnFile() {

        String timestamp = LocalDateTime.now().format(Constants.TIMESTAMP_FORMAT);
        File outputFile = null;

        if (Settings.SAVE_RECORDINGS_TO_DESKTOP) {
            outputFile = new File(Settings.OUTPUT_DESKTOP_FOLDER_PATH + Constants.FILENAME_PREFIX + timestamp + Constants.FILENAME_EXTENSION);
        } else if (Settings.SAVE_RECORDINGS_TO_CUSTOM_DIR) {
            verifyAndCreateOutputFolder(Settings.CUSTOM_RECORDINGS_DIRECTORY);
            outputFile = new File(Settings.CUSTOM_RECORDINGS_DIRECTORY + Constants.FILENAME_PREFIX + timestamp + Constants.FILENAME_EXTENSION);
        }
        return outputFile;
    }

    public static void openDestinationFolder() {

        String path = null;

        if (Settings.SAVE_RECORDINGS_TO_CUSTOM_DIR) {
            path = Settings.CUSTOM_RECORDINGS_DIRECTORY + Constants.CUSTOM_RECORDINGS_DIRECTORY_PREFIX;

        } else if (Settings.SAVE_RECORDINGS_TO_DESKTOP) {
            path = Settings.OUTPUT_DESKTOP_FOLDER_PATH;
        }

        if (path == null) {
            return;
        }

        File directory = new File(path);

        if (!directory.exists()) {
            return;
        }

        try {
            Desktop.getDesktop().open(directory);

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
