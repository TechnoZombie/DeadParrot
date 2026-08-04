package tz.deadparrot;

import lombok.extern.slf4j.Slf4j;
import tz.deadparrot.utils.AudioDeviceManager;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;

@Slf4j
public class Listener extends Thread {

    private final AudioRecorder recorder;
    private volatile boolean running = true;
    private TargetDataLine line = null;
    private boolean keepAlive = true;
    private final AudioFormat audioFormat;
    private final AudioDeviceManager.AudioDevice inputDevice;

    public Listener(AudioRecorder recorder) {
        this(recorder, null, null);
    }

    public Listener(AudioRecorder recorder, AudioDeviceManager.AudioDevice inputDevice, AudioFormat audioFormat) {
        this.recorder = recorder;
        this.inputDevice = inputDevice;
        this.audioFormat = audioFormat != null ? audioFormat : Settings.DEFAULT_AUDIO_FORMAT;
    }

    @Override
    public void run() {
        while (keepAlive) {
            try {
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
                if (!AudioSystem.isLineSupported(info)) {
                    log.error(Constants.LINE_NOT_SUPPORTED);
                    return;
                }

                // Use selected input device if available
                if (inputDevice != null && inputDevice.getMixerInfo() != null) {
                    try {
                        Mixer mixer = AudioSystem.getMixer(inputDevice.getMixerInfo());
                        line = (TargetDataLine) mixer.getLine(info);
                    } catch (Exception e) {
                        log.warn("Failed to use selected input device for listener, falling back to default: " + e.getMessage());
                        line = (TargetDataLine) AudioSystem.getLine(info);
                    }
                } else {
                    line = (TargetDataLine) AudioSystem.getLine(info);
                }

                line.open(audioFormat);
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
        // Handle different sample sizes
        if (audioFormat.getSampleSizeInBits() == 16) {
            // 16-bit audio
            for (int i = 0; i < length - 1; i += 2) {
                int sample;
                if (audioFormat.isBigEndian()) {
                    sample = (audioData[i] << 8) | (audioData[i + 1] & 0xff);
                } else {
                    sample = (audioData[i + 1] << 8) | (audioData[i] & 0xff);
                }
                if (Math.abs(sample) > Settings.SOUND_DETECTION_SENSITIVITY) {
                    return true;
                }
            }
        } else if (audioFormat.getSampleSizeInBits() == 8) {
            // 8-bit audio
            for (int i = 0; i < length; i++) {
                int sample = audioData[i] & 0xff;
                if (Math.abs(sample - 128) > Settings.SOUND_DETECTION_SENSITIVITY) {
                    return true;
                }
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
