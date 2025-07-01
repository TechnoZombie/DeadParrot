package tz.deadparrot;

import org.slf4j.Marker;

import java.time.format.DateTimeFormatter;

public class Constants {

    // Processor
    public static final String CREATED_FOLDER = "[OK] Created \"Recordings\" folder.";
    public static final String FOLDER_EXISTS = "[OK] \"Recordings\" folder exists.";
    public static final String KEEP_RECORDINGS_IS_ON = "⚠️ KEEP_RECORDINGS IS ACTIVE IN SETTINGS. RECORDINGS WILL NOT BE DELETED!️";
    public static final String LINE_UNAVAILABLE = "Line is Unavailable";
    public static final String SHUTTING_DOWN = "Shutting down DeadParrot...";
    public static final String SHUT_DOWN_COMPLETE = "[OFF]: DeadParrot is no more.";
    public static final String SPY_MODE_IS_ON = "⚠️ SPY_MODE IS ON! RECORDING ONLY, NO PLAYBACK!️";
    public static final String ERROR_CREATING_FOLDER = "Error creating directory: ";
    public static final String OPENED_WINDOWS_RECORDING_SETTINGS = "Opened Windows Sound Settings - Recording";
    public static final String FAILED_TO_OPEN_REC_SETTINGS = "Failed to Open Sound Settings: ";
    public static final String RUNNING_IN_STANDARD_MODE = "DeadParrot Mode: STANDARD.";
    public static final String RUNNING_IN_MARKER_MODE = "DeadParrot Mode: MARKER ONLY.";

    // AudioRecorder
    public static final String LINE_IN_READY = "Line-In Ready.";
    public static final String OUTPUT_TEMP_FILE_NAME = "tempRecord.wav";
    public static final String OUTPUT_FOLDER_PATH = "Recordings/";
    public static final String FILENAME_PREFIX = "DeadParrot-recording_";
    public static final String FILENAME_EXTENSION = ".wav";
    public static final String RECORDING_STARTED = "Recording started...";
    public static final String RECORDING_FINISHED = "Recording finished!";
    public static final String RECORDING_FAILED = "Recording failed: ";
    public static final String RECORDING_INTERRUPTED = "Recording interrupted";
    public static final String STOP_INTERRUPTED = "Interrupted while finalizing audio file.";
    public static final String MAX_TIME_REACHED = "Maximum recording time reached!";
    public static final String SILENCE_DETECTED = "Silence detected!";
    public static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy_HH.mm.ss");

    // AudioPlayer
    public static final String ERROR_COPY_TO_TEMP = "Failed to copy resource to temp file";
    public static final String FILE_DOESNT_EXIST = "File does not exist.";
    public static final String LEADING_PING_FILE_PATH = "/leadingPing.wav";
    public static final String MARKER_FILE_PATH = "/Marker.wav";
    public static final String PLAYBACK_STARTED = "Playback started...";
    public static final String PLAYBACK_FINISHED = "Playback finished!";
    public static final String PLAYING_LEADING_PING = "Playing leading ping...";
    public static final String ERROR_DURING_PLAYBACK = "Error during playback";
    public static final String MARKER_COUNT = "Marker count: ";

    // Listener
    public static final String LISTENING = "Listening for Audio...";
    public static final String SOUND_DETECTED = "Audio detected!";
    public static final String LINE_NOT_SUPPORTED = "Line not supported.";

};