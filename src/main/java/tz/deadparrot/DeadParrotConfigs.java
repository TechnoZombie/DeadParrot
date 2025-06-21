package tz.deadparrot;

import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.AudioFormat;

@Slf4j
public class DeadParrotConfigs {

    /*
     * Audio format config
     */
    public static final AudioFormat AUDIO_FORMAT = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            44100,
            16,
            2,
            4,
            44100,
            false);


    /*
     * AudioRecorder configs
     */

    // Configuration for silence detection
    static final int SILENCE_THRESHOLD = 500; // Adjust based on your needs
    static final int SILENCE_DURATION_MS = 1000; // Stop after 1.0 seconds of silence
    static final int MAX_RECORDING_TIME_MS = 60000; // 60 seconds max


}
