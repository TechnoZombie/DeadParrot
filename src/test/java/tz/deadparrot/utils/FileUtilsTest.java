package tz.deadparrot.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tz.deadparrot.Constants;
import tz.deadparrot.Settings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileUtilsTest {

    @TempDir
    Path tempDir;

    private final boolean originalKeepRecordings = Settings.KEEP_RECORDINGS;
    private final boolean originalSaveToDesktop = Settings.SAVE_RECORDINGS_TO_DESKTOP;
    private final boolean originalSaveToCustom = Settings.SAVE_RECORDINGS_TO_CUSTOM_DIR;
    private final String originalDesktopPath = Settings.OUTPUT_DESKTOP_FOLDER_PATH;
    private final String originalCustomPath = Settings.CUSTOM_RECORDINGS_DIRECTORY;
    private final FileUtils.DirectoryOpener originalDirectoryOpener = FileUtils.directoryOpener;

    @AfterEach
    void restoreSettings() {
        Settings.KEEP_RECORDINGS = originalKeepRecordings;
        Settings.SAVE_RECORDINGS_TO_DESKTOP = originalSaveToDesktop;
        Settings.SAVE_RECORDINGS_TO_CUSTOM_DIR = originalSaveToCustom;
        Settings.OUTPUT_DESKTOP_FOLDER_PATH = originalDesktopPath;
        Settings.CUSTOM_RECORDINGS_DIRECTORY = originalCustomPath;
        FileUtils.directoryOpener = originalDirectoryOpener;
    }

    @Test
    void createsAndReusesOutputDirectory() {
        Path outputDirectory = tempDir.resolve("recordings");

        FileUtils.verifyAndCreateOutputFolder(outputDirectory.toString());
        FileUtils.verifyAndCreateOutputFolder(outputDirectory.toString());

        assertTrue(Files.isDirectory(outputDirectory));
    }

    @Test
    void logsInvalidOutputDirectoryWithoutThrowing() {
        FileUtils.verifyAndCreateOutputFolder("\0");
    }

    @Test
    void selectsTemporaryDesktopAndCustomRecordingFiles() {
        Settings.KEEP_RECORDINGS = false;
        assertEquals(new File(Constants.OUTPUT_TEMP_FILE_NAME), FileUtils.generateOutputFile());

        Settings.KEEP_RECORDINGS = true;
        Settings.SAVE_RECORDINGS_TO_DESKTOP = true;
        Settings.OUTPUT_DESKTOP_FOLDER_PATH = tempDir + File.separator;
        assertRecordingFile(FileUtils.generateOutputFile(), tempDir);

        Settings.SAVE_RECORDINGS_TO_DESKTOP = false;
        Settings.SAVE_RECORDINGS_TO_CUSTOM_DIR = true;
        Settings.CUSTOM_RECORDINGS_DIRECTORY = tempDir + File.separator;
        assertRecordingFile(FileUtils.generateOutputFile(), tempDir);

        Settings.SAVE_RECORDINGS_TO_CUSTOM_DIR = false;
        assertNull(FileUtils.generateOutputFile());
    }

    @Test
    void safelyReturnsWhenNoDestinationOrMissingFolderIsConfigured() {
        Settings.SAVE_RECORDINGS_TO_DESKTOP = false;
        Settings.SAVE_RECORDINGS_TO_CUSTOM_DIR = false;
        FileUtils.openDestinationFolder();

        Settings.SAVE_RECORDINGS_TO_CUSTOM_DIR = true;
        Settings.CUSTOM_RECORDINGS_DIRECTORY = tempDir.resolve("missing").toString();
        FileUtils.openDestinationFolder();

        assertFalse(Files.exists(Path.of(Settings.CUSTOM_RECORDINGS_DIRECTORY)));
        assertNotNull(new FileUtils());
    }

    @Test
    void opensConfiguredDirectoryAndHandlesDesktopErrors() throws IOException {
        Path directory = Files.createDirectory(tempDir.resolve("open"));
        Settings.SAVE_RECORDINGS_TO_CUSTOM_DIR = true;
        Settings.CUSTOM_RECORDINGS_DIRECTORY = directory.toString();
        FileUtils.DirectoryOpener opener = FileUtils.directoryOpener;
        try {
            FileUtils.directoryOpener = file -> assertEquals(directory.toFile(), file);
            FileUtils.openDestinationFolder();
            FileUtils.directoryOpener = file -> {
                throw new IOException("desktop unavailable");
            };
            FileUtils.openDestinationFolder();
        } finally {
            FileUtils.directoryOpener = opener;
        }
    }

    @Test
    void opensConfiguredDesktopDirectory() throws IOException {
        Path directory = Files.createDirectory(tempDir.resolve("desktop"));
        Settings.SAVE_RECORDINGS_TO_CUSTOM_DIR = false;
        Settings.SAVE_RECORDINGS_TO_DESKTOP = true;
        Settings.OUTPUT_DESKTOP_FOLDER_PATH = directory.toString();
        FileUtils.DirectoryOpener opener = FileUtils.directoryOpener;
        try {
            FileUtils.directoryOpener = file -> assertEquals(directory.toFile(), file);
            FileUtils.openDestinationFolder();
        } finally {
            FileUtils.directoryOpener = opener;
        }
    }

    private void assertRecordingFile(File file, Path expectedDirectory) {
        assertTrue(file.toPath().getParent().equals(expectedDirectory));
        assertTrue(file.getName().matches(
                Constants.FILENAME_PREFIX + "\\d{2}-\\d{2}-\\d{4}_\\d{2}\\.\\d{2}\\.\\d{2}"
                        + Constants.FILENAME_EXTENSION.replace(".", "\\.")));
    }
}
