package tz.deadparrot.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tz.deadparrot.Settings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsPersistenceTest {

    @TempDir
    Path tempDir;

    private final boolean originalSpyMode = Settings.SPY_MODE;
    private final boolean originalMarkerMode = Settings.MARKER_MODE;
    private final boolean originalKeepRecordings = Settings.KEEP_RECORDINGS;
    private final boolean originalPrintSettings = Settings.PRINT_SETTINGS;
    private final boolean originalDarkMode = Settings.DARK_MODE;
    private final boolean originalOpenSettings = Settings.OPEN_OS_RECORDING_SETTINGS;
    private final boolean originalSaveToDesktop = Settings.SAVE_RECORDINGS_TO_DESKTOP;
    private final boolean originalSaveToCustom = Settings.SAVE_RECORDINGS_TO_CUSTOM_DIR;
    private final boolean originalEasterEgg = Settings.EASTER_EGG;
    private final String originalInputDevice = Settings.SELECTED_INPUT_DEVICE;
    private final String originalOutputDevice = Settings.SELECTED_OUTPUT_DEVICE;
    private final String originalAudioFormat = Settings.SELECTED_AUDIO_FORMAT;

    @AfterEach
    void restoreSettings() {
        Settings.SPY_MODE = originalSpyMode;
        Settings.MARKER_MODE = originalMarkerMode;
        Settings.KEEP_RECORDINGS = originalKeepRecordings;
        Settings.PRINT_SETTINGS = originalPrintSettings;
        Settings.DARK_MODE = originalDarkMode;
        Settings.OPEN_OS_RECORDING_SETTINGS = originalOpenSettings;
        Settings.SAVE_RECORDINGS_TO_DESKTOP = originalSaveToDesktop;
        Settings.SAVE_RECORDINGS_TO_CUSTOM_DIR = originalSaveToCustom;
        Settings.EASTER_EGG = originalEasterEgg;
        Settings.SELECTED_INPUT_DEVICE = originalInputDevice;
        Settings.SELECTED_OUTPUT_DEVICE = originalOutputDevice;
        Settings.SELECTED_AUDIO_FORMAT = originalAudioFormat;
    }

    @Test
    void savesAndLoadsAllSettings() throws IOException {
        Settings.SPY_MODE = true;
        Settings.MARKER_MODE = true;
        Settings.KEEP_RECORDINGS = true;
        Settings.PRINT_SETTINGS = true;
        Settings.DARK_MODE = true;
        Settings.OPEN_OS_RECORDING_SETTINGS = true;
        Settings.SAVE_RECORDINGS_TO_DESKTOP = false;
        Settings.SAVE_RECORDINGS_TO_CUSTOM_DIR = true;
        Settings.EASTER_EGG = true;
        Settings.SELECTED_INPUT_DEVICE = "Microphone";
        Settings.SELECTED_OUTPUT_DEVICE = "Speakers";
        Settings.SELECTED_AUDIO_FORMAT = "48000 Hz, 24-bit, 2 ch";

        Path settingsFile = tempDir.resolve("deadparrot.settings");
        SettingsPersistence.saveSettings(settingsFile);
        Properties saved = new Properties();
        try (var input = Files.newInputStream(settingsFile)) {
            saved.load(input);
        }
        assertTrue(saved.containsKey("selectedInputDevice"));
        assertTrue(saved.containsKey("selectedOutputDevice"));
        assertTrue(saved.containsKey("selectedAudioFormat"));

        Settings.SPY_MODE = false;
        Settings.MARKER_MODE = false;
        Settings.KEEP_RECORDINGS = false;
        Settings.PRINT_SETTINGS = false;
        Settings.DARK_MODE = false;
        Settings.OPEN_OS_RECORDING_SETTINGS = false;
        Settings.SAVE_RECORDINGS_TO_DESKTOP = true;
        Settings.SAVE_RECORDINGS_TO_CUSTOM_DIR = false;
        Settings.EASTER_EGG = false;

        SettingsPersistence.loadSettings(settingsFile);

        assertTrue(Settings.SPY_MODE);
        assertTrue(Settings.MARKER_MODE);
        assertTrue(Settings.KEEP_RECORDINGS);
        assertTrue(Settings.PRINT_SETTINGS);
        assertTrue(Settings.DARK_MODE);
        assertTrue(Settings.OPEN_OS_RECORDING_SETTINGS);
        assertFalse(Settings.SAVE_RECORDINGS_TO_DESKTOP);
        assertTrue(Settings.SAVE_RECORDINGS_TO_CUSTOM_DIR);
        assertTrue(Settings.EASTER_EGG);
    }

    @Test
    void logsAndContinuesWhenSettingsFileCannotBeWrittenOrRead() {
        Path directoryInsteadOfFile = tempDir;

        SettingsPersistence.saveSettings(directoryInsteadOfFile);
        SettingsPersistence.loadSettings(directoryInsteadOfFile);
    }

    @Test
    void appliesDefaultsForSettingsMissingFromFile() throws IOException {
        Path settingsFile = tempDir.resolve("partial.settings");
        Properties properties = new Properties();
        properties.setProperty("spyMode", "true");
        try (var output = Files.newOutputStream(settingsFile)) {
            properties.store(output, "partial settings");
        }

        SettingsPersistence.loadSettings(settingsFile);

        assertTrue(Settings.SPY_MODE);
        assertFalse(Settings.MARKER_MODE);
        assertFalse(Settings.KEEP_RECORDINGS);
        assertFalse(Settings.PRINT_SETTINGS);
        assertFalse(Settings.DARK_MODE);
        assertFalse(Settings.OPEN_OS_RECORDING_SETTINGS);
        assertTrue(Settings.SAVE_RECORDINGS_TO_DESKTOP);
        assertFalse(Settings.SAVE_RECORDINGS_TO_CUSTOM_DIR);
        assertFalse(Settings.EASTER_EGG);
    }
}
