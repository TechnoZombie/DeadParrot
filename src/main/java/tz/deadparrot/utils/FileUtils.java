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
            outputFile = new File(Settings.CUSTOM_RECORDINGS_DIRECTORY + Constants.FILENAME_PREFIX + timestamp + Constants.FILENAME_EXTENSION);
        }
        return outputFile;
    }

    public static void openDestinationFolder() {

        String path = null;

        if (Settings.SAVE_RECORDINGS_TO_CUSTOM_DIR) {
            path = Settings.CUSTOM_RECORDINGS_DIRECTORY;

        } else if (Settings.SAVE_RECORDINGS_TO_DESKTOP) {
            path = Settings.OUTPUT_DESKTOP_FOLDER_PATH;
        }

        if (path == null) {
            return;
        }

        File directory = new File(path);

        if (!directory.exists()) {
            log.error("Folder does not exist: {}", path);
            return;
        }

        try {
            Desktop.getDesktop().open(directory);

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
