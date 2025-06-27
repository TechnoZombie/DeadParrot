package tz.deadparrot.utils;

import lombok.extern.slf4j.Slf4j;
import tz.deadparrot.Constants;

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
}
