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
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public class Processor {
    private AudioRecorder audioRecorder;
    private Listener listener;
    private Supplier<AudioRecorder> recorderFactory =
            () -> createAudioRecorder(AudioInputProvider.SYSTEM);
    private Function<AudioRecorder, Listener> listenerFactory = Listener::new;
    private Supplier<AudioMarker> markerFactory = AudioMarker::new;
    private Supplier<AudioResourcesPreloader> preloaderFactory = AudioResourcesPreloader::new;
    Thread shutdownHook;

    public Processor() {
    }

    Processor(Supplier<AudioRecorder> recorderFactory,
              Function<AudioRecorder, Listener> listenerFactory,
              Supplier<AudioMarker> markerFactory,
              Supplier<AudioResourcesPreloader> preloaderFactory) {
        this.recorderFactory = recorderFactory;
        this.listenerFactory = listenerFactory;
        this.markerFactory = markerFactory;
        this.preloaderFactory = preloaderFactory;
    }

    public void init() {

        SystemUtils.detectOSandSetPaths(true);
        applySettings();
        initializeComponents();
        setupShutdownHook();

        try {
            preloaderFactory.get().copyMarkerToTemp();
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
            markerFactory.get().runMarkerMode(this);
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
            AudioDeviceManager.AudioDevice inputDevice = null;
            if (Settings.SELECTED_INPUT_DEVICE != null) {
                java.util.List<AudioDeviceManager.AudioDevice> devices = AudioDeviceManager.getInputDevices();
                inputDevice = AudioDeviceManager.findDeviceByName(Settings.SELECTED_INPUT_DEVICE, devices);
            }

            AudioFormat selectedFormat = null;
            if (Settings.SELECTED_AUDIO_FORMAT != null) {
                selectedFormat = parseAudioFormatString(Settings.SELECTED_AUDIO_FORMAT);
            }

            if (inputDevice != null || selectedFormat != null) {
                audioRecorder = new AudioRecorder(inputDevice, selectedFormat);
                listener = new Listener(audioRecorder, inputDevice, selectedFormat);
                listener.start();
            } else {
                audioRecorder = recorderFactory.get();
            }
        } catch (LineUnavailableException e) {
            log.error(Constants.LINE_UNAVAILABLE, e);
            throw e;
        }

        if (listener == null) {
            listener = listenerFactory.apply(audioRecorder);
            listener.start();
        }
    }

    static AudioRecorder createAudioRecorder(AudioInputProvider audioInputProvider) {
        try {
            return new AudioRecorder(audioInputProvider);
        } catch (LineUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    private AudioFormat parseAudioFormatString(String formatString) {
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
    }


    private void setupShutdownHook() {
        shutdownHook = new Thread(this::shutdown);
        Runtime.getRuntime().addShutdownHook(shutdownHook);
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