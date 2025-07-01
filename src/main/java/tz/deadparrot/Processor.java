package tz.deadparrot;

import lombok.extern.slf4j.Slf4j;
import tz.deadparrot.utils.AudioResourcesPreloader;
import tz.deadparrot.utils.FileUtils;
import tz.deadparrot.utils.ParrotQuotes;
import tz.deadparrot.utils.SoundSettingsOpener;

import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;

@Slf4j
public class Processor {
    private AudioRecorder audioRecorder;
    private Listener listener;

    public void init() {
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
            log.warn(Constants.SPY_MODE_IS_ON);
        }

        if (Settings.MARKER_MODE) {
            new AudioMarker().runMarkerMode(this);
        } else if (!Settings.SPY_MODE && !Settings.MARKER_MODE) {
            log.info(Constants.RUNNING_IN_STANDARD_MODE);
        }

        if (Settings.KEEP_RECORDINGS) {
            log.warn(Constants.KEEP_RECORDINGS_IS_ON);
            FileUtils.verifyAndCreateOutputFolder(Constants.OUTPUT_FOLDER_PATH);

        }
        if (Settings.OPEN_WINDOWS_RECORDING_SETTINGS) {
            SoundSettingsOpener.openRecordingSettings();
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


    protected void logShutdownMessage() {
        if (Settings.EASTER_EGG) {
            log.info(ParrotQuotes.getRandomParrotLine());
        } else {
            log.info(Constants.SHUT_DOWN_COMPLETE);
        }
    }
}