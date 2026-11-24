package tz.deadparrot;

import lombok.extern.slf4j.Slf4j;
import tz.deadparrot.utils.AudioLevelAnalyzer;
import tz.deadparrot.utils.AudioDeviceManager;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;

@Slf4j
public class Listener extends Thread {

    private final AudioRecorder recorder;
    private volatile boolean running = true;
    private TargetDataLine line = null;
    private boolean keepAlive = true;
    private final AudioInputProvider audioInputProvider;
    private final AudioFormat audioFormat;
    private final AudioDeviceManager.AudioDevice inputDevice;

    public Listener(AudioRecorder recorder) {
        this(recorder, null, null, AudioInputProvider.SYSTEM);
    }

    public Listener(AudioRecorder recorder, AudioDeviceManager.AudioDevice inputDevice, AudioFormat audioFormat) {
        this(recorder, inputDevice, audioFormat, AudioInputProvider.SYSTEM);
    }

    Listener(AudioRecorder recorder, AudioInputProvider audioInputProvider) {
        this(recorder, null, null, audioInputProvider);
    }

    Listener(AudioRecorder recorder, AudioDeviceManager.AudioDevice inputDevice, AudioFormat audioFormat, AudioInputProvider audioInputProvider) {
        this.recorder = recorder;
        this.inputDevice = inputDevice;
        this.audioFormat = audioFormat != null ? audioFormat : Settings.DEFAULT_AUDIO_FORMAT;
        this.audioInputProvider = audioInputProvider;
    }

    @Override
    public void run() {
        while (keepAlive) {
            try {
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
                if (!audioInputProvider.isLineSupported(info)) {
                    log.error(Constants.LINE_NOT_SUPPORTED);
                    return;
                }

                if (inputDevice != null && inputDevice.getMixerInfo() != null) {
                    try {
                        Mixer mixer = AudioDeviceManager.getMixer(inputDevice.getMixerInfo());
                        line = (TargetDataLine) mixer.getLine(info);
                    } catch (Exception e) {
                        log.warn("Failed to use selected input device for listener, falling back to default: " + e.getMessage());
                        line = audioInputProvider.getLine(info);
                    }
                } else {
                    line = audioInputProvider.getLine(info);
                }

                line.open(audioFormat);
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
