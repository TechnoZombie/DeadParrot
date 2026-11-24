package tz.deadparrot.utils;

import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class AudioDeviceManagerTestSupport {
    private AudioDeviceManagerTestSupport() {
    }

    public static AutoCloseable install(Mixer.Info[] mixerInfos,
                                        Function<Mixer.Info, Mixer> mixerProvider,
                                        Predicate<DataLine.Info> lineSupport) {
        AudioDeviceProvider original = AudioDeviceManager.audioDeviceProvider;
        AudioDeviceManager.audioDeviceProvider = new AudioDeviceProvider() {
            @Override
            public Mixer.Info[] getMixerInfo() {
                return mixerInfos;
            }

            @Override
            public Mixer getMixer(Mixer.Info info) {
                return mixerProvider.apply(info);
            }

            @Override
            public boolean isLineSupported(DataLine.Info info) {
                return lineSupport.test(info);
            }
        };
        return () -> AudioDeviceManager.audioDeviceProvider = original;
    }
}
