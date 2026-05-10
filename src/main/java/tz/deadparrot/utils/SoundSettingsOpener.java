package tz.deadparrot.utils;

import lombok.extern.slf4j.Slf4j;
import tz.deadparrot.Constants;

import java.io.IOException;

@Slf4j
public class SoundSettingsOpener {

    /**
     * Opens Sound Settings directly to the Recording tab
     */
    public static void openRecordingSettings() {

        if (Constants.IS_WINDOWS) {
            try {
                new ProcessBuilder("ms-settings:sound-devices").start();
            } catch (IOException e) {
                try {
                    new ProcessBuilder("control", "mmsys.cpl,,1").start();
                    log.info(Constants.OPENED_OS_RECORDING_SETTINGS);
                } catch (IOException fallbackException) {
                    log.error(Constants.FAILED_TO_OPEN_SOUND_SETTINGS + fallbackException.getMessage());
                }
            }
        } else if (Constants.IS_LINUX) {
            try {
                // Try GNOME
                new ProcessBuilder("gnome-control-center", "sound").start();
            } catch (IOException gnomeFail) {
                try {
                    // Try PulseAudio control
                    new ProcessBuilder("pavucontrol").start();
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
            ProcessBuilder pb = new ProcessBuilder("rundll32.exe", "shell32.dll,Control_RunDLL", "mmsys.cpl,,1");
            pb.start();
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
                new ProcessBuilder("control", "mmsys.cpl,,0").start();
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
