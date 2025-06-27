package tz.deadparrot;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AudioMarker {

    AudioPlayer player;
    Processor subProcessor;

    public void runMarkerMode(Processor processor) {
        this.subProcessor = processor;
        int procCounter = 0;
        log.info(Constants.RUNNING_IN_MARKER_MODE);
        player = new AudioPlayer();

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownMarkerMode));

        while (!Thread.currentThread().isInterrupted()) {
            procCounter++;
            log.info(Constants.MARKER_COUNT + procCounter);
            player.playMarker();

            try {
                Thread.sleep(Settings.MARKER_TIME);
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
