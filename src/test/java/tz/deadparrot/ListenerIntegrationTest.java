package tz.deadparrot;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListenerIntegrationTest {

    private final boolean originalSpyMode = Settings.SPY_MODE;

    @AfterEach
    void restoreSettings() {
        Settings.SPY_MODE = originalSpyMode;
    }

    @Test
    void recordsWhenInputExceedsTheConfiguredSensitivity() throws Exception {
        Settings.SPY_MODE = true;
        FakeTargetDataLine line = new FakeTargetDataLine(
                Settings.AUDIO_FORMAT, new byte[]{0, 0x20, 0, 0});
        AtomicReference<Listener> listenerRef = new AtomicReference<>();
        AtomicBoolean recorded = new AtomicBoolean();
        AudioRecorder recorder = new RecordingStub(() -> {
            recorded.set(true);
            listenerRef.get().shutdown();
        });
        Listener listener = new Listener(recorder, provider(line, true));
        listenerRef.set(listener);

        listener.run();

        assertTrue(recorded.get());
        assertFalse(line.isOpen());
    }

    @Test
    void ignoresPositiveAudioBelowTheDetectionThreshold() throws Exception {
        FakeTargetDataLine line = new FakeTargetDataLine(
                Settings.AUDIO_FORMAT, new byte[]{0, 1, 0, 1});
        CountDownLatch lineOpened = new CountDownLatch(1);
        AtomicBoolean recorded = new AtomicBoolean();
        AudioInputProvider input = new AudioInputProvider() {
            @Override public boolean isLineSupported(DataLine.Info lineInfo) { return true; }
            @Override public TargetDataLine getLine(DataLine.Info lineInfo) {
                lineOpened.countDown();
                return line;
            }
        };
        Listener listener = new Listener(new RecordingStub(() -> recorded.set(true)), input);
        Thread listenerThread = new Thread(listener);
        listenerThread.start();

        assertTrue(lineOpened.await(2, TimeUnit.SECONDS));
        Thread.sleep(20);
        listener.shutdown();
        listenerThread.join(2000);

        assertFalse(listenerThread.isAlive());
        assertFalse(recorded.get());
    }

    @Test
    void exitsWhenDeviceDoesNotSupportTheRequestedAudioFormat() throws Exception {
        Listener listener = new Listener(new RecordingStub(() -> {
            throw new AssertionError("unsupported input must not record");
        }), provider(null, false));

        listener.run();
    }

    @Test
    void constructsListenerWithTheSystemInputProviderWithoutOpeningHardware() throws Exception {
        AudioRecorder recorder = new RecordingStub(() -> { });
        Listener listener = new Listener(recorder);

        assertFalse(listener.isAlive());
    }

    @Test
    void shutdownStopsAnOpenInputLineWithoutDetectingSound() throws Exception {
        FakeTargetDataLine line = new FakeTargetDataLine(Settings.AUDIO_FORMAT, new byte[0]);
        Listener listener = new Listener(new RecordingStub(() -> {
            throw new AssertionError("silent input must not record");
        }), provider(line, true));
        Thread listenerThread = new Thread(listener);
        listenerThread.start();

        waitForLineOpen(line);
        listener.shutdown();
        listenerThread.join(2000);

        assertFalse(listenerThread.isAlive());
        assertFalse(line.isOpen());
    }

    @Test
    void retriesUnavailableDevicesUntilShutdown() throws Exception {
        AtomicBoolean firstAttempt = new AtomicBoolean(true);
        AudioInputProvider unavailableOnce = new AudioInputProvider() {
            @Override
            public boolean isLineSupported(DataLine.Info lineInfo) {
                return true;
            }

            @Override
            public TargetDataLine getLine(DataLine.Info lineInfo) throws LineUnavailableException {
                if (firstAttempt.getAndSet(false)) {
                    throw new LineUnavailableException("busy");
                }
                return new FakeTargetDataLine(Settings.AUDIO_FORMAT, new byte[0]);
            }
        };
        Listener listener = new Listener(new RecordingStub(() -> {
            throw new AssertionError("silent input must not record");
        }), unavailableOnce);
        Thread listenerThread = new Thread(listener);
        listenerThread.start();

        Thread.sleep(25);
        listener.shutdown();
        listenerThread.join(2000);

        assertFalse(listenerThread.isAlive());
    }

    private AudioInputProvider provider(TargetDataLine line, boolean supported) {
        return new AudioInputProvider() {
            @Override
            public boolean isLineSupported(DataLine.Info lineInfo) {
                return supported;
            }

            @Override
            public TargetDataLine getLine(DataLine.Info lineInfo) {
                return line;
            }
        };
    }

    private void waitForLineOpen(FakeTargetDataLine line) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (!line.isOpen() && System.currentTimeMillis() < deadline) {
            Thread.sleep(5);
        }
        assertTrue(line.isOpen());
    }

    private static final class RecordingStub extends AudioRecorder {
        private final Runnable recordingAction;

        private RecordingStub(Runnable recordingAction) throws LineUnavailableException {
            super(providerForStub());
            this.recordingAction = recordingAction;
        }

        @Override
        public void record() {
            recordingAction.run();
        }

        private static AudioInputProvider providerForStub() {
            return new AudioInputProvider() {
                @Override public boolean isLineSupported(DataLine.Info lineInfo) { return true; }
                @Override public TargetDataLine getLine(DataLine.Info lineInfo) {
                    return new FakeTargetDataLine(Settings.AUDIO_FORMAT, new byte[0]);
                }
            };
        }
    }
}
