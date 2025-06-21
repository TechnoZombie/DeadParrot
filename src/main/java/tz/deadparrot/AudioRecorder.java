package tz.deadparrot;

import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class AudioRecorder {
    DataLine.Info dataInfo = new DataLine.Info(TargetDataLine.class, DeadParrotConfigs.AUDIO_FORMAT);
    TargetDataLine recorderLine;
    AudioInputStream recordingStream;

    AudioPlayer audioPlayer = new AudioPlayer();

    File outputFile = new File(Constants.OUTPUT_FILE_PATH);
    File leadingPing = new File(Constants.LEADING_PING_FILE_PATH);

    Thread audioRecorderThread;

    public AudioRecorder() throws LineUnavailableException {
        if (!AudioSystem.isLineSupported(dataInfo)) {
            log.error(Constants.LINE_NOT_SUPPORTED);
        }

        recorderLine = (TargetDataLine) AudioSystem.getLine(dataInfo);
        recordingStream = new AudioInputStream(recorderLine);
        log.info(Constants.LINE_IN_STARTED);
    }

    public void record() throws LineUnavailableException {
        recorderLine.open();
        recorderLine.start();

        // Flags for controlling recording
        AtomicBoolean recordingActive = new AtomicBoolean(true);
        AtomicBoolean silenceDetected = new AtomicBoolean(false);

        audioRecorderThread = new Thread(() -> {
            try {
                recordWithMonitoring(recordingStream, outputFile,
                        DeadParrotConfigs.SILENCE_THRESHOLD, DeadParrotConfigs.SILENCE_DURATION_MS,
                        silenceDetected, recordingActive);
            } catch (IOException e) {
                log.error(Constants.RECORDING_FAILED, e);
                throw new RuntimeException(e);
            }
        });

        audioRecorderThread.start();
        log.info(Constants.RECORDING_STARTED);

        // Wait for either timeout or silence detection
        long startTime = System.currentTimeMillis();
        try {
            while (recordingActive.get() && !silenceDetected.get()) {
                Thread.sleep(100); // Check every 100ms

                // Check if we've exceeded max recording time
                if (System.currentTimeMillis() - startTime >= DeadParrotConfigs.MAX_RECORDING_TIME_MS) {
                    log.info(Constants.MAX_TIME_REACHED);
                    break;
                }
            }

            if (silenceDetected.get()) {
                log.info(Constants.SILENCE_DETECTED);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn(Constants.RECORDING_INTERRUPTED);
        }

        // Signal recording to stop
        recordingActive.set(false);
        stop();
        audioPlayer.play(leadingPing);
        audioPlayer.play(outputFile);
    }

    private void recordWithMonitoring(AudioInputStream audioStream, File outputFile,
                                      int silenceThreshold, int silenceDurationMs,
                                      AtomicBoolean silenceDetected, AtomicBoolean recordingActive)
            throws IOException {

        AudioFormat format = audioStream.getFormat();
        long lastSoundTime = System.currentTimeMillis();

        // Manual recording with monitoring
        try (FileOutputStream fos = new FileOutputStream(outputFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {

            // Write WAV header (simplified - you might want to use a proper WAV writer)
            writeWavHeader(bos, format);

            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalBytesWritten = 0;

            while (recordingActive.get() && (bytesRead = audioStream.read(buffer)) != -1) {
                // Write audio data
                bos.write(buffer, 0, bytesRead);
                totalBytesWritten += bytesRead;

                // Monitor audio level
                double audioLevel = calculateAudioLevel(buffer, bytesRead, format);

                if (audioLevel > silenceThreshold) {
                    lastSoundTime = System.currentTimeMillis();
                } else if (System.currentTimeMillis() - lastSoundTime >= silenceDurationMs) {
                    silenceDetected.set(true);
                    break;
                }
            }

            // Update WAV header with actual data size
            updateWavHeader(outputFile, totalBytesWritten);
        }
    }

    private double calculateAudioLevel(byte[] buffer, int bytesRead, AudioFormat format) {
        if (format.getSampleSizeInBits() == 16) {
            // 16-bit audio
            long sum = 0;
            int sampleCount = 0;

            for (int i = 0; i < bytesRead - 1; i += 2) {
                short sample;
                if (format.isBigEndian()) {
                    sample = (short) ((buffer[i] << 8) | (buffer[i + 1] & 0xFF));
                } else {
                    sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));
                }
                sum += Math.abs(sample);
                sampleCount++;
            }

            return sampleCount > 0 ? (double) sum / sampleCount : 0;
        } else if (format.getSampleSizeInBits() == 8) {
            // 8-bit audio
            long sum = 0;
            for (int i = 0; i < bytesRead; i++) {
                sum += Math.abs(buffer[i] - 128); // 8-bit is unsigned, center at 128
            }
            return bytesRead > 0 ? (double) sum / bytesRead : 0;
        }

        return 0; // Unsupported format
    }

    private void writeWavHeader(OutputStream out, AudioFormat format) throws IOException {
        int sampleRate = (int) format.getSampleRate();
        int channels = format.getChannels();
        int bitsPerSample = format.getSampleSizeInBits();
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        int blockAlign = channels * bitsPerSample / 8;

        // WAV header (44 bytes)
        out.write("RIFF".getBytes());
        writeInt(out, 36); // File size - 8 (will be updated later)
        out.write("WAVE".getBytes());
        out.write("fmt ".getBytes());
        writeInt(out, 16); // PCM header size
        writeShort(out, 1); // PCM format
        writeShort(out, channels);
        writeInt(out, sampleRate);
        writeInt(out, byteRate);
        writeShort(out, blockAlign);
        writeShort(out, bitsPerSample);
        out.write("data".getBytes());
        writeInt(out, 0); // Data size (will be updated later)
    }

    private void updateWavHeader(File file, long dataSize) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            // Update file size at offset 4
            raf.seek(4);
            writeInt(raf, (int) (dataSize + 36));

            // Update data size at offset 40
            raf.seek(40);
            writeInt(raf, (int) dataSize);
        }
    }

    private void writeInt(OutputStream out, int value) throws IOException {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 24) & 0xFF);
    }

    private void writeShort(OutputStream out, int value) throws IOException {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
    }

    private void writeInt(RandomAccessFile raf, int value) throws IOException {
        raf.write(value & 0xFF);
        raf.write((value >> 8) & 0xFF);
        raf.write((value >> 16) & 0xFF);
        raf.write((value >> 24) & 0xFF);
    }

    public void stop() {
        recorderLine.stop();
        recorderLine.close();

        try {
            if (this.audioRecorderThread != null) {
                audioRecorderThread.join(); // ensure the file is fully written
                log.info(Constants.RECORDING_FINISHED);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn(Constants.STOP_INTERRUPTED);
        }
    }

    public void shutdown() {
        if (recorderLine != null && recorderLine.isOpen()) {
            recorderLine.close();
        }
        delete();
    }

    public void delete() {
        if (outputFile.exists()) {
            outputFile.delete();
        }
    }
}
