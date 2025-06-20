package tz.deadparrot;

import javax.sound.sampled.AudioFormat;

public class Constants {

    public static final String FILE_PATH = "tempRecord.wav";
    public static final String LISTENING = "Listening for Audio...";
    public static final String SOUND_DETECTED = "Audio detected! ";
    public static final String RECORDING_STARTED = "Recording started...";
    public static final String RECORDING_FINISHED = "Recording finished!";
    public static final String PLAYBACK_STARTED = "Playback started...";
    public static final String PLAYBACK_FINISHED = "Playback finished!";
    public static final String LINE_NOT_SUPPORTED = "Line not supported.";
    public static final String LINE_IN_STARTED = "Line-In Open.";
    public static final String LINE_UNAVAILABLE = "Line is Unavailable";
    public static final String RECORDING_FAILED = "Recording failed: ";
    public static final String FILE_DOESNT_EXIST = "File does not exist.";
    public static final String SHUTTING_DOWN = "Shutting down DeadParrot...";
    public static final String SHUT_DOWN_COMPLETE = "DeadParrot is no more.";
    public static final String RECORDING_INTERRUPTED = "Recording interrupted";
    public static final String STOP_INTERRUPTED = "Interrupted while finalizing audio file.";
    public static final AudioFormat AUDIO_FORMAT = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            44100,
            16,
            2,
            4,
            44100,
            false);
};
