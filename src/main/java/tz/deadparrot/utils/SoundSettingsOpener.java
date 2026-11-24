package tz.deadparrot.utils;

import lombok.extern.slf4j.Slf4j;
import tz.deadparrot.Constants;

import java.io.IOException;
import java.util.List;

@Slf4j
public class SoundSettingsOpener {
    static ProcessLauncher processLauncher = command -> new ProcessBuilder(command).start();

    @FunctionalInterface
    interface ProcessLauncher {
        Process start(List<String> command) throws IOException;
    }

    private static Process start(String... command) throws IOException {
        return processLauncher.start(List.of(command));
    }

    /**
     * Opens Sound Settings directly to the Recording tab
     */
    public static void openRecordingSettings() {

        if (Constants.IS_WINDOWS) {
            try {
                start("ms-settings:sound-devices");
            } catch (IOException e) {
                try {
                    start("control", "mmsys.cpl,,1");
                    log.info(Constants.OPENED_OS_RECORDING_SETTINGS);
                } catch (IOException fallbackException) {
                    log.error(Constants.FAILED_TO_OPEN_SOUND_SETTINGS + fallbackException.getMessage());
                }
            }
        } else if (Constants.IS_LINUX) {
            try {
                // Try GNOME
                start("gnome-control-center", "sound");
            } catch (IOException gnomeFail) {
                try {
                    // Try PulseAudio control
                    start("pavucontrol");
                } catch (IOException pulseFail) {
                    log.error("Could not open Linux recording settings: " + pulseFail.getMessage());
                }
            }
        }
    }

    /**
     * Alternative method using rundll32 (more reliable for older Windows versions)
     */
    public static void openRecordingSettingsClassic() {
        try {
            // Opens the classic Sound control panel directly to Recording tab
            start("rundll32.exe", "shell32.dll,Control_RunDLL", "mmsys.cpl,,1");
            log.info(Constants.OPENED_OS_RECORDING_SETTINGS);

        } catch (IOException e) {
            log.error(Constants.FAILED_TO_OPEN_SOUND_SETTINGS + e.getMessage());
        }
    }

    /**
     * Opens Sound Settings directly to the Playback tab
     */
    public static void openPlaybackSettings() {

        if (Constants.IS_WINDOWS) {
            try {
                start("control", "mmsys.cpl,,0");
                log.info(Constants.OPENED_OS_PLAYBACK_SETTINGS);

            } catch (IOException e) {
                log.error(Constants.FAILED_TO_OPEN_SOUND_SETTINGS);
                log.error(e.getMessage());
            }
        } else {
            log.info(Constants.NOT_IMPLEMENTED);
        }
    }
}
