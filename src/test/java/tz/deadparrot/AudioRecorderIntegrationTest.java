package tz.deadparrot;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tz.deadparrot.utils.AudioDeviceManager;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.Control;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Proxy;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioRecorderIntegrationTest {

    @TempDir
    Path tempDir;

    private final boolean originalKeepRecordings = Settings.KEEP_RECORDINGS;
    private final boolean originalSpyMode = Settings.SPY_MODE;
    private final boolean originalSaveToDesktop = Settings.SAVE_RECORDINGS_TO_DESKTOP;
    private final String originalDesktopPath = Settings.OUTPUT_DESKTOP_FOLDER_PATH;
    private final boolean originalCustomSave = Settings.SAVE_RECORDINGS_TO_CUSTOM_DIR;
    private final String originalCustomPath = Settings.CUSTOM_RECORDINGS_DIRECTORY;
    private final String originalInputDevice = Settings.SELECTED_INPUT_DEVICE;
    private final String originalOutputDevice = Settings.SELECTED_OUTPUT_DEVICE;

    @AfterEach
    void restoreSettings() {
        Settings.KEEP_RECORDINGS = originalKeepRecordings;
        Settings.SPY_MODE = originalSpyMode;
        Settings.SAVE_RECORDINGS_TO_DESKTOP = originalSaveToDesktop;
        Settings.OUTPUT_DESKTOP_FOLDER_PATH = originalDesktopPath;
        Settings.SAVE_RECORDINGS_TO_CUSTOM_DIR = originalCustomSave;
        Settings.CUSTOM_RECORDINGS_DIRECTORY = originalCustomPath;
        Settings.SELECTED_INPUT_DEVICE = originalInputDevice;
        Settings.SELECTED_OUTPUT_DEVICE = originalOutputDevice;
    }

    @Test
    void recordsAudioToWaveAndStopsWhenInputEnds() throws Exception {
        Settings.SPY_MODE = true;
        Settings.KEEP_RECORDINGS = true;
        Settings.SAVE_RECORDINGS_TO_DESKTOP = true;
        Settings.OUTPUT_DESKTOP_FOLDER_PATH = tempDir + File.separator;
        FakeTargetDataLine line = new FakeTargetDataLine(Settings.AUDIO_FORMAT, new byte[4096]);

        AudioRecorder recorder = createRecorder(line);
        recorder.record();

        File recording = recorder.outputFile;
        assertTrue(recording.exists());
        try (FileInputStream input = new FileInputStream(recording)) {
            byte[] header = input.readNBytes(44);
            assertEquals("RIFF", new String(header, 0, 4));
            assertEquals(recording.length() - 44, littleEndianInt(header, 40));
        } finally {
            Files.deleteIfExists(recording.toPath());
        }
        assertFalse(line.isRunning());
    }

    @Test
    void stopsAStillOpenRecordingAtItsConfiguredMaximumDuration() throws Exception {
        Settings.SPY_MODE = true;
        Settings.KEEP_RECORDINGS = true;
        Settings.SAVE_RECORDINGS_TO_DESKTOP = true;
        Settings.OUTPUT_DESKTOP_FOLDER_PATH = tempDir + File.separator;
        FakeTargetDataLine line =
                new FakeTargetDataLine(Settings.DEFAULT_AUDIO_FORMAT, new byte[0], true);
        AudioRecorder recorder = new AudioRecorder(inputProvider(line), 1500, 0);

        recorder.record();

        assertTrue(recorder.outputFile.exists());
        assertFalse(line.isRunning());
        Files.deleteIfExists(recorder.outputFile.toPath());
    }

    @Test
    void stopsRecordingWhenSilenceIsDetectedByTheReaderThread() throws Exception {
        Settings.SPY_MODE = true;
        Settings.KEEP_RECORDINGS = true;
        Settings.SAVE_RECORDINGS_TO_DESKTOP = true;
        Settings.OUTPUT_DESKTOP_FOLDER_PATH = tempDir + File.separator;
        FakeTargetDataLine line =
                new FakeTargetDataLine(Settings.DEFAULT_AUDIO_FORMAT, new byte[4096]);
        AudioRecorder recorder = new AudioRecorder(inputProvider(line), 0, 10000);

        recorder.record();

        assertTrue(recorder.outputFile.exists());
        assertFalse(line.isRunning());
        Files.deleteIfExists(recorder.outputFile.toPath());
    }

    @Test
    void writesWaveHeaderAndDetectsSilenceFromAnAudioStream() throws Exception {
        AudioRecorder recorder = createRecorder(new FakeTargetDataLine(Settings.AUDIO_FORMAT, new byte[0]));
        byte[] silentAudio = new byte[8];
        AudioInputStream stream = new AudioInputStream(
                new ByteArrayInputStream(silentAudio), Settings.AUDIO_FORMAT, 2);
        Path recording = tempDir.resolve("silence.wav");
        AtomicBoolean silenceDetected = new AtomicBoolean();
        AtomicBoolean recordingActive = new AtomicBoolean(true);

        recorder.recordWithMonitoring(stream, recording.toFile(), 500, 0,
                silenceDetected, recordingActive);

        assertTrue(silenceDetected.get());
        assertTrue(recordingActive.get());
        assertEquals(52, Files.size(recording));
        assertEquals(8, littleEndianInt(Files.readAllBytes(recording), 40));
    }

    @Test
    void updatesSoundTimeAndFinalizesWaveWhenInputReachesEof() throws Exception {
        AudioRecorder recorder = createRecorder(new FakeTargetDataLine(Settings.AUDIO_FORMAT, new byte[0]));
        byte[] audibleAudio = {0, 32, 0, 32};
        AudioInputStream stream = new AudioInputStream(
                new ByteArrayInputStream(audibleAudio), Settings.AUDIO_FORMAT, 1);
        Path recording = tempDir.resolve("audible.wav");
        AtomicBoolean silenceDetected = new AtomicBoolean();
        AtomicBoolean recordingActive = new AtomicBoolean(true);

        recorder.recordWithMonitoring(stream, recording.toFile(), 500, 1500,
                silenceDetected, recordingActive);

        assertFalse(silenceDetected.get());
        assertEquals(48, Files.size(recording));
        assertEquals(4, littleEndianInt(Files.readAllBytes(recording), 40));
    }

    @Test
    void propagatesRecordingFileErrors() throws Exception {
        AudioRecorder recorder = createRecorder(new FakeTargetDataLine(Settings.AUDIO_FORMAT, new byte[0]));
        AudioInputStream stream = new AudioInputStream(
                new ByteArrayInputStream(new byte[0]), Settings.AUDIO_FORMAT, 0);
        AtomicBoolean silenceDetected = new AtomicBoolean();
        AtomicBoolean recordingActive = new AtomicBoolean(true);

        assertThrows(IOException.class, () -> recorder.recordWithMonitoring(stream,
                tempDir.resolve("missing").resolve("recording.wav").toFile(),
                500, 1000, silenceDetected, recordingActive));
    }

    @Test
    void shutsDownAndDeletesRecordingsWhenKeepingIsDisabled() throws Exception {
        Settings.KEEP_RECORDINGS = false;
        FakeTargetDataLine line = new FakeTargetDataLine(Settings.AUDIO_FORMAT, new byte[0]);
        line.open(Settings.AUDIO_FORMAT);
        AudioRecorder recorder = createRecorder(line);
        Path recording = tempDir.resolve("delete.wav");
        Files.write(recording, new byte[]{1});
        recorder.outputFile = recording.toFile();

        recorder.shutdown();

        assertFalse(Files.exists(recording));
        assertFalse(line.isOpen());
    }

    @Test
    void doesNotDeleteKeptRecordings() throws Exception {
        Settings.KEEP_RECORDINGS = true;
        AudioRecorder recorder = createRecorder(new FakeTargetDataLine(Settings.AUDIO_FORMAT, new byte[0]));
        Path recording = tempDir.resolve("keep.wav");
        Files.write(recording, new byte[]{1});
        recorder.outputFile = recording.toFile();

        recorder.delete();

        assertTrue(Files.exists(recording));
    }

    @Test
    void usesSelectedMixerAndAudioFormatAndFallsBackWhenMixerLookupFails() throws Exception {
        javax.sound.sampled.AudioFormat selectedFormat =
                new javax.sound.sampled.AudioFormat(48000, 24, 2, true, false);
        Mixer.Info mixerInfo = new Mixer.Info("Duplex", "Test", "Test mixer", "1") {};
        FakeTargetDataLine selectedLine = new FakeTargetDataLine(selectedFormat, new byte[0]);
        Mixer mixer = mixer(mixerInfo, selectedLine);
        AudioDeviceManager.AudioDevice device =
                new AudioDeviceManager.AudioDevice("Duplex", mixerInfo);
        Settings.SELECTED_OUTPUT_DEVICE = "Duplex";

        try (AutoCloseable ignored = tz.deadparrot.utils.AudioDeviceManagerTestSupport.install(
                new Mixer.Info[]{mixerInfo}, info -> mixer, info -> true)) {
            AudioRecorder recorder = new AudioRecorder(inputProvider(selectedLine), device, selectedFormat);
            assertSame(selectedLine, recorder.recorderLine);
            assertEquals(selectedFormat, recorder.dataInfo.getFormats()[0]);
            assertEquals("Duplex", outputDevice(recorder.audioPlayer).getName());

            FakeTargetDataLine fallbackLine =
                    new FakeTargetDataLine(Settings.DEFAULT_AUDIO_FORMAT, new byte[0]);
            AudioDeviceManager.AudioDevice unavailable =
                    new AudioDeviceManager.AudioDevice("Missing", mixerInfo);
            try (AutoCloseable ignoredLookup = tz.deadparrot.utils.AudioDeviceManagerTestSupport.install(
                    new Mixer.Info[]{mixerInfo}, info -> { throw new IllegalStateException("mixer gone"); },
                    info -> true)) {
                AudioRecorder fallback = new AudioRecorder(inputProvider(fallbackLine), unavailable, null);
                assertSame(fallbackLine, fallback.recorderLine);
                assertEquals(Settings.DEFAULT_AUDIO_FORMAT, fallback.dataInfo.getFormats()[0]);
            }
        }
    }

    private Mixer mixer(Mixer.Info info, TargetDataLine targetLine) {
        return (Mixer) Proxy.newProxyInstance(Mixer.class.getClassLoader(),
                new Class<?>[]{Mixer.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getTargetLineInfo" ->
                            new Line.Info[]{new DataLine.Info(TargetDataLine.class, (javax.sound.sampled.AudioFormat) null)};
                    case "getSourceLineInfo" ->
                            new Line.Info[]{new DataLine.Info(SourceDataLine.class, (javax.sound.sampled.AudioFormat) null)};
                    case "getLine" -> targetLine;
                    case "isLineSupported" -> true;
                    case "getMixerInfo" -> info;
                    case "getControls" -> new Control[0];
                    case "isControlSupported", "isOpen", "isRunning", "isActive" -> false;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> null;
                });
    }

    private AudioDeviceManager.AudioDevice outputDevice(AudioPlayer player)
            throws ReflectiveOperationException {
        Field field = AudioPlayer.class.getDeclaredField("outputDevice");
        field.setAccessible(true);
        return (AudioDeviceManager.AudioDevice) field.get(player);
    }

    @Test
    void stopWithoutRecordingThreadAndShutdownClosedLineAreSafe() throws Exception {
        FakeTargetDataLine line = new FakeTargetDataLine(Settings.AUDIO_FORMAT, new byte[0]);
        AudioRecorder recorder = createRecorder(line);

        recorder.stop();
        recorder.shutdown();
        recorder.shutdown();

        assertFalse(line.isOpen());
    }

    @Test
    void shutdownSkipsLineChecksWhenRecorderLineIsNull() throws Exception {
        AudioRecorder recorder = createRecorder(
                new FakeTargetDataLine(Settings.AUDIO_FORMAT, new byte[0]));
        recorder.recorderLine = null;
        recorder.shutdown();
    }

    @Test
    void deleteSkipsMissingAndUnsetOutputFiles() throws Exception {
        Settings.KEEP_RECORDINGS = false;
        AudioRecorder recorder = createRecorder(
                new FakeTargetDataLine(Settings.AUDIO_FORMAT, new byte[0]));

        recorder.delete();
        recorder.outputFile = tempDir.resolve("already-deleted.wav").toFile();
        recorder.delete();

        assertFalse(recorder.outputFile.exists());
    }

    @Test
    void logsUnsupportedInputButContinuesUsingTheProviderLine() throws Exception {
        FakeTargetDataLine line = new FakeTargetDataLine(Settings.AUDIO_FORMAT, new byte[0]);
        AudioRecorder recorder = new AudioRecorder(new AudioInputProvider() {
            @Override public boolean isLineSupported(DataLine.Info lineInfo) { return false; }
            @Override public TargetDataLine getLine(DataLine.Info lineInfo) { return line; }
        });

        assertEquals(line, recorder.recorderLine);
    }

    @Test
    void playsPingAndRecordingAfterNonSpyRecording() throws Exception {
        Settings.SPY_MODE = false;
        Settings.KEEP_RECORDINGS = true;
        Settings.SAVE_RECORDINGS_TO_DESKTOP = true;
        Settings.OUTPUT_DESKTOP_FOLDER_PATH = tempDir + File.separator;
        FakeTargetDataLine line = new FakeTargetDataLine(Settings.AUDIO_FORMAT, new byte[0]);
        AudioRecorder recorder = createRecorder(line);
        Settings.SPY_MODE = false;
        PlaybackStub playback = new PlaybackStub();
        recorder.audioPlayer = playback;

        recorder.record();

        assertEquals(1, playback.pingCount);
        assertEquals(1, playback.recordingCount);
        Files.deleteIfExists(recorder.outputFile.toPath());
    }

    @Test
    void stopRestoresInterruptStatusAfterInterruptedJoin() throws Exception {
        AudioRecorder recorder = createRecorder(
                new FakeTargetDataLine(Settings.AUDIO_FORMAT, new byte[0]));
        CountDownLatch threadStarted = new CountDownLatch(1);
        recorder.audioRecorderThread = new Thread(() -> {
            threadStarted.countDown();
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        });
        recorder.audioRecorderThread.start();
        assertTrue(threadStarted.await(2, TimeUnit.SECONDS));

        Thread.currentThread().interrupt();
        try {
            recorder.stop();
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
            recorder.audioRecorderThread.interrupt();
            recorder.audioRecorderThread.join(2000);
        }
    }

    @Test
    void recordRestoresInterruptStatusAndStopsBlockedAudioInput() throws Exception {
        Settings.SPY_MODE = true;
        Settings.KEEP_RECORDINGS = true;
        Settings.SAVE_RECORDINGS_TO_DESKTOP = true;
        Settings.OUTPUT_DESKTOP_FOLDER_PATH = tempDir + File.separator;
        FakeTargetDataLine line = new FakeTargetDataLine(Settings.AUDIO_FORMAT, new byte[0], true);
        AudioRecorder recorder = createRecorder(line);

        Thread.currentThread().interrupt();
        try {
            recorder.record();
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }

        recorder.audioRecorderThread.join(2000);
        assertFalse(recorder.audioRecorderThread.isAlive());
        assertFalse(line.isOpen());
        Files.deleteIfExists(recorder.outputFile.toPath());
    }

    @Test
    void stopsBlockedRecordingWhenMaximumDurationIsReached() throws Exception {
        Settings.SPY_MODE = true;
        Settings.KEEP_RECORDINGS = true;
        Settings.SAVE_RECORDINGS_TO_DESKTOP = true;
        Settings.OUTPUT_DESKTOP_FOLDER_PATH = tempDir + File.separator;
        FakeTargetDataLine line = new FakeTargetDataLine(Settings.AUDIO_FORMAT, new byte[0], true);
        AudioRecorder recorder = new AudioRecorder(inputProvider(line), 1500, 0);

        recorder.record();

        assertFalse(line.isOpen());
        assertFalse(recorder.audioRecorderThread.isAlive());
        Files.deleteIfExists(recorder.outputFile.toPath());
    }

    @Test
    void stopsAndPlaysRecordingWhenSilenceIsDetected() throws Exception {
        Settings.SPY_MODE = true;
        Settings.KEEP_RECORDINGS = true;
        Settings.SAVE_RECORDINGS_TO_DESKTOP = true;
        Settings.OUTPUT_DESKTOP_FOLDER_PATH = tempDir + File.separator;
        FakeTargetDataLine line = new FakeTargetDataLine(Settings.AUDIO_FORMAT, new byte[8]);
        AudioRecorder recorder = new AudioRecorder(inputProvider(line), 0, 60000);

        recorder.record();

        assertFalse(line.isOpen());
        assertFalse(recorder.audioRecorderThread.isAlive());
        assertTrue(Files.size(recorder.outputFile.toPath()) >= 44);
        Files.deleteIfExists(recorder.outputFile.toPath());
    }

    @Test
    void recordsAudioReadFailuresOnTheRecorderThread() throws Exception {
        Settings.SPY_MODE = true;
        Settings.KEEP_RECORDINGS = true;
        Settings.SAVE_RECORDINGS_TO_DESKTOP = true;
        Settings.OUTPUT_DESKTOP_FOLDER_PATH = tempDir + File.separator;
        AudioRecorder recorder = createRecorder(
                new FakeTargetDataLine(Settings.AUDIO_FORMAT, new byte[0]));
        recorder.recordingStream = new AudioInputStream(new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("simulated input failure");
            }
        }, Settings.AUDIO_FORMAT, 1);
        CountDownLatch uncaught = new CountDownLatch(1);
        AtomicBoolean failureReported = new AtomicBoolean();
        Thread.UncaughtExceptionHandler originalHandler =
                Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, failure) -> {
            if (failure.getCause() instanceof IOException) {
                failureReported.set(true);
            }
            uncaught.countDown();
        });

        try {
            recorder.record();
            assertTrue(uncaught.await(2, TimeUnit.SECONDS));
            assertTrue(failureReported.get());
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(originalHandler);
            Files.deleteIfExists(recorder.outputFile.toPath());
        }
    }

    @Test
    void monitoringStopsBeforeReadingAgainWhenRecordingBecomesInactive() throws Exception {
        AudioRecorder recorder = createRecorder(
                new FakeTargetDataLine(Settings.AUDIO_FORMAT, new byte[0]));
        AtomicBoolean recordingActive = new AtomicBoolean(true);
        InputStream input = new InputStream() {
            private boolean read;

            @Override
            public int read() {
                return -1;
            }

            @Override
            public int read(byte[] buffer, int offset, int length) {
                if (read) return -1;
                read = true;
                recordingActive.set(false);
                buffer[offset] = 1;
                buffer[offset + 1] = 0;
                buffer[offset + 2] = 1;
                buffer[offset + 3] = 0;
                return 4;
            }
        };
        Path recording = tempDir.resolve("inactive.wav");
        recorder.recordWithMonitoring(new AudioInputStream(input, Settings.AUDIO_FORMAT, 1),
                recording.toFile(), 0, 1000, new AtomicBoolean(), recordingActive);

        assertEquals(48, Files.size(recording));
    }

    private AudioRecorder createRecorder(TargetDataLine line) throws LineUnavailableException {
        Settings.SPY_MODE = true;
        return new AudioRecorder(inputProvider(line));
    }

    private AudioInputProvider inputProvider(TargetDataLine line) {
        return new AudioInputProvider() {
            @Override
            public boolean isLineSupported(DataLine.Info lineInfo) {
                return true;
            }

            @Override
            public TargetDataLine getLine(DataLine.Info lineInfo) {
                return line;
            }
        };
    }

    private int littleEndianInt(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF)
                | ((bytes[offset + 1] & 0xFF) << 8)
                | ((bytes[offset + 2] & 0xFF) << 16)
                | ((bytes[offset + 3] & 0xFF) << 24);
    }

    private static final class PlaybackStub extends AudioPlayer {
        private int pingCount;
        private int recordingCount;

        private PlaybackStub() {
            super(new AudioPlaybackProvider() {
                @Override public AudioInputStream getAudioInputStream(File file) { return null; }
                @Override public Clip getClip() { return null; }
            });
        }

        @Override public void playLeadingPing() { pingCount++; }
        @Override public void play(File file) { recordingCount++; }
    }

}
