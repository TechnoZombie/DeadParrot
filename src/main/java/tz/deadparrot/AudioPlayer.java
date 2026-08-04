package tz.deadparrot;

import lombok.extern.slf4j.Slf4j;
import tz.deadparrot.utils.AudioDeviceManager;
import tz.deadparrot.utils.AudioResourcesPreloader;

import javax.sound.sampled.*;
import java.io.*;
import java.util.function.Supplier;

@Slf4j
public class AudioPlayer {
    private File filePath;
    private File leadingPing;
    private File markerFile;
    private Thread audioPlayerThread;
    private final AudioPlaybackProvider playbackProvider;
    private final AudioDeviceManager.AudioDevice outputDevice;

    public AudioPlayer() {
        this(AudioPlaybackProvider.SYSTEM, AudioResourcesPreloader::new, null);
    }

    public AudioPlayer(AudioDeviceManager.AudioDevice outputDevice) {
        this(AudioPlaybackProvider.SYSTEM, AudioResourcesPreloader::new, outputDevice);
    }

    AudioPlayer(AudioPlaybackProvider playbackProvider) {
        this(playbackProvider, AudioResourcesPreloader::new, null);
    }

    AudioPlayer(AudioPlaybackProvider playbackProvider,
                Supplier<AudioResourcesPreloader> preloaderFactory) {
        this(playbackProvider, preloaderFactory, null);
    }

    private AudioPlayer(AudioPlaybackProvider playbackProvider,
                        Supplier<AudioResourcesPreloader> preloaderFactory,
                        AudioDeviceManager.AudioDevice outputDevice) {
        this.playbackProvider = playbackProvider;
        this.outputDevice = outputDevice;
        AudioResourcesPreloader preloader = preloaderFactory.get();
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
                    AudioInputStream audioInput = playbackProvider.getAudioInputStream(filePath);
                    Clip clip;
                    if (outputDevice != null) {
                        try {
                            Mixer mixer = AudioSystem.getMixer(outputDevice.getMixerInfo());
                            clip = (Clip) mixer.getLine(new DataLine.Info(Clip.class, null));
                            log.info("Using selected output device: " + outputDevice.getName());
                        } catch (Exception e) {
                            log.warn("Failed to use selected output device, falling back to default: " + e.getMessage());
                            clip = playbackProvider.getClip();
                        }
                    } else {
                        clip = playbackProvider.getClip();
                    }
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
