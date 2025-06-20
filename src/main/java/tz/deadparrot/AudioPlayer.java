package tz.deadparrot;

import lombok.extern.slf4j.Slf4j;
import tz.deadparrot.Constants;

import javax.sound.sampled.*;
import java.io.File;

@Slf4j
public class AudioPlayer {

    public void play() {
        Thread audioPlayerThread = new Thread(() -> {
            try {
                File filePath = new File(Constants.FILE_PATH);
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
                log.error(String.valueOf(e));
            }
        });

        audioPlayerThread.start();

        try {
            audioPlayerThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

    }
}
