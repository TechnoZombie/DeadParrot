package tz.deadparrot;

import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;

@Slf4j
public class Listener extends Thread {

    private final AudioRecorder recorder;
    private volatile boolean running = true;
    private TargetDataLine line = null;
    private boolean keepAlive = true;

    public Listener(AudioRecorder recorder) {
        this.recorder = recorder;
    }

    @Override
    public void run() {
        AudioFormat format = DeadParrotConfigs.AUDIO_FORMAT;
        while (keepAlive) {
            try {
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                if (!AudioSystem.isLineSupported(info)) {
                    log.error(Constants.LINE_NOT_SUPPORTED);
                    return;
                }

                line = (TargetDataLine) AudioSystem.getLine(info);
                line.open(format);
                line.start();

                log.info(Constants.LISTENING);

                byte[] buffer = new byte[4096];
                ByteArrayOutputStream out = new ByteArrayOutputStream();

                while (running) {
                    int bytesRead = line.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        if (detectSound(buffer, bytesRead)) {
                            log.info(Constants.SOUND_DETECTED);
                            line.stop();
                            line.close();
                            recorder.record();
                            break;
                        }
                    }
                }

            } catch (LineUnavailableException e) {
                log.error(Constants.LINE_UNAVAILABLE, e);
            } finally {
                if (line != null) {
                    line.stop();
                    line.close();
                }
            }
        }
    }

    private boolean detectSound(byte[] audioData, int length) {
        for (int i = 0; i < length - 1; i += 2) {
            int sample = (audioData[i + 1] << 8) | (audioData[i] & 0xff);
            if (Math.abs(sample) > 5000) { // adjust this threshold if needed
                return true;
            }
        }
        return false;
    }

    public void shutdown() {
        if (line != null && line.isOpen()) {
            line.close();
        }
        running = false;
        keepAlive = false;
    }

}
