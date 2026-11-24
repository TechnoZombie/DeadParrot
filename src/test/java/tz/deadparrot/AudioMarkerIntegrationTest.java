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

    @Test
    void constructsMarkerUsingDefaultInterval() {
        assertTrue(new AudioMarker() != null);
    }

    @Test
    void shutdownWithoutCreatedPlayerStillLogsProcessorShutdown() throws Exception {
        TestProcessor processor = new TestProcessor();
        AudioMarker marker = new AudioMarker(() -> null, 0);
        var subProcessor = AudioMarker.class.getDeclaredField("subProcessor");
        subProcessor.setAccessible(true);
        subProcessor.set(marker, processor);
        var shutdown = AudioMarker.class.getDeclaredMethod("shutdownMarkerMode");
        shutdown.setAccessible(true);

        shutdown.invoke(marker);

        assertTrue(processor.shutdownLogged.get());
    }

    @Test
    void continuesAfterACompletedMarkerInterval() throws Exception {
        CountDownLatch markersPlayed = new CountDownLatch(2);
        TestPlayer player = new TestPlayer(markersPlayed, new AtomicBoolean());
        AudioMarker marker = new AudioMarker(() -> player, 0);
        Thread markerThread = new Thread(() -> marker.runMarkerMode(new TestProcessor()));
        markerThread.start();

        assertTrue(markersPlayed.await(2, TimeUnit.SECONDS));
        long deadline = System.currentTimeMillis() + 2000;
        while (markersPlayed.getCount() > 0 && System.currentTimeMillis() < deadline) {
            Thread.yield();
        }
        markerThread.interrupt();
        markerThread.join(2000);
        Runtime.getRuntime().removeShutdownHook(marker.shutdownHook);

        assertFalse(markerThread.isAlive());
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
