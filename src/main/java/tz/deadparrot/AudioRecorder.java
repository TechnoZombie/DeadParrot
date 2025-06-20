package tz.deadparrot;

import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

@Slf4j
public class AudioRecorder {
    DataLine.Info dataInfo = new DataLine.Info(TargetDataLine.class, Constants.AUDIO_FORMAT);
    TargetDataLine targetLine;
    AudioInputStream recordingStream;

    AudioPlayer audioPlayer = new AudioPlayer();

    File outputFile = new File(Constants.FILE_PATH);

    Thread audioRecorderThread;

    public AudioRecorder() throws LineUnavailableException {
        if (!AudioSystem.isLineSupported(dataInfo)) {
            log.error(Constants.LINE_NOT_SUPPORTED);
        }

        targetLine = (TargetDataLine) AudioSystem.getLine(dataInfo);
        recordingStream = new AudioInputStream(targetLine);
        log.info(Constants.LINE_IN_STARTED);
    }

    public void record() throws LineUnavailableException {
        targetLine.open();
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
        // stop after 10 secs.
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn(Constants.RECORDING_INTERRUPTED);
        }
        stop();
        audioPlayer.play(outputFile);

    }

    public void stop() {
        targetLine.stop();
        targetLine.close();

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
        if (targetLine != null && targetLine.isOpen()) {
            targetLine.close();
        }
        delete();
    }

    public void delete() {
        if (outputFile.exists()) {
            outputFile.delete();
        }
    }
}
