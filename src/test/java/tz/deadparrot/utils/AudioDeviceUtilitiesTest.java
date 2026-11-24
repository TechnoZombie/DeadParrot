package tz.deadparrot.utils;

import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Control;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioDeviceUtilitiesTest {

    @Test
    void probesOnlyInputTargetDataLinesAndClosesSupportedFormats() {
        TestMixer input = new TestMixer(new Line.Info[]{
                new Line.Info(Line.class),
                new DataLine.Info(TargetDataLine.class, (AudioFormat) null)
        });
        input.targetLine = input.createTargetLine();
        AudioDeviceProvider provider = provider(
                new TestMixer(new Line.Info[0]), input);

        AudioFormatProbe.probeAudioFormats(provider);

        assertEquals(9, input.opens);
        assertEquals(5, input.closes);
    }

    @Test
    void handlesUnavailableMixerLinesAndSystemsWithoutMixers() {
        TestMixer unavailable = new TestMixer(new Line.Info[]{
                new DataLine.Info(TargetDataLine.class, (AudioFormat) null)
        });
        unavailable.unavailable = true;

        AudioFormatProbe.probeAudioFormats(provider(unavailable));
        AudioFormatProbe.probeAudioFormats(provider());
    }

    @Test
    void entryPointsUseTheConfiguredDeviceProvider() {
        AudioDeviceProvider originalProbeProvider = AudioFormatProbe.audioDeviceProvider;
        AudioDeviceProvider originalListProvider = ListInputDevices.audioDeviceProvider;
        AudioDeviceProvider testProvider = provider();
        try {
            AudioFormatProbe.audioDeviceProvider = testProvider;
            ListInputDevices.audioDeviceProvider = testProvider;

            AudioFormatProbe.main(new String[0]);
            ListInputDevices.main(new String[0]);
        } finally {
            AudioFormatProbe.audioDeviceProvider = originalProbeProvider;
            ListInputDevices.audioDeviceProvider = originalListProvider;
        }
    }

    @Test
    void systemProviderCanEnumerateMixersWithoutOpeningAudioDevices() {
        Mixer.Info[] mixers = AudioDeviceProvider.SYSTEM.getMixerInfo();
        if (mixers.length > 0) {
            assertTrue(AudioDeviceProvider.SYSTEM.getMixer(mixers[0]) != null);
        }
    }

    @Test
    void listsOnlyMixersThatExposeInputLines() {
        TestMixer outputOnly = new TestMixer(new Line.Info[0]);
        TestMixer input = new TestMixer(new Line.Info[]{
                new DataLine.Info(TargetDataLine.class, (AudioFormat) null)
        });
        TestMixer.Info outputInfo = new TestMixer.Info("Output");
        TestMixer.Info inputInfo = new TestMixer.Info("Input");
        AudioDeviceProvider provider = new AudioDeviceProvider() {
            @Override
            public Mixer.Info[] getMixerInfo() {
                return new Mixer.Info[]{outputInfo, inputInfo};
            }

            @Override
            public Mixer getMixer(Mixer.Info info) {
                return info == outputInfo ? outputOnly.mixer : input.mixer;
            }
        };
        input.targetLine = input.createTargetLine();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(output));

            ListInputDevices.listInputDevices(provider);
        } finally {
            System.setOut(originalOut);
        }

        assertTrue(output.toString().contains("Mixer: Input - Test audio mixer"));
        assertTrue(output.toString().contains("  Line: "));
        assertTrue(!output.toString().contains("Mixer: Output"));
    }

    private AudioDeviceProvider provider(TestMixer... mixers) {
        List<Mixer.Info> infos = new ArrayList<>();
        for (TestMixer mixer : mixers) infos.add(mixer.info);
        return new AudioDeviceProvider() {
            @Override
            public Mixer.Info[] getMixerInfo() {
                return infos.toArray(Mixer.Info[]::new);
            }

            @Override
            public Mixer getMixer(Mixer.Info info) {
                return mixers[infos.indexOf(info)].mixer;
            }
        };
    }

    private static final class TestMixer {
        private final Mixer.Info info;
        private final Mixer mixer;
        private Line targetLine;
        private boolean unavailable;
        private int opens;
        private int closes;

        private TestMixer(Line.Info[] lines) {
            info = new Info("Test");
            mixer = (Mixer) Proxy.newProxyInstance(Mixer.class.getClassLoader(),
                    new Class<?>[]{Mixer.class}, (proxy, method, args) -> {
                        return switch (method.getName()) {
                            case "getTargetLineInfo" -> lines;
                            case "getLine" -> {
                                if (unavailable) throw new LineUnavailableException("busy");
                                yield targetLine;
                            }
                            case "getMixerInfo" -> info;
                            case "getLineInfo" -> new Line.Info(Mixer.class);
                            case "getControls" -> new Control[0];
                            case "isControlSupported" -> false;
                            case "isOpen", "isRunning", "isActive" -> false;
                            case "toString" -> "TestMixer";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> null;
                        };
                    });
        }

        private TargetDataLine createTargetLine() {
            return (TargetDataLine) Proxy.newProxyInstance(TargetDataLine.class.getClassLoader(),
                    new Class<?>[]{TargetDataLine.class}, (proxy, method, args) -> {
                        if (method.getName().equals("open")) {
                            opens++;
                            AudioFormat format = (AudioFormat) args[0];
                            if (format.getSampleSizeInBits() != 16) {
                                throw new IllegalArgumentException("unsupported format");
                            }
                        } else if (method.getName().equals("close")) {
                            closes++;
                        }
                        return null;
                    });
        }

        private static final class Info extends Mixer.Info {
            private Info(String name) {
                super(name, "Test vendor", "Test audio mixer", "1.0");
            }
        }
    }
}
