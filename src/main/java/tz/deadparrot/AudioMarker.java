package tz.deadparrot;

import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

@Slf4j
public class AudioMarker {

    AudioPlayer player;
    Processor subProcessor;
    private final Supplier<AudioPlayer> playerFactory;
    private final long markerInterval;
    Thread shutdownHook;

    public AudioMarker() {
        this(AudioPlayer::new, Settings.MARKER_TIME);
    }

    AudioMarker(Supplier<AudioPlayer> playerFactory, long markerInterval) {
        this.playerFactory = playerFactory;
        this.markerInterval = markerInterval;
    }

    public void runMarkerMode(Processor processor) {
        this.subProcessor = processor;
        int procCounter = 0;
        log.info(Constants.RUNNING_IN_MARKER_MODE);
        log.info(Settings.MARKER_ENABLED);
        player = playerFactory.get();

        shutdownHook = new Thread(this::shutdownMarkerMode);
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        while (!Thread.currentThread().isInterrupted()) {
            procCounter++;
            log.info(Constants.MARKER_COUNT + procCounter);
            player.playMarker();

            try {
                Thread.sleep(markerInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void shutdownMarkerMode() {
        log.info(Constants.SHUTTING_DOWN);
        if (player != null) {
            player.stopAndDelete();
        }
        subProcessor.logShutdownMessage();
    }

}
