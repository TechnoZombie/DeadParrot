package tz.deadparrot;

import lombok.extern.slf4j.Slf4j;
import tz.deadparrot.utils.AudioResourcesPreloader;

import javax.sound.sampled.*;
import java.io.*;

@Slf4j
public class AudioPlayer {
    private File filePath;
    private File leadingPing;
    private File markerFile;
    private Thread audioPlayerThread;


    public AudioPlayer() {
        AudioResourcesPreloader preloader = new AudioResourcesPreloader();
        try {
            // Only preload leadingPing if neither marker mode nor spy mode is active
            if (!Settings.MARKER_MODE && !Settings.SPY_MODE) {
                leadingPing = preloader.copyLeadingPingToTemp();
            }

            // Preload marker only if marker mode is active
            if (Settings.MARKER_MODE || Settings.PLAY_MARKER) {
                markerFile = preloader.copyMarkerToTemp();
            }
        } catch (IOException e) {
            log.error(Constants.ERROR_COPY_TO_TEMP, e);
        }
    }

    public void play(File fileToPlay) {
        audioPlayerThread = new Thread(() -> {
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
                    } else if (fileToPlay.equals(markerFile)) {
                        // Do nothing
                    } else {
                        // Optional: handle other files if needed
                        log.info(Constants.PLAYBACK_STARTED);
                    }

                    synchronized (playbackCompletedLock) {
                        playbackCompletedLock.wait();
                    }

                    clip.close();
                    audioInput.close();

                    // Only display info is file being played is recording
                    if (!fileToPlay.equals(leadingPing) && !fileToPlay.equals(markerFile)) {
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

    public void stopAndDelete() {
        try {
            if (audioPlayerThread != null && audioPlayerThread.isAlive()) {
                log.info("Waiting for audio playback to finish before deleting marker.");
                audioPlayerThread.join();  // Wait until file is no longer in use
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (markerFile != null && markerFile.exists()) {
            if (markerFile.delete()) {
                log.info("Marker file deleted successfully.");
            } else {
                log.warn("Failed to delete marker file.");
            }
        }
    }

    public void playLeadingPing() {
        play(leadingPing);
    }

    public void playMarker() {
        play(markerFile);
    }
}
