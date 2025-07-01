package tz.deadparrot.utils;

import lombok.extern.slf4j.Slf4j;
import tz.deadparrot.Constants;

import java.io.IOException;

@Slf4j
public class SoundSettingsOpener {

    /**
     * Opens Windows Sound Settings directly to the Recording tab
     */
    public static void openRecordingSettings() {
        try {
            // Method 1: Direct to Recording tab (Windows 10/11)
            ProcessBuilder pb = new ProcessBuilder("ms-settings:sound-devices");
            pb.start();

        } catch (IOException e) {
            // Fallback method: Open classic Sound control panel
            try {
                ProcessBuilder fallback = new ProcessBuilder("control", "mmsys.cpl,,1");
                fallback.start();
                log.info(Constants.OPENED_WINDOWS_RECORDING_SETTINGS);

            } catch (IOException fallbackException) {
                log.error(Constants.FAILED_TO_OPEN_REC_SETTINGS + fallbackException.getMessage());
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
            log.info(Constants.OPENED_WINDOWS_RECORDING_SETTINGS);

        } catch (IOException e) {
            log.error(Constants.FAILED_TO_OPEN_REC_SETTINGS + e.getMessage());
        }
    }

    /**
     * Method to open general sound settings (Windows 10/11 Settings app)
     */
    public static void openSoundSettings() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ms-settings:sound");
            pb.start();
            log.info(Constants.OPENED_WINDOWS_RECORDING_SETTINGS);

        } catch (IOException e) {
            log.error(Constants.FAILED_TO_OPEN_REC_SETTINGS + e.getMessage());
        }
    }
}
