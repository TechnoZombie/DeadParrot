package tz.deadparrot.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tz.deadparrot.Constants;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SoundSettingsOpenerTest {

    private final boolean originalWindows = Constants.IS_WINDOWS;
    private final boolean originalLinux = Constants.IS_LINUX;
    private final SoundSettingsOpener.ProcessLauncher originalLauncher =
            SoundSettingsOpener.processLauncher;
    private final List<List<String>> commands = new ArrayList<>();

    @AfterEach
    void restoreState() {
        Constants.IS_WINDOWS = originalWindows;
        Constants.IS_LINUX = originalLinux;
        SoundSettingsOpener.processLauncher = originalLauncher;
    }

    @Test
    void opensWindowsRecordingAndPlaybackSettings() {
        Constants.IS_WINDOWS = true;
        Constants.IS_LINUX = false;
        SoundSettingsOpener.processLauncher = this::recordCommand;

        SoundSettingsOpener.openRecordingSettings();
        SoundSettingsOpener.openPlaybackSettings();
        SoundSettingsOpener.openRecordingSettingsClassic();

        assertEquals(List.of(
                List.of("ms-settings:sound-devices"),
                List.of("control", "mmsys.cpl,,0"),
                List.of("rundll32.exe", "shell32.dll,Control_RunDLL", "mmsys.cpl,,1")
        ), commands);
    }

    @Test
    void fallsBackWhenWindowsRecordingSettingsCannotOpen() {
        Constants.IS_WINDOWS = true;
        Constants.IS_LINUX = false;
        SoundSettingsOpener.processLauncher = command -> {
            commands.add(command);
            if (command.getFirst().equals("ms-settings:sound-devices")) {
                throw new IOException("Windows settings unavailable");
            }
            return null;
        };

        SoundSettingsOpener.openRecordingSettings();

        assertEquals(List.of(
                List.of("ms-settings:sound-devices"),
                List.of("control", "mmsys.cpl,,1")
        ), commands);
    }

    @Test
    void logsProcessLaunchFailuresWithoutLaunchingAnythingElse() {
        Constants.IS_WINDOWS = true;
        Constants.IS_LINUX = false;
        SoundSettingsOpener.processLauncher = command -> {
            commands.add(command);
            throw new IOException("not available");
        };

        SoundSettingsOpener.openRecordingSettings();
        SoundSettingsOpener.openRecordingSettingsClassic();
        SoundSettingsOpener.openPlaybackSettings();

        assertEquals(4, commands.size());
    }

    @Test
    void triesLinuxRecordingSettingsInOrderAndDoesNothingForUnknownOs() {
        Constants.IS_WINDOWS = false;
        Constants.IS_LINUX = true;
        SoundSettingsOpener.processLauncher = command -> {
            commands.add(command);
            if (command.getFirst().equals("gnome-control-center")) {
                throw new IOException("GNOME unavailable");
            }
            return null;
        };

        SoundSettingsOpener.openRecordingSettings();
        SoundSettingsOpener.openPlaybackSettings();

        Constants.IS_LINUX = false;
        SoundSettingsOpener.openRecordingSettings();

        assertEquals(List.of(
                List.of("gnome-control-center", "sound"),
                List.of("pavucontrol")
        ), commands);
    }

    @Test
    void handlesBothLinuxRecordingLaunchesFailing() {
        Constants.IS_WINDOWS = false;
        Constants.IS_LINUX = true;
        SoundSettingsOpener.processLauncher = command -> {
            commands.add(command);
            throw new IOException("settings app unavailable");
        };

        SoundSettingsOpener.openRecordingSettings();

        assertEquals(2, commands.size());
    }

    @Test
    void opensLinuxRecordingSettingsWhenGnomeIsAvailable() {
        Constants.IS_WINDOWS = false;
        Constants.IS_LINUX = true;
        SoundSettingsOpener.processLauncher = this::recordCommand;

        SoundSettingsOpener.openRecordingSettings();
        assertEquals(List.of(List.of("gnome-control-center", "sound")), commands);
        assertEquals("SoundSettingsOpener", new SoundSettingsOpener().getClass().getSimpleName());
    }

    @Test
    void defaultLauncherCanStartAHarmlessShortLivedProcess() throws Exception {
        SoundSettingsOpener.ProcessLauncher launcher = SoundSettingsOpener.processLauncher;
        String javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java").toString();

        Process process = launcher.start(List.of(javaExecutable, "-version"));

        assertEquals(0, process.waitFor());
    }

    private Process recordCommand(List<String> command) {
        commands.add(command);
        return null;
    }
}
