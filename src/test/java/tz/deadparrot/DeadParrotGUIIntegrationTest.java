package tz.deadparrot;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeadParrotGUIIntegrationTest {

    @TempDir
    Path tempDir;

    private final Path settingsFile = Path.of("deadparrot.settings");
    private byte[] originalSettingsFile;
    private boolean settingsFileExisted;
    private DeadParrotGUI gui;
    private final DeadParrotGUI.LookAndFeelInstaller originalInstaller =
            DeadParrotGUI.lookAndFeelInstaller;
    private final boolean originalSpyMode = Settings.SPY_MODE;
    private final boolean originalMarkerMode = Settings.MARKER_MODE;
    private final boolean originalKeepRecordings = Settings.KEEP_RECORDINGS;
    private final boolean originalPrintSettings = Settings.PRINT_SETTINGS;
    private final boolean originalDarkMode = Settings.DARK_MODE;
    private final boolean originalOpenSettings = Settings.OPEN_OS_RECORDING_SETTINGS;
    private final boolean originalSaveDesktop = Settings.SAVE_RECORDINGS_TO_DESKTOP;
    private final boolean originalSaveCustom = Settings.SAVE_RECORDINGS_TO_CUSTOM_DIR;
    private final boolean originalEasterEgg = Settings.EASTER_EGG;
    private final String originalDesktopPath = Settings.OUTPUT_DESKTOP_FOLDER_PATH;
    private final String originalCustomPath = Settings.CUSTOM_RECORDINGS_DIRECTORY;

    @BeforeEach
    void prepareSettingsFile() throws Exception {
        settingsFileExisted = Files.exists(settingsFile);
        if (settingsFileExisted) {
            originalSettingsFile = Files.readAllBytes(settingsFile);
        }
        Properties properties = new Properties();
        properties.setProperty("spyMode", "false");
        properties.setProperty("markerMode", "false");
        properties.setProperty("keepRecordings", "false");
        properties.setProperty("printSettings", "false");
        properties.setProperty("darkMode", "false");
        properties.setProperty("openOSRecordingSettings", "false");
        properties.setProperty("saveToDesktop", "false");
        properties.setProperty("saveToCustomDir", "false");
        properties.setProperty("easterEgg", "false");
        try (var output = Files.newOutputStream(settingsFile)) {
            properties.store(output, "GUI integration test");
        }
    }

    @AfterEach
    void cleanup() throws Exception {
        if (gui != null) {
            onEdt(() -> {
                invoke(gui, "cleanup");
                gui.dispose();
                return null;
            });
        }
        if (settingsFileExisted) {
            Files.write(settingsFile, originalSettingsFile);
        } else {
            Files.deleteIfExists(settingsFile);
        }
        DeadParrotGUI.lookAndFeelInstaller = originalInstaller;
        Settings.SPY_MODE = originalSpyMode;
        Settings.MARKER_MODE = originalMarkerMode;
        Settings.KEEP_RECORDINGS = originalKeepRecordings;
        Settings.PRINT_SETTINGS = originalPrintSettings;
        Settings.DARK_MODE = originalDarkMode;
        Settings.OPEN_OS_RECORDING_SETTINGS = originalOpenSettings;
        Settings.SAVE_RECORDINGS_TO_DESKTOP = originalSaveDesktop;
        Settings.SAVE_RECORDINGS_TO_CUSTOM_DIR = originalSaveCustom;
        Settings.EASTER_EGG = originalEasterEgg;
        Settings.OUTPUT_DESKTOP_FOLDER_PATH = originalDesktopPath;
        Settings.CUSTOM_RECORDINGS_DIRECTORY = originalCustomPath;
    }

    @Test
    void constructsGuiExercisesSettingsDirectoriesAudioControlsAndConsole() throws Exception {
        gui = onEdt(DeadParrotGUI::new);
        AtomicInteger directoryOpens = new AtomicInteger();
        AtomicInteger playbackSettings = new AtomicInteger();
        AtomicInteger recordingSettings = new AtomicInteger();
        CountDownLatch probeFinished = new CountDownLatch(1);
        AtomicInteger probeCount = new AtomicInteger();
        AtomicInteger chooserCalls = new AtomicInteger();
        gui.directoryChooserFactory = () -> new JFileChooser() {
            @Override
            public int showOpenDialog(java.awt.Component parent) {
                return chooserCalls.getAndIncrement() == 0
                        ? JFileChooser.APPROVE_OPTION : JFileChooser.CANCEL_OPTION;
            }

            @Override
            public File getSelectedFile() {
                return tempDir.toFile();
            }
        };
        gui.openDirectory = directoryOpens::incrementAndGet;
        gui.openPlaybackSettings = playbackSettings::incrementAndGet;
        gui.openRecordingSettings = recordingSettings::incrementAndGet;
        gui.audioProbe = () -> {
            probeCount.incrementAndGet();
            probeFinished.countDown();
        };

        onEdt(() -> {
            checkBox("spyModeEnabled").doClick();
            checkBox("spyModeEnabled").doClick();
            checkBox("markerModeEnabled").doClick();
            checkBox("markerModeEnabled").doClick();
            checkBox("keepRecordingsEnabled").doClick();
            checkBox("keepRecordingsEnabled").doClick();
            checkBox("printSettingsEnabled").doClick();
            checkBox("openOSSettingsEnabled").doClick();
            checkBox("easterEggEnabled").doClick();
            checkBox("darkModeEnabled").doClick();
            checkBox("darkModeEnabled").doClick();
            button(GUILabels.SAVE_TO_DESKTOP_BUTTON).doClick();
            button(GUILabels.CHOOSE_DIRECTORY_BUTTON).doClick();
            String selectedDirectory = Settings.CUSTOM_RECORDINGS_DIRECTORY;
            button(GUILabels.CHOOSE_DIRECTORY_BUTTON).doClick();
            assertEquals(selectedDirectory, Settings.CUSTOM_RECORDINGS_DIRECTORY);
            button(GUILabels.OPEN_DIRECTORY_BUTTON).doClick();
            button(GUILabels.OPEN_PLAYBACK_SETTINGS_BUTTON).doClick();
            button(GUILabels.OPEN_RECORDING_SETTINGS_BUTTON).doClick();
            button(GUILabels.AUDIO_PROBE_BUTTON).doClick();
            gui.logMessage("GUI test message");
            return null;
        });

        assertTrue(probeFinished.await(2, TimeUnit.SECONDS));
        onEdt(() -> null);

        assertTrue(directoryOpens.get() == 1);
        assertTrue(playbackSettings.get() == 1);
        assertTrue(recordingSettings.get() == 1);
        assertEquals(1, probeCount.get());
        assertTrue(Files.isDirectory(tempDir.resolve("Recordings")));
        assertTrue(((JTextArea) field(gui, "consoleOutput", false)).getText()
                .contains("GUI test message"));

        onEdt(() -> {
            button(GUILabels.CLEAR_CONSOLE_BUTTON).doClick();
            return null;
        });
        assertEquals("", ((JTextArea) field(gui, "consoleOutput", false)).getText());

        onEdt(() -> {
            setField(gui, "updatingUI", true);
            checkBox("spyModeEnabled").doClick();
            checkBox("markerModeEnabled").doClick();
            checkBox("keepRecordingsEnabled").doClick();
            checkBox("printSettingsEnabled").doClick();
            checkBox("openOSSettingsEnabled").doClick();
            checkBox("easterEggEnabled").doClick();
            checkBox("darkModeEnabled").doClick();
            setField(gui, "updatingUI", false);
            Settings.SPY_MODE = true;
            invoke(gui, "refreshSettingsUIState");
            return null;
        });
        assertFalse(checkBox("keepRecordingsEnabled").isEnabled());
        assertFalse(checkBox("markerModeEnabled").isEnabled());

        LoggingEvent event = new LoggingEvent();
        event.setLevel(Level.INFO);
        event.setLoggerName("tz.deadparrot.TestLogger");
        event.setMessage("Appender message");
        AppenderBase appender = (AppenderBase) field(gui, "guiLogAppender", false);
        appender.doAppend(event);
        onEdt(() -> null);
        assertTrue(((JTextArea) field(gui, "consoleOutput", false)).getText()
                .contains("TestLogger - Appender message"));

        onEdt(() -> {
            setField(gui, "isRunning", true);
            button(GUILabels.AUDIO_PROBE_BUTTON).doClick();
            setField(gui, "isRunning", false);
            return null;
        });
        assertEquals(1, probeCount.get());
    }

    @Test
    void startsAndStopsProcessorAndReportsStartupFailures() throws Exception {
        gui = onEdt(DeadParrotGUI::new);
        CountDownLatch initialized = new CountDownLatch(1);
        TestProcessor processor = new TestProcessor(initialized);
        gui.processorFactory = () -> processor;

        onEdt(() -> {
            button(GUILabels.START_BUTTON).doClick();
            return null;
        });
        assertTrue(initialized.await(2, TimeUnit.SECONDS));
        onEdt(() -> {
            button(GUILabels.STOP_BUTTON).doClick();
            return null;
        });
        assertEquals(1, processor.shutdowns.get());
        assertFalse((boolean) field(gui, "isRunning", false));

        gui.processorFactory = () -> {
            throw new IllegalStateException("startup failure");
        };
        onEdt(() -> {
            button(GUILabels.START_BUTTON).doClick();
            return null;
        });
        onEdt(() -> null);
        assertFalse((boolean) field(gui, "isRunning", false));
        assertTrue(((JTextArea) field(gui, "consoleOutput", false)).getText()
                .contains("startup failure"));

        gui.processorFactory = () -> new TestProcessor(() -> {
            throw new IllegalStateException("processor init failure");
        });
        onEdt(() -> {
            button(GUILabels.START_BUTTON).doClick();
            return null;
        });
        waitUntilStopped();
        onEdt(() -> null);
        assertTrue(((JTextArea) field(gui, "consoleOutput", false)).getText()
                .contains("processor init failure"));

        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch interruptObserved = new CountDownLatch(1);
        gui.processorFactory = () -> new TestProcessor(() -> {
            entered.countDown();
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException exception) {
                interruptObserved.countDown();
                Thread.currentThread().interrupt();
            }
        });
        onEdt(() -> {
            button(GUILabels.START_BUTTON).doClick();
            return null;
        });
        assertTrue(entered.await(2, TimeUnit.SECONDS));
        onEdt(() -> {
            button(GUILabels.STOP_BUTTON).doClick();
            return null;
        });
        assertTrue(interruptObserved.await(2, TimeUnit.SECONDS));

        gui.processorFactory = () -> new TestProcessor() {
            @Override
            void shutdown() {
                throw new IllegalStateException("shutdown failure");
            }
        };
        onEdt(() -> {
            button(GUILabels.START_BUTTON).doClick();
            button(GUILabels.STOP_BUTTON).doClick();
            return null;
        });
        onEdt(() -> null);
        assertTrue(((JTextArea) field(gui, "consoleOutput", false)).getText()
                .contains("shutdown failure"));
        onEdt(() -> {
            setField(gui, "processor", null);
            invoke(gui, "stopRepeater");
            return null;
        });
    }

    @Test
    void handlesExitConfirmationChoicesAndStoppedExit() throws Exception {
        gui = onEdt(DeadParrotGUI::new);
        AtomicInteger exits = new AtomicInteger();
        gui.processorFactory = TestProcessor::new;
        gui.exitAction = ignored -> exits.incrementAndGet();

        onEdt(() -> {
            invoke(gui, "handleExit");
            return null;
        });
        assertEquals(1, exits.get());

        gui = recreateGui();
        gui.processorFactory = TestProcessor::new;
        gui.exitAction = ignored -> exits.incrementAndGet();
        gui.exitConfirmation = () -> javax.swing.JOptionPane.NO_OPTION;
        startGuiRepeater();
        onEdt(() -> {
            invoke(gui, "handleExit");
            return null;
        });
        assertEquals(2, exits.get());

        gui = recreateGui();
        gui.processorFactory = TestProcessor::new;
        gui.exitAction = ignored -> exits.incrementAndGet();
        gui.exitConfirmation = () -> javax.swing.JOptionPane.CANCEL_OPTION;
        startGuiRepeater();
        onEdt(() -> {
            invoke(gui, "handleExit");
            return null;
        });
        assertEquals(2, exits.get());

        gui = recreateGui();
        gui.processorFactory = TestProcessor::new;
        gui.exitAction = ignored -> exits.incrementAndGet();
        gui.exitConfirmation = () -> javax.swing.JOptionPane.YES_OPTION;
        startGuiRepeater();
        onEdt(() -> {
            invoke(gui, "handleExit");
            return null;
        });
        assertEquals(3, exits.get());
        assertFalse((boolean) field(gui, "isRunning", false));

        onEdt(() -> {
            gui.getWindowListeners()[0].windowClosing(
                    new WindowEvent(gui, WindowEvent.WINDOW_CLOSING));
            return null;
        });
        assertEquals(4, exits.get());
    }

    @Test
    void fallsBackAndLogsWhenLookAndFeelInstallationFails() throws Exception {
        AtomicInteger installs = new AtomicInteger();
        DeadParrotGUI.lookAndFeelInstaller = new DeadParrotGUI.LookAndFeelInstaller() {
            @Override
            public void install(javax.swing.LookAndFeel lookAndFeel)
                    throws javax.swing.UnsupportedLookAndFeelException {
                installs.incrementAndGet();
                throw new javax.swing.UnsupportedLookAndFeelException("theme failure");
            }

            @Override
            public void install(String className) {
                installs.incrementAndGet();
            }
        };
        gui = onEdt(DeadParrotGUI::new);
        assertTrue(installs.get() >= 2);

        DeadParrotGUI.lookAndFeelInstaller = new DeadParrotGUI.LookAndFeelInstaller() {
            @Override public void install(javax.swing.LookAndFeel lookAndFeel)
                    throws javax.swing.UnsupportedLookAndFeelException {
                throw new javax.swing.UnsupportedLookAndFeelException("theme failure");
            }
            @Override public void install(String className)
                    throws javax.swing.UnsupportedLookAndFeelException {
                throw new javax.swing.UnsupportedLookAndFeelException("fallback failure");
            }
        };
        onEdt(() -> {
            invoke(gui, "setupLookAndFeel");
            invoke(gui, "setLookAndFeel", boolean.class, true);
            return null;
        });
    }

    private DeadParrotGUI recreateGui() throws Exception {
        onEdt(() -> {
            invoke(gui, "cleanup");
            gui.dispose();
            return null;
        });
        return onEdt(DeadParrotGUI::new);
    }

    private void startGuiRepeater() throws Exception {
        onEdt(() -> {
            button(GUILabels.START_BUTTON).doClick();
            return null;
        });
        onEdt(() -> null);
    }

    private JCheckBox checkBox(String name) throws ReflectiveOperationException {
        return (JCheckBox) field(gui, name, false);
    }

    private JButton button(String text) {
        String fieldName = switch (text) {
            case GUILabels.START_BUTTON -> "startButton";
            case GUILabels.STOP_BUTTON -> "stopButton";
            case GUILabels.SAVE_TO_DESKTOP_BUTTON -> "saveToDesktopButton";
            case GUILabels.CHOOSE_DIRECTORY_BUTTON -> "chooseDirectoryButton";
            case GUILabels.OPEN_DIRECTORY_BUTTON -> "openDirectoryButton";
            default -> null;
        };
        if (fieldName != null) {
            try {
                return (JButton) field(gui, fieldName, false);
            } catch (ReflectiveOperationException exception) {
                throw new AssertionError(exception);
            }
        }
        JButton found = findButton(gui.getContentPane(), text);
        if (found == null) throw new AssertionError("Button not found: " + text);
        return found;
    }

    private JButton findButton(Component component, String text) {
        if (component instanceof JButton button && button.getText().equals(text)) {
            return button;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                JButton found = findButton(child, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private Object field(Object target, String name, boolean ignored) throws ReflectiveOperationException {
        var field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private void invoke(Object target, String methodName) {
        try {
            var method = target.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(target);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private void invoke(Object target, String methodName, Class<?> parameterType, Object argument) {
        try {
            var method = target.getClass().getDeclaredMethod(methodName, parameterType);
            method.setAccessible(true);
            method.invoke(target, argument);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private void setField(Object target, String name, Object value) {
        try {
            var field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private void waitUntilStopped() throws Exception {
        long deadline = System.currentTimeMillis() + 2000;
        while ((boolean) field(gui, "isRunning", false)
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(5);
        }
        assertFalse((boolean) field(gui, "isRunning", false));
    }

    private <T> T onEdt(java.util.concurrent.Callable<T> action) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) return action.call();
        java.util.concurrent.atomic.AtomicReference<T> result = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<Throwable> failure = new java.util.concurrent.atomic.AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                result.set(action.call());
            } catch (Throwable throwable) {
                failure.set(throwable);
            }
        });
        if (failure.get() != null) throw new AssertionError(failure.get());
        return result.get();
    }

    private static class TestProcessor extends Processor {
        private final CountDownLatch initialized;
        private final AtomicInteger shutdowns = new AtomicInteger();
        private final Runnable initializer;

        private TestProcessor() {
            this(new CountDownLatch(0), () -> { });
        }

        private TestProcessor(CountDownLatch initialized) {
            this(initialized, () -> { });
        }

        private TestProcessor(Runnable initializer) {
            this(new CountDownLatch(0), initializer);
        }

        private TestProcessor(CountDownLatch initialized, Runnable initializer) {
            this.initialized = initialized;
            this.initializer = initializer;
        }

        @Override public void init() {
            initializer.run();
            initialized.countDown();
        }
        @Override void shutdown() { shutdowns.incrementAndGet(); }
    }
}
