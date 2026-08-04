package tz.deadparrot;

public class GUILabels {

    /**
     * Control Panel
     */
    public static final String CONTROL_PANEL_TITLE = "RepeaterControls";
    public static final String START_BUTTON = "▶ START";
    public static final String STOP_BUTTON = "■ STOP";

    // Options Panel
    public static final String OPTIONS_PANEL_TITLE = "Options";

    // Checkboxes
    public static final String SPY_MODE_CHECKBOX = "Spy Mode";
    public static final String MARKER_MODE_CHECKBOX = "Marker Mode";
    public static final String KEEP_RECORDINGS_CHECKBOX = "Keep Recordings";
    public static final String PRINT_SETTINGS_CHECKBOX = "Print Settings on Start";
    public static final String OPEN_OS_RECORDING_SETTINGS_CHECKBOX = "Open OS Recording Settings";
    public static final String EASTER_EGG_CHECKBOX = "Easter Egg";
    public static final String DARK_MODE_CHECKBOX = "Dark Mode";

    // Tooltips
    public static final String SPY_MODE_CHECKBOX_TOOLTIP = "Enable spy mode - automatically enables keep recordings and disables marker mode.";
    public static final String MARKER_MODE_CHECKBOX_TOOLTIP = "Enable marker mode - only plays audio marker at set intervals.";
    public static final String KEEP_RECORDINGS_CHECKBOX_TOOLTIP = "Keep all recorded audio files permanently.";
    public static final String PRINT_SETTINGS_CHECKBOX_TOOLTIP = "Print Settings on Start";
    public static final String OPEN_OS_RECORDING_SETTINGS_CHECKBOX_TOOLTIP = "Open OS sound recording settings on start.";
    public static final String EASTER_EGG_CHECKBOX_TOOLTIP = "Activate the Easter Egg!";
    public static final String DARK_MODE_CHECKBOX_TOOLTIP = "Enable Dark Mode for the interface.";

    /**
     * Save Directory Panel
     */
    public static final String DIRECTORY_PANEL_TITLE = "Save Directory";

    // Buttons
    public static final String SAVE_TO_DESKTOP_BUTTON = "Save to Desktop";
    public static final String CHOOSE_DIRECTORY_BUTTON = "Choose Directory";
    public static final String OPEN_DIRECTORY_BUTTON = "Open Directory";

    // Tooltips
    public static final String SAVE_TO_DESKTOP_BUTTON_TOOLTIP = "Save recordings to desktop folder.";
    public static final String CHOOSE_DIRECTORY_BUTTON_TOOLTIP = "Choose a custom directory for recordings.";
    public static final String OPEN_DIRECTORY_BUTTON_TOOLTIP = "Open recordings folder.";

    public static final String DIR_NOT_SET = "Dir: Not Set";
    public static final String DIR_DESKTOP = "Dir: Desktop";
    public static final String DIR = "Dir: ";

    /**
     * Audio Controls Panel
     */
    public static final String AUDIO_CONTROLS_PANEL_TITLE = "Audio Controls";

    // Buttons
    public static final String AUDIO_PROBE_BUTTON = "Run Audio Probe";
    public static final String OPEN_PLAYBACK_SETTINGS_BUTTON = "Open Playback Settings";
    public static final String OPEN_RECORDING_SETTINGS_BUTTON = "Open Recording Settings";

    // Tooltips
    public static final String AUDIO_PROBE_BUTTON_TOOLTIP = "Probe available audio formats and devices.";
    public static final String OPEN_PLAYBACK_SETTINGS_BUTTON_TOOLTIP = "Open System Audio Playback Settings.";
    public static final String OPEN_RECORDING_SETTINGS_BUTTON_TOOLTIP = "Open System Audio Recording Settings.";

    /**
     * Audio Device Selection Panel
     */
    public static final String AUDIO_DEVICE_PANEL_TITLE = "Audio Device Selection";

    // Labels
    public static final String INPUT_DEVICE_LABEL = "Input Device:";
    public static final String OUTPUT_DEVICE_LABEL = "Output Device:";
    public static final String AUDIO_FORMAT_LABEL = "Audio Format:";

    // Tooltips
    public static final String INPUT_DEVICE_TOOLTIP = "Select the audio input (recording) device to use.";
    public static final String OUTPUT_DEVICE_TOOLTIP = "Select the audio output (playback) device to use.";
    public static final String AUDIO_FORMAT_TOOLTIP = "Select the audio format for recording.";

    // Logs
    public static final String LOG_INPUT_DEVICE_CHANGED = "Input device changed to: ";
    public static final String LOG_OUTPUT_DEVICE_CHANGED = "Output device changed to: ";
    public static final String LOG_AUDIO_FORMAT_CHANGED = "Audio format changed to: ";
    public static final String LOG_DEVICES_LOADING = "Loading available audio devices...";

    /**
     *  Console Panel
     */
    public static final String CONSOLE_PANEL_TITLE = "Console Output";

    // Buttons
    public static final String CLEAR_CONSOLE_BUTTON = "Clear Console";

    // Status Panel
    public static final String PIPE_SPACED = " | ";
    public static final String STATUS_STOPPED = "Status: Stopped";
    public static final String STATUS_RUNNING = "Status: Running";
    public static final String STATUS_RECORDING_IDLE = "Audio Recorder: Idle";
    public static final String STATUS_RECORDING_ACTIVE = "Audio Recorder: Active";
    public static final String STATUS_LISTENER_IDLE = "Listener: Idle";
    public static final String STATUS_LISTENER_ACTIVE = "Listener: Active";

    /**
     * Logs
      */
    public static final String LOG_STOP_BEFORE_RUNNING = "Stop the repeater before running the audio probe.";
    public static final String LOG_AUDIO_PROBE_RUNNING = "Running audio probe...";
    public static final String LOG_AUDIO_PROBE_COMPLETED = "Audio probe completed!";
    public static final String LOG_ERROR_STARTING_REPEATER = "Error starting repeater: ";
    public static final String LOG_ERROR_STOPPING_REPEATER = "Error stopping repeater: ";
    public static final String LOG_FAILED_TO_START_REPEATER = "Failed to start repeater: ";
    public static final String LOG_SETTINGS_LOADED_FROM_CONFIG = "Settings loaded from configuration";
    public static final String LOG_FAILED_TO_SET_LOOK_AND_FEEL = "Failed to set look and feel";
    public static final String LOG_SAVING_RECORDINGS_TO = "Saving recordings to: ";
    public static final String LOG_SAVING_RECORDINGS_TO_DESKTOP = "Saving recordings to desktop folder.";
    public static final String LOG_MARKER_MODE = "Marker Mode: ";
    public static final String LOG_KEEP_RECORDINGS = "Keep Recordings: ";
    public static final String LOG_PRINT_SETTINGS_ON_START = "Print Settings on Start: ";
    public static final String LOG_OPEN_OS_SETTINGS = "Open OS Settings: ";
    public static final String LOG_EASTER_EGG = "Easter Egg: ";
    public static final String LOG_DARK_MODE = "Dark Mode: ";
}


