package tz.deadparrot.utils;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;

interface AudioDeviceProvider {
    Mixer.Info[] getMixerInfo();

    Mixer getMixer(Mixer.Info info);

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
