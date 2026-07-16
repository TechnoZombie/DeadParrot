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
        AudioFormat format = Settings.AUDIO_FORMAT;

        while (keepAlive) {
            try {
                line = null;

                // Find and open the first working capture device
                outer:
                for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {

                    Mixer mixer = AudioSystem.getMixer(mixerInfo);

                    for (Line.Info lineInfo : mixer.getTargetLineInfo()) {

                        if (!TargetDataLine.class.isAssignableFrom(lineInfo.getLineClass())) continue;

                        try {
                            log.info("Trying mixer: {}", mixerInfo.getName());
                            log.info("Line info: {}", lineInfo);
                            line = (TargetDataLine) mixer.getLine(lineInfo);
                            line.open(format);
                            log.info("Opened line: {}", line.getLineInfo());

                            log.info("Listener using mixer: {}", mixerInfo.getName());

                            break outer;

                        } catch (Exception ignored) {
                            // Try the next line/mixer
                        }
                    }
                }

                if (line == null) {
                    log.error("No capture device available.");
                    return;
                }

                line.start();

                log.info(Constants.LISTENING);

                byte[] buffer = new byte[4096];

                while (running) {
                    int bytesRead = line.read(buffer, 0, buffer.length);


                    if (bytesRead > 0) {

                        int max = 0;

                        for (int i = 0; i < bytesRead - 1; i += 2) {
                            int sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xff));
                            max = Math.max(max, Math.abs(sample));
                        }


                        if (max > Settings.SOUND_DETECTION_SENSITIVITY) {
                            log.info(Constants.SOUND_DETECTED);

                            line.stop();
                            line.close();
                            line = null;

                            recorder.record();
                            break;
                        }
                    }
                }

            } catch (LineUnavailableException e) {
                log.error(Constants.LINE_UNAVAILABLE, e);

            } finally {
                if (line != null) {
                    try {
                        line.stop();
                    } catch (Exception ignored) {
                    }

                    try {
                        line.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    private boolean detectSound(byte[] audioData, int length) {
        for (int i = 0; i < length - 1; i += 2) {
            int sample = (audioData[i + 1] << 8) | (audioData[i] & 0xff);
            if (Math.abs(sample) > Settings.SOUND_DETECTION_SENSITIVITY) { // adjust this threshold if needed
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
