package tz.deadparrot;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioMarkerIntegrationTest {

    private final boolean originalSpyMode = Settings.SPY_MODE;
    private final boolean originalMarkerMode = Settings.MARKER_MODE;

    @AfterEach
    void restoreSettings() {
        Settings.SPY_MODE = originalSpyMode;
        Settings.MARKER_MODE = originalMarkerMode;
    }

    @Test
    void playsMarkersUntilInterruptedAndRunsShutdownCleanup() throws Exception {
        Settings.SPY_MODE = false;
        Settings.MARKER_MODE = true;
        CountDownLatch markerPlayed = new CountDownLatch(1);
        AtomicBoolean playerStopped = new AtomicBoolean();
        TestPlayer player = new TestPlayer(markerPlayed, playerStopped);
        TestProcessor processor = new TestProcessor();
        AudioMarker marker = new AudioMarker(() -> player, Long.MAX_VALUE);
        Thread markerThread = new Thread(() -> marker.runMarkerMode(processor));
        markerThread.start();

        assertTrue(markerPlayed.await(2, TimeUnit.SECONDS));
        markerThread.interrupt();
        markerThread.join(2000);
        assertFalse(markerThread.isAlive());

        marker.shutdownHook.run();
        Runtime.getRuntime().removeShutdownHook(marker.shutdownHook);

        assertTrue(playerStopped.get());
        assertTrue(processor.shutdownLogged.get());
    }

    private static final class TestPlayer extends AudioPlayer {
        private final CountDownLatch markerPlayed;
        private final AtomicBoolean stopped;

        private TestPlayer(CountDownLatch markerPlayed, AtomicBoolean stopped) {
            super(new AudioPlaybackProvider() {
                @Override
                public javax.sound.sampled.AudioInputStream getAudioInputStream(File file) {
                    return null;
                }

                @Override
                public Clip getClip() throws LineUnavailableException {
                    return null;
                }
            });
            this.markerPlayed = markerPlayed;
            this.stopped = stopped;
        }

        @Override
        public void playMarker() {
            markerPlayed.countDown();
        }

        @Override
        public void stopAndDelete() {
            stopped.set(true);
        }
    }

    private static final class TestProcessor extends Processor {
        private final AtomicBoolean shutdownLogged = new AtomicBoolean();

        @Override
        protected void logShutdownMessage() {
            shutdownLogged.set(true);
        }
    }
}
