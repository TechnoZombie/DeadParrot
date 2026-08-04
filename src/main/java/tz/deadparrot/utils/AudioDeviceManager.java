package tz.deadparrot.utils;

import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.*;
import java.util.*;

/**
 * AudioDeviceManager
 * Utility class to discover, manage, and select audio input/output devices and formats.
 */
@Slf4j
public class AudioDeviceManager {

    /**
     * Represents an audio device with its name and mixer info
     */
    public static class AudioDevice {
        private final String name;
        private final Mixer.Info mixerInfo;

        public AudioDevice(String name, Mixer.Info mixerInfo) {
            this.name = name;
            this.mixerInfo = mixerInfo;
        }

        public String getName() {
            return name;
        }

        public Mixer.Info getMixerInfo() {
            return mixerInfo;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Get all available audio input devices (recording devices)
     */
    public static List<AudioDevice> getInputDevices() {
        List<AudioDevice> inputDevices = new ArrayList<>();
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();

        for (Mixer.Info mixerInfo : mixers) {
            try {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                Line.Info[] lineInfos = mixer.getTargetLineInfo();

                if (lineInfos.length > 0) {
                    // Check if this mixer supports TargetDataLine (input)
                    for (Line.Info lineInfo : lineInfos) {
                        if (TargetDataLine.class.isAssignableFrom(lineInfo.getLineClass())) {
                            inputDevices.add(new AudioDevice(mixerInfo.getName(), mixerInfo));
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Error checking mixer {}: {}", mixerInfo.getName(), e.getMessage());
            }
        }

        if (inputDevices.isEmpty()) {
            log.warn("No input devices found");
        }
        return inputDevices;
    }

    /**
     * Get all available audio output devices (playback devices)
     */
    public static List<AudioDevice> getOutputDevices() {
        List<AudioDevice> outputDevices = new ArrayList<>();
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();

        for (Mixer.Info mixerInfo : mixers) {
            try {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                Line.Info[] lineInfos = mixer.getSourceLineInfo();

                if (lineInfos.length > 0) {
                    // Check if this mixer supports SourceDataLine (output)
                    for (Line.Info lineInfo : lineInfos) {
                        if (SourceDataLine.class.isAssignableFrom(lineInfo.getLineClass())) {
                            outputDevices.add(new AudioDevice(mixerInfo.getName(), mixerInfo));
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Error checking mixer {}: {}", mixerInfo.getName(), e.getMessage());
            }
        }

        if (outputDevices.isEmpty()) {
            log.warn("No output devices found");
        }
        return outputDevices;
    }

    /**
     * Get all common audio formats
     */
    public static List<AudioFormat> getCommonFormats() {
        return Arrays.asList(
                new AudioFormat(8000f, 16, 1, true, false),
                new AudioFormat(16000f, 16, 1, true, false),
                new AudioFormat(22050f, 16, 1, true, false),
                new AudioFormat(44100f, 8, 1, true, false),
                new AudioFormat(44100f, 16, 1, true, false),
                new AudioFormat(44100f, 16, 2, true, false),
                new AudioFormat(44100f, 24, 2, true, false),
                new AudioFormat(48000f, 24, 2, true, false),
                new AudioFormat(96000f, 24, 2, true, false)
        );
    }

    /**
     * Get all audio formats supported by the given input device
     */
    public static List<AudioFormat> getSupportedFormatsForInputDevice(AudioDevice device) {
        List<AudioFormat> supportedFormats = new ArrayList<>();

        try {
            Mixer mixer = AudioSystem.getMixer(device.getMixerInfo());
            Line.Info[] lineInfos = mixer.getTargetLineInfo();

            for (Line.Info lineInfo : lineInfos) {
                if (!TargetDataLine.class.isAssignableFrom(lineInfo.getLineClass())) {
                    continue;
                }

                try {
                    TargetDataLine line = (TargetDataLine) mixer.getLine(lineInfo);
                    for (AudioFormat format : getCommonFormats()) {
                        try {
                            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                            if (AudioSystem.isLineSupported(info)) {
                                line.open(format);
                                supportedFormats.add(format);
                                line.close();
                            }
                        } catch (LineUnavailableException e) {
                            // Format not supported, continue
                        }
                    }
                    break;
                } catch (LineUnavailableException e) {
                    log.warn("Line unavailable for device {}: {}", device.getName(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error getting supported formats for device {}: {}", device.getName(), e.getMessage());
        }

        return supportedFormats;
    }

    /**
     * Get display string for audio format
     */
    public static String formatToString(AudioFormat format) {
        return String.format("%d Hz, %d-bit, %d ch",
                (int) format.getSampleRate(),
                format.getSampleSizeInBits(),
                format.getChannels());
    }

    /**
     * Find device by name
     */
    public static AudioDevice findDeviceByName(String name, List<AudioDevice> devices) {
        return devices.stream()
                .filter(d -> d.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get default input device (usually the system default)
     */
    public static AudioDevice getDefaultInputDevice() {
        List<AudioDevice> devices = getInputDevices();
        return devices.isEmpty() ? null : devices.get(0);
    }

    /**
     * Get default output device (usually the system default)
     */
    public static AudioDevice getDefaultOutputDevice() {
        List<AudioDevice> devices = getOutputDevices();
        return devices.isEmpty() ? null : devices.get(0);
    }
}

