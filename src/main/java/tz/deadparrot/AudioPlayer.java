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

    public void play(File fileToPlay) {
        Thread audioPlayerThread = new Thread(() -> {
            try {
                this.filePath = fileToPlay;

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

                    // Log info based on what file is playing
                    if (fileToPlay == leadingPing) {
                        log.info(Constants.PLAYING_LEADING_PING);
                    } else {
                        log.info(Constants.PLAYBACK_STARTED);
                    }

                    synchronized (playbackCompletedLock) {
                        playbackCompletedLock.wait();
                    }

                    clip.close();
                    audioInput.close();

                    // Only display info is file being played is recording
                    if (fileToPlay != leadingPing) {
                        log.info(Constants.PLAYBACK_FINISHED);
                    }
                } else {
                    log.error(Constants.FILE_DOESNT_EXIST);
                }
            } catch (Exception e) {
                log.error(Constants.ERROR_DURING_PLAYBACK, e);
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
