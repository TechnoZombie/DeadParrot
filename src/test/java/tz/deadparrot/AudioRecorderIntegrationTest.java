package tz.deadparrot;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioRecorderIntegrationTest {

    @TempDir
    Path tempDir;

    private final boolean originalKeepRecordings = Settings.KEEP_RECORDINGS;
    private final boolean originalSpyMode = Settings.SPY_MODE;
    private final boolean originalSaveToDesktop = Settings.SAVE_RECORDINGS_TO_DESKTOP;
    private final String originalDesktopPath = Settings.OUTPUT_DESKTOP_FOLDER_PATH;

    @AfterEach
    void restoreSettings() {
        Settings.KEEP_RECORDINGS = originalKeepRecordings;
        Settings.SPY_MODE = originalSpyMode;
        Settings.SAVE_RECORDINGS_TO_DESKTOP = originalSaveToDesktop;
        Settings.OUTPUT_DESKTOP_FOLDER_PATH = originalDesktopPath;
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

    private AudioRecorder createRecorder(TargetDataLine line) throws LineUnavailableException {
        Settings.SPY_MODE = true;
        return new AudioRecorder(new AudioInputProvider() {
            @Override
            public boolean isLineSupported(DataLine.Info lineInfo) {
                return true;
            }

            @Override
            public TargetDataLine getLine(DataLine.Info lineInfo) {
                return line;
            }
        });
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
