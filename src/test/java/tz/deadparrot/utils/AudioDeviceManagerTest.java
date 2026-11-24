package tz.deadparrot.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Control;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioDeviceManagerTest {
    private final AudioDeviceProvider originalProvider = AudioDeviceManager.audioDeviceProvider;

    @AfterEach
    void restoreProvider() {
        AudioDeviceManager.audioDeviceProvider = originalProvider;
    }

    @Test
    void discoversInputOutputDevicesAndUsesFirstAsDefault() {
        TestMixer input = new TestMixer("Input",
                new Line.Info[]{new Line.Info(Line.class),
                        new DataLine.Info(TargetDataLine.class, (AudioFormat) null)},
                new Line.Info[0]);
        TestMixer output = new TestMixer("Output", new Line.Info[0],
                new Line.Info[]{new DataLine.Info(SourceDataLine.class, (AudioFormat) null)});
        TestMixer empty = new TestMixer("Empty", new Line.Info[0], new Line.Info[0]);
        TestMixer broken = new TestMixer("Broken", new Line.Info[0], new Line.Info[0]);
        broken.failGetMixer = true;
        install(input, output, empty, broken);

        List<AudioDeviceManager.AudioDevice> inputs = AudioDeviceManager.getInputDevices();
        List<AudioDeviceManager.AudioDevice> outputs = AudioDeviceManager.getOutputDevices();

        assertEquals(1, inputs.size());
        assertEquals("Input", inputs.getFirst().getName());
        assertEquals("Input", inputs.getFirst().toString());
        assertEquals("Output", outputs.getFirst().getName());
        assertEquals("Input", AudioDeviceManager.getDefaultInputDevice().getName());
        assertEquals("Output", AudioDeviceManager.getDefaultOutputDevice().getName());
        assertSame(inputs.getFirst(), AudioDeviceManager.findDeviceByName("Input", inputs));
        assertNull(AudioDeviceManager.findDeviceByName("missing", inputs));
    }

    @Test
    void returnsNullDefaultsWhenNoMixersExposeAudioLines() {
        install(new TestMixer("Empty", new Line.Info[0], new Line.Info[0]));

        assertTrue(AudioDeviceManager.getInputDevices().isEmpty());
        assertTrue(AudioDeviceManager.getOutputDevices().isEmpty());
        assertNull(AudioDeviceManager.getDefaultInputDevice());
        assertNull(AudioDeviceManager.getDefaultOutputDevice());
    }

    @Test
    void probesCommonFormatsAndClosesEachSuccessfullyOpenedLine() {
        TestMixer input = new TestMixer("Input",
                new Line.Info[]{new Line.Info(Line.class),
                        new DataLine.Info(TargetDataLine.class, (AudioFormat) null)},
                new Line.Info[0]);
        install(input);

        AudioDeviceManager.AudioDevice device = device(input);
        List<AudioFormat> formats = AudioDeviceManager.getSupportedFormatsForInputDevice(device);

        assertEquals(9, formats.size());
        assertEquals(9, input.opens);
        assertEquals(9, input.closes);
        assertEquals("44100 Hz, 16-bit, 2 ch",
                AudioDeviceManager.formatToString(formats.get(5)));
        assertEquals(9, AudioDeviceManager.getCommonFormats().size());
    }

    @Test
    void handlesUnsupportedFormatsMissingLinesAndUnavailableMixerLines() {
        TestMixer input = new TestMixer("Input",
                new Line.Info[]{new DataLine.Info(TargetDataLine.class, (AudioFormat) null)},
                new Line.Info[0]);
        install(input, new TestMixer("No target", new Line.Info[]{new Line.Info(Line.class)},
                new Line.Info[0]));
        AudioDeviceManager.audioDeviceProvider = provider(input, (info) -> false);

        assertTrue(AudioDeviceManager.getSupportedFormatsForInputDevice(device(input)).isEmpty());

        input.unavailable = true;
        AudioDeviceManager.audioDeviceProvider = provider(input, (info) -> true);
        assertTrue(AudioDeviceManager.getSupportedFormatsForInputDevice(device(input)).isEmpty());

        input.unavailable = false;
        input.targetLine = null;
        assertTrue(AudioDeviceManager.getSupportedFormatsForInputDevice(device(input)).isEmpty());
    }

    @Test
    void returnsEmptyFormatsWhenMixerLookupFails() {
        TestMixer broken = new TestMixer("Broken", new Line.Info[0], new Line.Info[0]);
        broken.failGetMixer = true;
        install(broken);

        assertTrue(AudioDeviceManager.getSupportedFormatsForInputDevice(device(broken)).isEmpty());
    }

    private void install(TestMixer... mixers) {
        AudioDeviceManager.audioDeviceProvider = provider(mixers, info -> true);
    }

    private AudioDeviceProvider provider(TestMixer mixer, FormatSupport support) {
        return provider(new TestMixer[]{mixer}, support);
    }

    private AudioDeviceProvider provider(TestMixer[] mixers, FormatSupport support) {
        return new AudioDeviceProvider() {
            @Override
            public Mixer.Info[] getMixerInfo() {
                return java.util.Arrays.stream(mixers).map(m -> m.info).toArray(Mixer.Info[]::new);
            }

            @Override
            public Mixer getMixer(Mixer.Info info) {
                for (TestMixer mixer : mixers) {
                    if (mixer.info == info) {
                        if (mixer.failGetMixer) throw new IllegalStateException("mixer unavailable");
                        return mixer.mixer;
                    }
                }
                throw new IllegalArgumentException("unknown mixer");
            }

            @Override
            public boolean isLineSupported(DataLine.Info info) {
                return support.supports(info);
            }
        };
    }

    private AudioDeviceManager.AudioDevice device(TestMixer mixer) {
        return new AudioDeviceManager.AudioDevice(mixer.info.getName(), mixer.info);
    }

    @FunctionalInterface
    private interface FormatSupport {
        boolean supports(DataLine.Info info);
    }

    private static final class TestMixer {
        private final Mixer.Info info;
        private final Mixer mixer;
        private final Line.Info[] targets;
        private final Line.Info[] sources;
        private TargetDataLine targetLine;
        private boolean unavailable;
        private boolean failGetMixer;
        private int opens;
        private int closes;

        private TestMixer(String name, Line.Info[] targets, Line.Info[] sources) {
            this.info = new Mixer.Info(name, "Test", "Test mixer", "1") {};
            this.targets = targets;
            this.sources = sources;
            this.mixer = (Mixer) Proxy.newProxyInstance(Mixer.class.getClassLoader(),
                    new Class<?>[]{Mixer.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "getTargetLineInfo" -> this.targets;
                        case "getSourceLineInfo" -> this.sources;
                        case "getLine" -> {
                            if (unavailable) throw new LineUnavailableException("busy");
                            if (targetLine == null) throw new LineUnavailableException("no target");
                            yield targetLine;
                        }
                        case "isLineSupported" -> true;
                        case "getMixerInfo" -> info;
                        case "getControls" -> new Control[0];
                        case "isControlSupported", "isOpen", "isRunning", "isActive" -> false;
                        case "toString" -> name;
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> null;
                    });
            this.targetLine = (TargetDataLine) Proxy.newProxyInstance(TargetDataLine.class.getClassLoader(),
                    new Class<?>[]{TargetDataLine.class}, (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "open" -> opens++;
                            case "close" -> closes++;
                            case "toString" -> { return "Fake target line"; }
                            default -> { }
                        }
                        return null;
                    });
        }
    }
}
