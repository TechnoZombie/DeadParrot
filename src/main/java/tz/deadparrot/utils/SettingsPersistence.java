package tz.deadparrot.utils;

import lombok.extern.slf4j.Slf4j;
import tz.deadparrot.Settings;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

@Slf4j
public class SettingsPersistence {

    private static final String SETTINGS_FILE = "deadparrot.settings";

    public static void saveSettings() {
        saveSettings(Path.of(SETTINGS_FILE));
    }

    static void saveSettings(Path settingsFile) {

        Properties properties = new Properties();

        properties.setProperty("spyMode", String.valueOf(Settings.SPY_MODE));

        properties.setProperty("markerMode", String.valueOf(Settings.MARKER_MODE));

        properties.setProperty("keepRecordings", String.valueOf(Settings.KEEP_RECORDINGS));

        properties.setProperty("printSettings", String.valueOf(Settings.PRINT_SETTINGS));

        properties.setProperty("darkMode", String.valueOf(Settings.DARK_MODE));

        properties.setProperty("openOSRecordingSettings", String.valueOf(Settings.OPEN_OS_RECORDING_SETTINGS));

        properties.setProperty("saveToDesktop", String.valueOf(Settings.SAVE_RECORDINGS_TO_DESKTOP));

        properties.setProperty("saveToCustomDir", String.valueOf(Settings.SAVE_RECORDINGS_TO_CUSTOM_DIR));

        properties.setProperty("easterEgg", String.valueOf(Settings.EASTER_EGG));

        try (FileOutputStream fos = new FileOutputStream(settingsFile.toFile())) {

            properties.store(fos, "DeadParrot Settings");

            //   log.info("Settings saved successfully.");

        } catch (IOException e) {
            log.error("Failed to save settings.", e);
        }
    }

    public static void loadSettings() {
        loadSettings(Path.of(SETTINGS_FILE));
    }

    static void loadSettings(Path settingsFile) {

        Properties properties = new Properties();

        try (FileInputStream fis = new FileInputStream(settingsFile.toFile())) {

            properties.load(fis);

            Settings.SPY_MODE = Boolean.parseBoolean(properties.getProperty("spyMode", "false"));

            Settings.MARKER_MODE = Boolean.parseBoolean(properties.getProperty("markerMode", "false"));

            Settings.KEEP_RECORDINGS = Boolean.parseBoolean(properties.getProperty("keepRecordings", "false"));

            Settings.PRINT_SETTINGS = Boolean.parseBoolean(properties.getProperty("printSettings", "false"));

            Settings.DARK_MODE = Boolean.parseBoolean(properties.getProperty("darkMode", "false"));

            Settings.OPEN_OS_RECORDING_SETTINGS = Boolean.parseBoolean(properties.getProperty("openOSRecordingSettings", "false"));

            Settings.SAVE_RECORDINGS_TO_DESKTOP = Boolean.parseBoolean(properties.getProperty("saveToDesktop", "true"));

            Settings.SAVE_RECORDINGS_TO_CUSTOM_DIR = Boolean.parseBoolean(properties.getProperty("saveToCustomDir", "false"));

            Settings.EASTER_EGG = Boolean.parseBoolean(properties.getProperty("easterEgg", "false"));

            log.info("Settings loaded successfully.");

        } catch (IOException e) {

            log.warn("No settings file found. Using defaults.");
        }
    }
}