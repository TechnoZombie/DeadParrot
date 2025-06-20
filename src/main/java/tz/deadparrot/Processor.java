package tz.deadparrot;

import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.LineUnavailableException;
import java.util.Timer;

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

        audioRecorder.record();


        // stop after 10 secs.
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Recording interrupted");
        }
        audioRecorder.stop();
        audioPlayer.play();

    }


}


