package tz.deadparrot;

import lombok.extern.slf4j.Slf4j;
import tz.deadparrot.utils.ParrotQuotes;

import javax.sound.sampled.LineUnavailableException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class Processor {
    AudioRecorder audioRecorder;
    Listener listener;

    public void init() {

        if (Settings.SPY_MODE) {
            Settings.KEEP_RECORDINGS = true;
            log.warn(Constants.SPY_MODE_IS_ON);
        } else {
            log.info(Constants.RUNNING_IN_STANDARD_MODE);
        }

        if (Settings.KEEP_RECORDINGS) {
            log.warn(Constants.KEEP_RECORDINGS_IS_ON);
            verifyAndCreateOutputFolder(Constants.OUTPUT_FOLDER_PATH);
        }

        try {
            audioRecorder = new AudioRecorder();
        } catch (LineUnavailableException e) {
            log.error(Constants.LINE_UNAVAILABLE, e);
            throw new RuntimeException(e);
        }

        listener = new Listener(audioRecorder);
        listener.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info(Constants.SHUTTING_DOWN);
            audioRecorder.shutdown();
            listener.shutdown();
            if (Settings.EASTEREGG) {
                log.info(ParrotQuotes.getRandomParrotLine());
            } else {
                log.info(Constants.SHUT_DOWN_COMPLETE);
            }
        }));
    }

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


