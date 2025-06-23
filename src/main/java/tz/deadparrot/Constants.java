package tz.deadparrot;

import java.time.format.DateTimeFormatter;

public class Constants {

    public static final String OUTPUT_FILE_PATH = "tempRecord.wav";
    public static final String LEADING_PING_FILE_PATH = "/leadingPing.wav";
    public static final String LISTENING = "Listening for Audio...";
    public static final String SOUND_DETECTED = "Audio detected!";
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
    public static final String MAX_TIME_REACHED = "Recording stopped: Maximum time reached";
    public static final String SILENCE_DETECTED = "Recording stopped: Silence detected";
    public static final String ERROR_COPY_TO_TEMP = "Failed to copy resource to temp file";
    public static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");
    public static final String FILENAME_PREFIX = "DeadParrot-recording_";
    public static final String FILENAME_EXTENSION = ".wav";
    public static final String PLAYING_LEADING_PING = "Playing leading ping...";
};