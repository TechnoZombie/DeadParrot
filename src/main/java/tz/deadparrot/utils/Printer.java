package tz.deadparrot.utils;

import lombok.extern.slf4j.Slf4j;
import tz.deadparrot.Settings;

import java.util.Map;

@Slf4j
public class Printer {

    public void printCurrentSettings() {
        Map<String, Object> settingsMap = Map.of(
                "Audio Format", Settings.AUDIO_FORMAT.toString(),
                "Sound Detection Sensitivity", Settings.SOUND_DETECTION_SENSITIVITY,
                "Spy Mode", Settings.SPY_MODE,
                "Keep Recordings", Settings.KEEP_RECORDINGS,
                "Silence Threshold", Settings.SILENCE_THRESHOLD,
                "Silence Duration (ms)", Settings.SILENCE_DURATION_MS,
                "Max Recording Time (ms)", Settings.MAX_RECORDING_TIME_MS
        );

        settingsMap.forEach((key, value) -> log.info("{}: {}", key, value));
    }

}