package tz.deadparrot;

import lombok.extern.slf4j.Slf4j;
import tz.deadparrot.utils.AudioResourcesPreloader;
import tz.deadparrot.utils.FileUtils;
import tz.deadparrot.utils.SoundSettingsOpener;
import tz.deadparrot.utils.SystemUtils;
import tz.deadparrot.utils.Printer;
import tz.deadparrot.utils.ParrotQuotes;
import tz.deadparrot.utils.AudioDeviceManager;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;

@Slf4j
public class Processor {
    private AudioRecorder audioRecorder;
    private Listener listener;

    public void init() {

        SystemUtils.detectOSandSetPaths(true);
        applySettings();
        initializeComponents();
        setupShutdownHook();

        try {
            new AudioResourcesPreloader().copyMarkerToTemp();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void applySettings() {

        if (Settings.SPY_MODE) {
            Settings.KEEP_RECORDINGS = true;
            Settings.MARKER_MODE = false;
            log.warn(Constants.SPY_MODE_ENABLED);
        }

        if (Settings.MARKER_MODE) {
            new AudioMarker().runMarkerMode(this);
        } else if (!Settings.SPY_MODE && !Settings.MARKER_MODE) {
            log.info(Constants.RUNNING_IN_STANDARD_MODE);
        }

        if (Settings.KEEP_RECORDINGS) {
            log.warn(Constants.KEEP_RECORDINGS_IS_ON);
            if (Settings.SAVE_RECORDINGS_TO_DESKTOP) {
                FileUtils.verifyAndCreateOutputFolder(Settings.OUTPUT_DESKTOP_FOLDER_PATH);
            } else if (Settings.SAVE_RECORDINGS_TO_CUSTOM_DIR) {
                FileUtils.verifyAndCreateOutputFolder(Settings.CUSTOM_RECORDINGS_DIRECTORY);
            } else {
                FileUtils.verifyAndCreateOutputFolder(Constants.OUTPUT_FOLDER_PATH);
            }

        }
        if (Settings.OPEN_OS_RECORDING_SETTINGS) {
            SoundSettingsOpener.openRecordingSettings();
        }

        if (Settings.PRINT_SETTINGS) {
            Printer.printCurrentSettingsV2();
        }
    }

    private void initializeComponents() {
        try {
            // Get selected input device
            AudioDeviceManager.AudioDevice inputDevice = null;
            if (Settings.SELECTED_INPUT_DEVICE != null) {
                java.util.List<AudioDeviceManager.AudioDevice> devices = AudioDeviceManager.getInputDevices();
                inputDevice = AudioDeviceManager.findDeviceByName(Settings.SELECTED_INPUT_DEVICE, devices);
            }

            // Get selected audio format
            AudioFormat selectedFormat = null;
            if (Settings.SELECTED_AUDIO_FORMAT != null) {
                // Parse the format string (e.g., "44100 Hz, 16-bit, 2 ch")
                selectedFormat = parseAudioFormatString(Settings.SELECTED_AUDIO_FORMAT);
            }

            audioRecorder = new AudioRecorder(inputDevice, selectedFormat);
            listener = new Listener(audioRecorder, inputDevice, selectedFormat);
            listener.start();
        } catch (LineUnavailableException e) {
            log.error(Constants.LINE_UNAVAILABLE, e);
            throw new RuntimeException(e);
        }
    }

    private AudioFormat parseAudioFormatString(String formatString) {
        // Format string is like "44100 Hz, 16-bit, 2 ch"
        try {
            String[] parts = formatString.split(",");
            if (parts.length >= 3) {
                float sampleRate = Float.parseFloat(parts[0].trim().split(" ")[0]);
                int sampleSizeInBits = Integer.parseInt(parts[1].trim().split("-")[0]);
                int channels = Integer.parseInt(parts[2].trim().split(" ")[0]);

                return new AudioFormat(sampleRate, sampleSizeInBits, channels, true, false);
            }
        } catch (Exception e) {
            log.warn("Failed to parse audio format string: " + formatString, e);
        }
        return null;
    }


    private void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    void shutdown() {
        log.info(Constants.SHUTTING_DOWN);
        if (audioRecorder != null) {
            audioRecorder.shutdown();
        }

        if (listener != null) {
            listener.shutdown();
        }

        logShutdownMessage();
    }


    protected void logShutdownMessage() {
        if (Settings.EASTER_EGG) {
            log.info(ParrotQuotes.getRandomParrotLine());
        } else {
            log.info(Constants.SHUT_DOWN_COMPLETE);
        }
    }
}