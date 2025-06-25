package tz.deadparrot;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.AudioFormat;

/**
 * Configuration settings for DeadParrot Ham Radio Repeater
 */
@Slf4j
public class Settings {

    // Audio Format Configuration

    /**
     * Standard audio format used throughout the application.
     * Default: PCM signed, 44.1kHz, 16-bit, stereo.
     */
    public static final AudioFormat AUDIO_FORMAT = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);

    // Sound Detection Settings

    /**
     * Sensitivity threshold for sound detection.
     * Higher values make detection less sensitive.
     * Default: 5000
     */
    public static final int SOUND_DETECTION_SENSITIVITY = 5000;

    // Recording Behavior Settings

    /**
     * When true, enables spy mode (audio recording only, no other features).
     * Default: false
     */
    public static final boolean SPY_MODE = false;

    /**
     * When true, all recordings are kept permanently.
     * Default: false
     */
    public static boolean KEEP_RECORDINGS = false;

    // Silence Detection Settings

    /**
     * Amplitude threshold below which audio is considered silence.
     * Higher values require more quiet audio to trigger silence detection.
     * Default: 500
     */
    public static final int SILENCE_THRESHOLD = 500;

    /**
     * Duration of continuous silence required before stopping recording.
     * Default: 1000ms (1 second)
     */
    public static final int SILENCE_DURATION_MS = 1500;

    /**
     * Maximum allowed recording duration.
     * Default: 60000ms (60 seconds)
     */
    public static final int MAX_RECORDING_TIME_MS = 60000;
}