package tz.deadparrot;

import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.AudioFormat;

@Slf4j
public class Settings {

    /*
     * Audio format settings
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
     * Listener settings
     */

    // Sensitivity for sound detection
    static final int SOUND_DETECTION_SENSITIVITY = 5000; // Default: 5000. The higher the value the less sensitive it is.

    /*
     * AudioRecorder settings
     */

    // Keep all recordings
    public static final boolean KEEP_RECORDINGS = false; // Default: false

    // Settings for silence detection
    static final int SILENCE_THRESHOLD = 500; // Default: 500. The higher the value more time it needs to be silent.
    static final int SILENCE_DURATION_MS = 1000; // Stop recording after X seconds of silence - set in milliseconds
    static final int MAX_RECORDING_TIME_MS = 60000; // Max recording time - set in milliseconds


}
