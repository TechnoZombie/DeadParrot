package tz.deadparrot;

import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.LineUnavailableException;

@Slf4j
public class Processor {
    AudioRecorder audioRecorder;
    Listener listener;

    public void init() {

        if (Settings.KEEP_RECORDINGS) {
            log.warn(Constants.KEEP_RECORDINGS_IS_ON);
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
            log.info(Constants.SHUT_DOWN_COMPLETE);
        }));
    }
}


