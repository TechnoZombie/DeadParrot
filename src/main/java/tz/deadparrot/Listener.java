package tz.deadparrot;

import lombok.extern.slf4j.Slf4j;
import tz.deadparrot.utils.AudioLevelAnalyzer;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;

@Slf4j
public class Listener extends Thread {

    private final AudioRecorder recorder;
    private volatile boolean running = true;
    private TargetDataLine line = null;
    private boolean keepAlive = true;
    private final AudioInputProvider audioInputProvider;

    public Listener(AudioRecorder recorder) {
        this(recorder, AudioInputProvider.SYSTEM);
    }

    Listener(AudioRecorder recorder, AudioInputProvider audioInputProvider) {
        this.recorder = recorder;
        this.audioInputProvider = audioInputProvider;
    }

    @Override
    public void run() {
        AudioFormat format = Settings.AUDIO_FORMAT;
        while (keepAlive) {
            try {
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                if (!audioInputProvider.isLineSupported(info)) {
                    log.error(Constants.LINE_NOT_SUPPORTED);
                    return;
                }

                line = audioInputProvider.getLine(info);
                line.open(format);
                line.start();

                log.info(Constants.LISTENING);

                byte[] buffer = new byte[4096];
                ByteArrayOutputStream out = new ByteArrayOutputStream();

                while (running) {
                    int bytesRead = line.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        if (AudioLevelAnalyzer.detectSound(
                                buffer, bytesRead, Settings.SOUND_DETECTION_SENSITIVITY)) {
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

    public void shutdown() {
        if (line != null && line.isOpen()) {
            line.close();
        }
        running = false;
        keepAlive = false;
    }

}
