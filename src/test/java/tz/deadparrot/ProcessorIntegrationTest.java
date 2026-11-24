package tz.deadparrot;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tz.deadparrot.utils.AudioResourcesPreloader;

import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessorIntegrationTest {

    @TempDir
    Path tempDir;

    private final boolean originalSpyMode = Settings.SPY_MODE;
    private final boolean originalMarkerMode = Settings.MARKER_MODE;
    private final boolean originalKeepRecordings = Settings.KEEP_RECORDINGS;
    private final boolean originalSaveToDesktop = Settings.SAVE_RECORDINGS_TO_DESKTOP;
    private final boolean originalSaveToCustom = Settings.SAVE_RECORDINGS_TO_CUSTOM_DIR;
    private final boolean originalOpenSettings = Settings.OPEN_OS_RECORDING_SETTINGS;
    private final boolean originalPrintSettings = Settings.PRINT_SETTINGS;
    private final boolean originalEasterEgg = Settings.EASTER_EGG;
    private final String originalCustomDirectory = Settings.CUSTOM_RECORDINGS_DIRECTORY;
    private final String originalOsName = System.getProperty("os.name");
    private final boolean originalWindows = Constants.IS_WINDOWS;
    private final boolean originalLinux = Constants.IS_LINUX;

    @AfterEach
    void restoreSettings() {
        Settings.SPY_MODE = originalSpyMode;
        Settings.MARKER_MODE = originalMarkerMode;
        Settings.KEEP_RECORDINGS = originalKeepRecordings;
        Settings.SAVE_RECORDINGS_TO_DESKTOP = originalSaveToDesktop;
        Settings.SAVE_RECORDINGS_TO_CUSTOM_DIR = originalSaveToCustom;
        Settings.OPEN_OS_RECORDING_SETTINGS = originalOpenSettings;
        Settings.PRINT_SETTINGS = originalPrintSettings;
        Settings.EASTER_EGG = originalEasterEgg;
        Settings.CUSTOM_RECORDINGS_DIRECTORY = originalCustomDirectory;
        System.setProperty("os.name", originalOsName);
        Constants.IS_WINDOWS = originalWindows;
        Constants.IS_LINUX = originalLinux;
    }

    @Test
    void initializesAndShutsDownInjectedAudioComponents() throws Exception {
        configureStandardMode();
        FakeTargetDataLine recorderLine = new FakeTargetDataLine(Settings.AUDIO_FORMAT, new byte[0]);
        FakeTargetDataLine listenerLine = new FakeTargetDataLine(Settings.AUDIO_FORMAT, new byte[0]);
        Path marker = tempDir.resolve("marker.wav");
        AudioResourcesPreloader preloader = new AudioResourcesPreloader() {
            @Override
            public File copyMarkerToTemp() throws IOException {
                return Files.createFile(marker).toFile();
            }
        };
        Processor processor = new Processor(
                () -> createRecorder(recorderLine),
                recorder -> new Listener(recorder, inputProvider(listenerLine)),
                AudioMarker::new,
                () -> preloader);

        processor.init();
        processor.shutdown();
        Runtime.getRuntime().removeShutdownHook(processor.shutdownHook);

        assertTrue(Files.exists(marker));
        assertFalse(recorderLine.isOpen());
        assertFalse(listenerLine.isOpen());
        Files.deleteIfExists(marker);
    }

    @Test
    void enablesSpyModeAndAppliesRecordingAndPrintSettings() throws Exception {
        configureStandardMode();
        Settings.SPY_MODE = true;
        Settings.MARKER_MODE = true;
        Settings.SAVE_RECORDINGS_TO_CUSTOM_DIR = true;
        Settings.CUSTOM_RECORDINGS_DIRECTORY = tempDir.toString();
        Settings.PRINT_SETTINGS = true;
        Processor processor = newProcessor(AudioMarker::new);

        invokeApplySettings(processor);

        assertTrue(Settings.KEEP_RECORDINGS);
        assertFalse(Settings.MARKER_MODE);
        assertTrue(Files.isDirectory(tempDir));
    }

    @Test
    void delegatesMarkerModeWhenItIsEnabledOutsideSpyMode() throws Exception {
        configureStandardMode();
        Settings.MARKER_MODE = true;
        MarkerStub marker = new MarkerStub();
        Processor processor = newProcessor(() -> marker);

        invokeApplySettings(processor);

        assertTrue(marker.wasRun);
    }

    @Test
    void rethrowsRecorderInitializationFailures() {
        Processor processor = new Processor(
                () -> {
                    throw new IllegalStateException("audio unavailable");
                },
                recorder -> new Listener(recorder, inputProvider(
                        new FakeTargetDataLine(Settings.AUDIO_FORMAT, new byte[0]))),
                AudioMarker::new,
                AudioResourcesPreloader::new);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> invokeInitialize(processor));

        assertTrue(exception.getMessage().contains("audio unavailable"));
    }

    @Test
    void logsShutdownMessageWithAndWithoutEasterEgg() {
        Processor processor = new Processor();

        Settings.EASTER_EGG = false;
        processor.logShutdownMessage();
        Settings.EASTER_EGG = true;
        processor.logShutdownMessage();
    }

    @Test
    void shutsDownSafelyBeforeComponentsAreInitialized() {
        new Processor().shutdown();
    }

    @Test
    void appliesSpySettingsWithoutMarkerMode() throws Exception {
        configureStandardMode();
        Settings.SPY_MODE = true;
        invokeApplySettings(newProcessor(AudioMarker::new));

        assertTrue(Settings.KEEP_RECORDINGS);
        assertFalse(Settings.MARKER_MODE);
    }

    @Test
    void wrapsDefaultRecorderLineUnavailableFailure() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> Processor.createAudioRecorder(new AudioInputProvider() {
                    @Override public boolean isLineSupported(DataLine.Info lineInfo) { return true; }
                    @Override public TargetDataLine getLine(DataLine.Info lineInfo)
                            throws LineUnavailableException {
                        throw new LineUnavailableException("no input device");
                    }
                }));

        assertTrue(exception.getCause() instanceof LineUnavailableException);
    }

    @Test
    void wrapsMarkerResourceFailureDuringInitializationAndShutsDownListener() throws Exception {
        configureStandardMode();
        AudioRecorder recorder = createRecorder(
                new FakeTargetDataLine(Settings.AUDIO_FORMAT, new byte[0]));
        Processor processor = new Processor(
                () -> recorder,
                audioRecorder -> new Listener(audioRecorder, inputProvider(
                        new FakeTargetDataLine(Settings.AUDIO_FORMAT, new byte[0]))) {
                    @Override public void run() { }
                },
                AudioMarker::new,
                () -> new AudioResourcesPreloader() {
                    @Override
                    public File copyMarkerToTemp() throws IOException {
                        throw new IOException("missing marker");
                    }
                });

        RuntimeException exception = assertThrows(RuntimeException.class, processor::init);
        assertTrue(exception.getCause() instanceof IOException);
        assertNotNull(processor.shutdownHook);
        Runtime.getRuntime().removeShutdownHook(processor.shutdownHook);
        processor.shutdown();
    }

    @Test
    void appliesDefaultRecordingDirectoryAndOpensNoOsDialogWhenUnsupported() throws Exception {
        configureStandardMode();
        Settings.KEEP_RECORDINGS = true;
        Settings.OPEN_OS_RECORDING_SETTINGS = true;
        Constants.IS_WINDOWS = false;
        Constants.IS_LINUX = false;
        Path recordingDirectory = Path.of(Constants.OUTPUT_FOLDER_PATH);
        boolean directoryExisted = Files.exists(recordingDirectory);

        try {
            invokeApplySettings(newProcessor(AudioMarker::new));
            assertTrue(Files.isDirectory(recordingDirectory));
        } finally {
            if (!directoryExisted) {
                Files.deleteIfExists(recordingDirectory);
            }
        }
    }

    @Test
    void appliesDesktopRecordingDirectory() throws Exception {
        configureStandardMode();
        Settings.KEEP_RECORDINGS = true;
        Settings.SAVE_RECORDINGS_TO_DESKTOP = true;
        Settings.OUTPUT_DESKTOP_FOLDER_PATH = tempDir + File.separator;
        invokeApplySettings(newProcessor(AudioMarker::new));

        assertTrue(Files.isDirectory(tempDir));
    }

    private void configureStandardMode() {
        Settings.SPY_MODE = false;
        Settings.MARKER_MODE = false;
        Settings.KEEP_RECORDINGS = false;
        Settings.SAVE_RECORDINGS_TO_DESKTOP = false;
        Settings.SAVE_RECORDINGS_TO_CUSTOM_DIR = false;
        Settings.OPEN_OS_RECORDING_SETTINGS = false;
        Settings.PRINT_SETTINGS = false;
        Settings.EASTER_EGG = false;
    }

    private Processor newProcessor(java.util.function.Supplier<AudioMarker> markerFactory) {
        return new Processor(
                () -> createRecorder(
                        new FakeTargetDataLine(Settings.AUDIO_FORMAT, new byte[0])),
                recorder -> new Listener(recorder, inputProvider(
                        new FakeTargetDataLine(Settings.AUDIO_FORMAT, new byte[0]))),
                markerFactory,
                AudioResourcesPreloader::new);
    }

    private AudioInputProvider inputProvider(TargetDataLine line) {
        return new AudioInputProvider() {
            @Override public boolean isLineSupported(DataLine.Info lineInfo) { return true; }
            @Override public TargetDataLine getLine(DataLine.Info lineInfo) { return line; }
        };
    }

    private AudioRecorder createRecorder(TargetDataLine line) {
        try {
            return new AudioRecorder(inputProvider(line));
        } catch (LineUnavailableException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void invokeApplySettings(Processor processor) throws Exception {
        var method = Processor.class.getDeclaredMethod("applySettings");
        method.setAccessible(true);
        method.invoke(processor);
    }

    private void invokeInitialize(Processor processor) {
        try {
            var method = Processor.class.getDeclaredMethod("initializeComponents");
            method.setAccessible(true);
            method.invoke(processor);
        } catch (java.lang.reflect.InvocationTargetException exception) {
            if (exception.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new AssertionError(exception.getCause());
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static final class MarkerStub extends AudioMarker {
        private boolean wasRun;

        @Override
        public void runMarkerMode(Processor processor) {
            wasRun = true;
        }
    }
}
