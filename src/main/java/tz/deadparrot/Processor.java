package tz.deadparrot;

import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.LineUnavailableException;

@Slf4j
public class Processor {


    AudioRecorder audioRecorder;
    AudioPlayer audioPlayer = new AudioPlayer();

    public void init() {

        try {
            audioRecorder = new AudioRecorder();
        } catch (LineUnavailableException e) {
            log.error(Constants.LINE_UNAVAILABLE, e);
            throw new RuntimeException(e);
        }

        // Add shutdown hook for clean exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info(Constants.SHUTTING_DOWN);
            audioRecorder.shutdown();
            audioPlayer.delete();
            log.info(Constants.SHUT_DOWN_COMPLETE);
        }));

        audioRecorder.record();


        // stop after 10 secs.
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn(Constants.RECORDING_INTERRUPTED);
        }
        audioRecorder.stop();
        audioPlayer.play();

    }


}


