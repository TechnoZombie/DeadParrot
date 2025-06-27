package tz.deadparrot;

import lombok.extern.slf4j.Slf4j;
import tz.deadparrot.utils.FileUtils;
import tz.deadparrot.utils.ParrotQuotes;

import javax.sound.sampled.LineUnavailableException;

@Slf4j
public class Processor {
    private AudioRecorder audioRecorder;
    private Listener listener;

    public void init() {
        applySettings();
        initializeComponents();
        setupShutdownHook();
    }

    private void applySettings() {

        if (Settings.SPY_MODE) {
            Settings.KEEP_RECORDINGS = true;
            Settings.MARKER_MODE = false;
            log.warn(Constants.SPY_MODE_IS_ON);
        }
        if (Settings.MARKER_MODE) {
            runMarkerMode();
        } else if (!Settings.SPY_MODE && !Settings.MARKER_MODE) {
            log.info(Constants.RUNNING_IN_STANDARD_MODE);
        }
        if (Settings.KEEP_RECORDINGS) {
            log.warn(Constants.KEEP_RECORDINGS_IS_ON);
            FileUtils.verifyAndCreateOutputFolder(Constants.OUTPUT_FOLDER_PATH);
        }
    }

    private void initializeComponents() {
        try {
            audioRecorder = new AudioRecorder();
        } catch (LineUnavailableException e) {
            log.error(Constants.LINE_UNAVAILABLE, e);
            throw new RuntimeException(e);
        }

        listener = new Listener(audioRecorder);
        listener.start();
    }

    private void runMarkerMode() {
        int procCounter = 0;
        log.info(Constants.RUNNING_IN_MARKER_MODE);
        AudioPlayer player = new AudioPlayer();

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownMarkerMode));

        while (!Thread.currentThread().isInterrupted()) {
            procCounter++;
            log.info(Constants.MARKER_COUNT + procCounter);
            player.playMarker();

            try {
                Thread.sleep(Settings.MARKER_TIME);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    private void shutdown() {
        log.info(Constants.SHUTTING_DOWN);
        if (audioRecorder != null) {
            audioRecorder.shutdown();
        }

        if (listener != null) {
            listener.shutdown();
        }

        logShutdownMessage();
    }

    private void shutdownMarkerMode() {
        log.info(Constants.SHUTTING_DOWN);
        logShutdownMessage();
    }

    private void logShutdownMessage() {
        if (Settings.EASTEREGG) {
            log.info(ParrotQuotes.getRandomParrotLine());
        } else {
            log.info(Constants.SHUT_DOWN_COMPLETE);
        }
    }
}