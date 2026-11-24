package tz.deadparrot.utils;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;

interface AudioDeviceProvider {
    Mixer.Info[] getMixerInfo();

    Mixer getMixer(Mixer.Info info);

    default boolean isLineSupported(DataLine.Info info) {
        return AudioSystem.isLineSupported(info);
    }

    AudioDeviceProvider SYSTEM = new AudioDeviceProvider() {
        @Override
        public Mixer.Info[] getMixerInfo() {
            return AudioSystem.getMixerInfo();
        }

        @Override
        public Mixer getMixer(Mixer.Info info) {
            return AudioSystem.getMixer(info);
        }
    };
}
