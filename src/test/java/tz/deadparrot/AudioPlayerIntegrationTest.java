package tz.deadparrot;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tz.deadparrot.utils.AudioResourcesPreloader;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioPlayerIntegrationTest {

    @TempDir
    Path tempDir;

    private final boolean originalSpyMode = Settings.SPY_MODE;
    private final boolean originalMarkerMode = Settings.MARKER_MODE;
    private final boolean originalPlayMarker = Settings.PLAY_MARKER;

    @AfterEach
    void restoreSettings() {
        Settings.SPY_MODE = originalSpyMode;
        Settings.MARKER_MODE = originalMarkerMode;
    }

    @Test
    void preloadsAndPlaysLeadingPingAndRecording() throws Exception {
        Settings.SPY_MODE = false;
        Settings.MARKER_MODE = false;
        AudioPlayer player = new AudioPlayer(provider());
        File ping = field(player, "leadingPing");
        assertNotNull(ping);

        player.playLeadingPing();
        File recording = Files.createFile(tempDir.resolve("recording.wav")).toFile();
        player.play(recording);

        assertTrue(ping.exists());
        assertFalse(playerThread(player).isAlive());
        Files.deleteIfExists(ping.toPath());
    }

    @Test
    void preloadsAndPlaysMarkerThenDeletesItOnShutdown() throws Exception {
        Settings.SPY_MODE = false;
        Settings.MARKER_MODE = true;
        AudioPlayer player = new AudioPlayer(provider());
        File marker = field(player, "markerFile");

        assertNotNull(marker);
        assertTrue(marker.exists());
        player.playMarker();
        player.stopAndDelete();

        assertFalse(marker.exists());
        assertFalse(playerThread(player).isAlive());
    }

    @Test
    void skipsMissingFilesAndReportsPlaybackFailuresWithoutPropagatingThem() throws Exception {
        Settings.SPY_MODE = true;
        Settings.MARKER_MODE = false;
        AudioPlayer player = new AudioPlayer(provider());

        player.play(tempDir.resolve("missing.wav").toFile());

        AudioPlayer failingPlayer = new AudioPlayer(new AudioPlaybackProvider() {
            @Override
            public AudioInputStream getAudioInputStream(File file) {
                throw new IllegalArgumentException("invalid audio");
            }

            @Override
            public Clip getClip() {
                throw new AssertionError("clip should not be requested");
            }
        });
        failingPlayer.play(Files.createFile(tempDir.resolve("invalid.wav")).toFile());
        failingPlayer.playLeadingPing();
        failingPlayer.playMarker();
        failingPlayer.stopAndDelete();
    }

    @Test
    void logsResourcePreloadErrorsWithoutBreakingPlayerConstruction() {
        Settings.SPY_MODE = false;
        Settings.MARKER_MODE = false;
        AudioPlayer player = new AudioPlayer(provider(), () -> new AudioResourcesPreloader() {
            @Override
            public File copyLeadingPingToTemp() throws IOException {
                throw new IOException("missing ping");
            }
        });

        assertNotNull(player);
    }

    @Test
    void constructsPlayerWithTheSystemProviderWithoutOpeningAudioDevices() {
        Settings.SPY_MODE = true;
        Settings.MARKER_MODE = false;

        assertNotNull(new AudioPlayer());
    }

    @Test
    void cleanupWithoutPlaybackOrExistingMarkerIsSafe() throws Exception {
        Settings.SPY_MODE = true;
        Settings.MARKER_MODE = false;
        AudioPlayer player = new AudioPlayer(provider());
        player.stopAndDelete();
        setField(player, "markerFile", tempDir.resolve("missing-marker.wav").toFile());

        player.stopAndDelete();
    }

    @Test
    void preservesInterruptStatusWhenPlaybackJoinIsInterrupted() throws Exception {
        Settings.SPY_MODE = true;
        Settings.MARKER_MODE = false;
        CountDownLatch playbackStarted = new CountDownLatch(1);
        CountDownLatch finishPlayback = new CountDownLatch(1);
        AudioPlayer player = new AudioPlayer(new AudioPlaybackProvider() {
            @Override
            public AudioInputStream getAudioInputStream(File file) {
                playbackStarted.countDown();
                try {
                    finishPlayback.await();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
                throw new IllegalStateException("test playback finished");
            }

            @Override
            public Clip getClip() {
                throw new AssertionError("clip should not be requested");
            }
        });
        File recording = Files.createFile(tempDir.resolve("recording.wav")).toFile();
        Thread.currentThread().interrupt();
        try {
            player.play(recording);
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
            finishPlayback.countDown();
        }

        assertTrue(playbackStarted.await(2, TimeUnit.SECONDS));
        playerThread(player).join(2000);
        assertFalse(playerThread(player).isAlive());
    }

    @Test
    void stopAndDeleteWaitsForPlaybackAndPreservesInterruptStatus() throws Exception {
        Settings.SPY_MODE = true;
        Settings.MARKER_MODE = false;
        CountDownLatch streamRequested = new CountDownLatch(1);
        CountDownLatch allowPlaybackToFail = new CountDownLatch(1);
        AudioPlayer player = new AudioPlayer(new AudioPlaybackProvider() {
            @Override
            public AudioInputStream getAudioInputStream(File file) {
                streamRequested.countDown();
                try {
                    allowPlaybackToFail.await();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
                throw new IllegalStateException("simulated playback completion");
            }

            @Override
            public Clip getClip() {
                throw new AssertionError("clip should not be requested");
            }
        });
        Path recording = Files.createFile(tempDir.resolve("recording.wav"));
        Path markerDirectory = Files.createDirectory(tempDir.resolve("marker"));
        Path child = Files.createFile(markerDirectory.resolve("in-use.wav"));
        setField(player, "markerFile", markerDirectory.toFile());
        Thread playCaller = new Thread(() -> player.play(recording.toFile()));
        playCaller.start();
        assertTrue(streamRequested.await(2, TimeUnit.SECONDS));

        Thread.currentThread().interrupt();
        try {
            player.stopAndDelete();
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
            allowPlaybackToFail.countDown();
        }

        playCaller.join(2000);
        playerThread(player).join(2000);
        assertFalse(playCaller.isAlive());
        assertTrue(Files.exists(markerDirectory));
        Files.deleteIfExists(child);
        Files.deleteIfExists(markerDirectory);
    }

    @Test
    void waitsForActivePlaybackBeforeDeletingMarker() throws Exception {
        Settings.SPY_MODE = true;
        Settings.MARKER_MODE = false;
        CountDownLatch streamRequested = new CountDownLatch(1);
        CountDownLatch allowPlaybackToFail = new CountDownLatch(1);
        AudioPlayer player = new AudioPlayer(new AudioPlaybackProvider() {
            @Override
            public AudioInputStream getAudioInputStream(File file) {
                streamRequested.countDown();
                try {
                    allowPlaybackToFail.await();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
                throw new IllegalStateException("simulated playback completion");
            }

            @Override public Clip getClip() { throw new AssertionError("clip should not be requested"); }
        });
        Path recording = Files.createFile(tempDir.resolve("recording-wait.wav"));
        Path marker = Files.createFile(tempDir.resolve("marker-wait.wav"));
        setField(player, "markerFile", marker.toFile());
        Thread playCaller = new Thread(() -> player.play(recording.toFile()));
        playCaller.start();
        assertTrue(streamRequested.await(2, TimeUnit.SECONDS));

        Thread cleanup = new Thread(player::stopAndDelete);
        cleanup.start();
        Thread.sleep(20);
        assertTrue(cleanup.isAlive());
        allowPlaybackToFail.countDown();
        cleanup.join(2000);
        playCaller.join(2000);

        assertFalse(cleanup.isAlive());
        assertFalse(playCaller.isAlive());
        assertFalse(Files.exists(marker));
    }

    @Test
    void ignoresNonStopClipEventsBeforePlaybackCompletes() throws Exception {
        Settings.SPY_MODE = true;
        Settings.MARKER_MODE = false;
        AtomicReference<LineListener> listenerRef = new AtomicReference<>();
        AudioPlayer player = new AudioPlayer(new AudioPlaybackProvider() {
            @Override
            public AudioInputStream getAudioInputStream(File file) {
                return new AudioInputStream(new ByteArrayInputStream(new byte[0]),
                        new AudioFormat(8000, 16, 1, true, false), 0);
            }

            @Override
            public Clip getClip() {
                return (Clip) Proxy.newProxyInstance(Clip.class.getClassLoader(),
                        new Class<?>[]{Clip.class}, (proxy, method, args) -> {
                            if (method.getName().equals("addLineListener")) {
                                listenerRef.set((LineListener) args[0]);
                            } else if (method.getName().equals("start")) {
                                new Thread(() -> {
                                    try {
                                        Thread.sleep(10);
                                    } catch (InterruptedException exception) {
                                        Thread.currentThread().interrupt();
                                        return;
                                    }
                                    listenerRef.get().update(new LineEvent((Line) proxy,
                                            LineEvent.Type.OPEN, 0));
                                    listenerRef.get().update(new LineEvent((Line) proxy,
                                            LineEvent.Type.STOP, 0));
                                }).start();
                            }
                            return null;
                        });
            }
        });
        Path recording = Files.createFile(tempDir.resolve("events.wav"));

        player.play(recording.toFile());

        assertFalse(playerThread(player).isAlive());
    }

    private AudioPlaybackProvider provider() {
        return new AudioPlaybackProvider() {
            @Override
            public AudioInputStream getAudioInputStream(File file) {
                return new AudioInputStream(new ByteArrayInputStream(new byte[0]),
                        new AudioFormat(8000, 16, 1, true, false), 0);
            }

            @Override
            public Clip getClip() {
                AtomicReference<LineListener> listener = new AtomicReference<>();
                AtomicBoolean open = new AtomicBoolean();
                return (Clip) Proxy.newProxyInstance(Clip.class.getClassLoader(),
                        new Class<?>[]{Clip.class}, (proxy, method, args) -> {
                            switch (method.getName()) {
                                case "open" -> open.set(true);
                                case "close" -> open.set(false);
                                case "addLineListener" -> listener.set((LineListener) args[0]);
                                case "start" -> {
                                    Thread notification = new Thread(() -> {
                                        try {
                                            Thread.sleep(10);
                                        } catch (InterruptedException e) {
                                            Thread.currentThread().interrupt();
                                            return;
                                        }
                                        listener.get().update(new LineEvent((Line) proxy,
                                                LineEvent.Type.STOP, 0));
                                    });
                                    notification.start();
                                }
                                case "isOpen" -> {
                                    return open.get();
                                }
                                case "toString" -> {
                                    return "TestClip";
                                }
                                case "hashCode" -> {
                                    return System.identityHashCode(proxy);
                                }
                                case "equals" -> {
                                    return proxy == args[0];
                                }
                            }
                            return null;
                        });
            }
        };
    }

    private File field(AudioPlayer player, String name) throws ReflectiveOperationException {
        Field field = AudioPlayer.class.getDeclaredField(name);
        field.setAccessible(true);
        return (File) field.get(player);
    }

    private Thread playerThread(AudioPlayer player) throws ReflectiveOperationException {
        Field field = AudioPlayer.class.getDeclaredField("audioPlayerThread");
        field.setAccessible(true);
        return (Thread) field.get(player);
    }

    private void setField(AudioPlayer player, String name, Object value)
            throws ReflectiveOperationException {
        Field field = AudioPlayer.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(player, value);
    }
}
