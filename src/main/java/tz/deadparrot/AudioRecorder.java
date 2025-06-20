package tz.deadparrot;

import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

@Slf4j
public class AudioRecorder {
    private static final AudioFormat audioFormat = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            44100,
            16,
            2,
            4,
            44100,
            false);

    DataLine.Info dataInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
    TargetDataLine targetLine;
    AudioInputStream recordingStream;

    File outputFile = new File(Constants.FILE_PATH);

    Thread audioRecorderThread;

    public AudioRecorder() throws LineUnavailableException {
        if (!AudioSystem.isLineSupported(dataInfo)) {
            log.error(Constants.LINE_NOT_SUPPORTED);
        }

        targetLine = (TargetDataLine) AudioSystem.getLine(dataInfo);
        targetLine.open();
        recordingStream = new AudioInputStream(targetLine);
        log.info(Constants.LINE_IN_STARTED);
    }

    public void record() {
        targetLine.start();

        audioRecorderThread = new Thread(() -> {
            try {
                AudioSystem.write(recordingStream, AudioFileFormat.Type.WAVE, outputFile);
            } catch (IOException e) {
                log.error(Constants.RECORDING_FAILED, e);
                throw new RuntimeException(e);
            }
        });
        audioRecorderThread.start();
        log.info(Constants.RECORDING_STARTED);
    }

    public void stop() {
        targetLine.stop();
        targetLine.close();

        try {
            audioRecorderThread.join(); // ensure the file is fully written
            log.info(Constants.RECORDING_FINISHED);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn(Constants.STOP_INTERRUPTED);
        }
    }

    public void shutdown() {
        if (targetLine != null && targetLine.isOpen()) {
            targetLine.close();
        }
    }
}
