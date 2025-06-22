package tz.deadparrot;

import lombok.extern.slf4j.Slf4j;
import tz.deadparrot.utils.LeadingPingPreloader;

import javax.sound.sampled.*;
import java.io.*;

@Slf4j
public class AudioPlayer {
    private File filePath;
    private File leadingPing;
    LeadingPingPreloader preloader = new LeadingPingPreloader();

    public AudioPlayer() {
        try {
            leadingPing = preloader.copyLeadingPingToTemp(Constants.LEADING_PING_FILE_PATH);
        } catch (IOException e) {
            log.error(Constants.ERROR_COPY_TO_TEMP, e);
        }
    }

    public void play(File outputFile) {
        Thread audioPlayerThread = new Thread(() -> {
            try {
                this.filePath = outputFile;

                if (filePath.exists()) {
                    AudioInputStream audioInput = AudioSystem.getAudioInputStream(filePath);
                    Clip clip = AudioSystem.getClip();
                    clip.open(audioInput);

                    Object playbackCompletedLock = new Object();

                    clip.addLineListener(event -> {
                        if (event.getType() == LineEvent.Type.STOP) {
                            synchronized (playbackCompletedLock) {
                                playbackCompletedLock.notify();
                            }
                        }
                    });

                    clip.start();
                    log.info(Constants.PLAYBACK_STARTED);

                    synchronized (playbackCompletedLock) {
                        playbackCompletedLock.wait();
                    }

                    clip.close();
                    audioInput.close();
                    log.info(Constants.PLAYBACK_FINISHED);
                } else {
                    log.error(Constants.FILE_DOESNT_EXIST);
                }
            } catch (Exception e) {
                log.error("Error during playback", e);
            }
        });

        audioPlayerThread.start();

        try {
            audioPlayerThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void playLeadingPing() {
        play(leadingPing);
    }


}
