package tz.deadparrot;

import lombok.extern.slf4j.Slf4j;
import tz.deadparrot.utils.AudioDeviceManager;
import tz.deadparrot.utils.FileUtils;
import tz.deadparrot.utils.WaveWriterUtil;

import javax.sound.sampled.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class AudioRecorder {
    private AudioFormat selectedAudioFormat;
    DataLine.Info dataInfo;
    TargetDataLine recorderLine;
    AudioInputStream recordingStream;
    AudioPlayer audioPlayer;
    File outputFile;
    Thread audioRecorderThread;
    WaveWriterUtil waveWriterUtil = new WaveWriterUtil();

    public AudioRecorder() throws LineUnavailableException {
        this(null, null);
    }

    public AudioRecorder(AudioDeviceManager.AudioDevice inputDevice, AudioFormat audioFormat) throws LineUnavailableException {
        // Determine which format to use
        if (audioFormat != null) {
            this.selectedAudioFormat = audioFormat;
        } else {
            this.selectedAudioFormat = Settings.AUDIO_FORMAT;
        }

        // Initialize audio player with selected output device
        AudioDeviceManager.AudioDevice outputDevice = null;
        if (Settings.SELECTED_OUTPUT_DEVICE != null) {
            java.util.List<AudioDeviceManager.AudioDevice> devices = AudioDeviceManager.getOutputDevices();
            outputDevice = AudioDeviceManager.findDeviceByName(Settings.SELECTED_OUTPUT_DEVICE, devices);
        }
        this.audioPlayer = new AudioPlayer(outputDevice);

        // Determine which device to use
        Mixer selectedMixer = null;
        if (inputDevice != null) {
            try {
                selectedMixer = AudioSystem.getMixer(inputDevice.getMixerInfo());
                log.info("Using selected input device: " + inputDevice.getName());
            } catch (Exception e) {
                log.warn("Failed to open selected input device, falling back to default: " + e.getMessage());
                selectedMixer = null;
            }
        }

        // Get the data line info
        dataInfo = new DataLine.Info(TargetDataLine.class, selectedAudioFormat);

        if (!AudioSystem.isLineSupported(dataInfo)) {
            log.error(Constants.LINE_NOT_SUPPORTED);
        }

        try {
            if (selectedMixer != null) {
                recorderLine = (TargetDataLine) selectedMixer.getLine(dataInfo);
            } else {
                recorderLine = (TargetDataLine) AudioSystem.getLine(dataInfo);
            }
            recordingStream = new AudioInputStream(recorderLine);
            log.info(Constants.LINE_IN_READY);
        } catch (LineUnavailableException e) {
            log.error("Failed to get recording line: " + e.getMessage());
            throw e;
        }
    }

    public void record() throws LineUnavailableException {
        outputFile = FileUtils.generateOutputFile();
        recorderLine.open();
        recorderLine.start();

        // Flags for controlling recording
        AtomicBoolean recordingActive = new AtomicBoolean(true);
        AtomicBoolean silenceDetected = new AtomicBoolean(false);

        audioRecorderThread = new Thread(() -> {
            try {
                recordWithMonitoring(recordingStream, outputFile,
                        Settings.SILENCE_THRESHOLD, Settings.SILENCE_DURATION_MS,
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
                if (System.currentTimeMillis() - startTime >= Settings.MAX_RECORDING_TIME_MS) {
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

        // Play only if SPY_MODE is inactive
        if (!Settings.SPY_MODE) {
            audioPlayer.playLeadingPing();
            audioPlayer.play(outputFile);
        }
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
            waveWriterUtil.startWriteWavHeader(bos, format);

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
            waveWriterUtil.startUpdateWavHeader(outputFile, totalBytesWritten);
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
        if (outputFile != null && outputFile.exists() && !Settings.KEEP_RECORDINGS) {
            outputFile.delete();
        }
    }
}
