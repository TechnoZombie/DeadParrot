package tz.deadparrot;

import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.slf4j.LoggerFactory;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatDarculaLaf;
import tz.deadparrot.utils.*;

@Slf4j
public class DeadParrotGUI extends JFrame {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private boolean updatingUI = false;

    // GUI components
    private JTextArea consoleOutput;
    private JButton startButton;
    private JButton stopButton;
    private JButton saveToDesktopButton;
    private JButton chooseDirectoryButton;
    private JButton openDirectoryButton;

    private Color stopButtonColor = new Color(155, 0, 20);

    private JLabel dirOutput;

    private JCheckBox spyModeEnabled;
    private JCheckBox markerModeEnabled;
    private JCheckBox keepRecordingsEnabled;
    private JCheckBox printSettingsEnabled;

    private JCheckBox openOSSettingsEnabled;
    private JCheckBox easterEggEnabled;
    private JCheckBox darkModeEnabled;

    private JLabel statusLabel;
    private JLabel recordingStatusLabel;
    private JLabel listenerStatusLabel;

    // Runtime state
    private boolean isRunning = false;
    private Processor processor;
    private Thread processorThread;
    private GuiLogAppender guiLogAppender;

    public DeadParrotGUI() {
        SettingsPersistence.loadSettings();
        SystemUtils.detectOSandSetPaths(false);
        setupLookAndFeel();
        initializeGUI();
        setupLogAppender();
        loadCurrentSettings();
    }

    private void setupLookAndFeel() {
        try {
            if (Settings.DARK_MODE) {
                UIManager.setLookAndFeel(new FlatDarculaLaf());
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
            }
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                     UnsupportedLookAndFeelException ex) {
                log.error("Failed to set look and feel", ex);
            }
        }
    }

    private void initializeGUI() {
        setTitle("DeadParrot Ham Radio Repeater v2.0");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        // setSize(1200, 650);

        // Sets the window size to a percentage of the screen resolution. For example, 75% = 0.75
        Dimension size = SystemUtils.getScaledScreenSize(0.75);
        setSize(size);

        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setIconImage(new ImageIcon(getClass().getResource("/img/DeadParrotSmall.png")).getImage());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleExit();
            }
        });

        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(createControlPanel(), BorderLayout.NORTH);
        northPanel.add(createDirPanel(), BorderLayout.CENTER);
        northPanel.add(createAudioControlsPanel(), BorderLayout.SOUTH);

        add(northPanel, BorderLayout.NORTH);
        add(createConsolePanel(), BorderLayout.CENTER);
        add(createStatusPanel(), BorderLayout.SOUTH);
    }

    private void setupLogAppender() {
        guiLogAppender = new GuiLogAppender();
        guiLogAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        guiLogAppender.start();

        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(guiLogAppender);
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder(GUILabels.CONTROL_PANEL_TITLE));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
       // startButton = new JButton("▶ START");
        startButton = new JButton(GUILabels.START_BUTTON);
        stopButton = new JButton(GUILabels.STOP_BUTTON);
       // stopButton.setEnabled(true);
        stopButton.setForeground(stopButtonColor);

        startButton.addActionListener(e -> {
            if (!isRunning) startRepeater();
        });

        stopButton.addActionListener(e -> {
            if (isRunning) stopRepeater();
        });

        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);

        panel.add(buttonPanel, BorderLayout.NORTH);
        panel.add(createOptionsPanel(), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createOptionsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder(GUILabels.OPTIONS_PANEL_TITLE));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        spyModeEnabled = new JCheckBox(GUILabels.SPY_MODE_CHECKBOX);
        markerModeEnabled = new JCheckBox(GUILabels.MARKER_MODE_CHECKBOX);
        keepRecordingsEnabled = new JCheckBox(GUILabels.KEEP_RECORDINGS_CHECKBOX);
        printSettingsEnabled = new JCheckBox(GUILabels.PRINT_SETTINGS_CHECKBOX);
        openOSSettingsEnabled = new JCheckBox(GUILabels.OPEN_OS_RECORDING_SETTINGS_CHECKBOX);
        easterEggEnabled = new JCheckBox(GUILabels.EASTER_EGG_CHECKBOX);
        darkModeEnabled = new JCheckBox(GUILabels.DARK_MODE_CHECKBOX);

        spyModeEnabled.setToolTipText(GUILabels.SPY_MODE_CHECKBOX_TOOLTIP);
        markerModeEnabled.setToolTipText(GUILabels.MARKER_MODE_CHECKBOX_TOOLTIP);
        keepRecordingsEnabled.setToolTipText(GUILabels.KEEP_RECORDINGS_CHECKBOX_TOOLTIP);
        printSettingsEnabled.setToolTipText(GUILabels.PRINT_SETTINGS_CHECKBOX_TOOLTIP);

        openOSSettingsEnabled.setToolTipText(GUILabels.OPEN_OS_RECORDING_SETTINGS_CHECKBOX_TOOLTIP);
        easterEggEnabled.setToolTipText(GUILabels.EASTER_EGG_CHECKBOX_TOOLTIP);
        darkModeEnabled.setToolTipText(GUILabels.DARK_MODE_CHECKBOX_TOOLTIP);

        spyModeEnabled.addActionListener(e -> {
            if (updatingUI) {
                return;
            }
            updateSettings();

            if (Settings.SPY_MODE) {
                logToConsole(Constants.SPY_MODE_ENABLED);
            } else {
                logToConsole(Constants.RUNNING_IN_STANDARD_MODE);
            }
        });

        markerModeEnabled.addActionListener(e -> {
            if (updatingUI) {
                return;
            }
            updateSettings();

            logToConsole("Marker Mode: " + Settings.MARKER_MODE);
        });

        keepRecordingsEnabled.addActionListener(e -> {
            if (updatingUI) {
                return;
            }
            updateSettings();

            logToConsole("Keep Recordings: " + Settings.KEEP_RECORDINGS);
        });

        printSettingsEnabled.addActionListener(e -> {
            if (updatingUI) {
                return;
            }
            updateSettings();

            logToConsole("Print Settings on Start: " + Settings.PRINT_SETTINGS);
        });

        openOSSettingsEnabled.addActionListener(e -> {
            if (updatingUI) {
                return;
            }
            updateSettings();

            logToConsole("Open OS Settings: " + Settings.OPEN_OS_RECORDING_SETTINGS);
        });

        easterEggEnabled.addActionListener(e -> {
            if (updatingUI) {
                return;
            }
            updateSettings();

            logToConsole("Easter Egg: " + Settings.EASTER_EGG);
        });

        darkModeEnabled.addActionListener(e -> {
            if (updatingUI) {
                return;
            }
            updateSettings();

            setLookAndFeel(Settings.DARK_MODE);
            updateConsoleColors(Settings.DARK_MODE);

            logToConsole("Dark Mode: " + Settings.DARK_MODE);
        });


        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(spyModeEnabled, gbc);
        gbc.gridx = 1;
        panel.add(markerModeEnabled, gbc);
        gbc.gridx = 2;
        panel.add(keepRecordingsEnabled, gbc);
        gbc.gridx = 3;
        panel.add(printSettingsEnabled, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(darkModeEnabled, gbc);
        gbc.gridx = 1;
        panel.add(openOSSettingsEnabled, gbc);
        gbc.gridx = 2;
        panel.add(easterEggEnabled, gbc);


        return panel;
    }

    private JPanel createDirPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder(GUILabels.DIRECTORY_PANEL_TITLE));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        saveToDesktopButton = new JButton(GUILabels.SAVE_TO_DESKTOP_BUTTON);
        saveToDesktopButton.setToolTipText(GUILabels.SAVE_TO_DESKTOP_BUTTON_TOOLTIP);

        saveToDesktopButton.addActionListener(e -> {

            Settings.SAVE_RECORDINGS_TO_DESKTOP = true;
            Settings.SAVE_RECORDINGS_TO_CUSTOM_DIR = false;

            FileUtils.verifyAndCreateOutputFolder(Settings.OUTPUT_DESKTOP_FOLDER_PATH);

            refreshSettingsUIState();

            logToConsole("Saving recordings to desktop folder");
        });

        chooseDirectoryButton = new JButton(GUILabels.CHOOSE_DIRECTORY_BUTTON);

        chooseDirectoryButton.setToolTipText(GUILabels.CHOOSE_DIRECTORY_BUTTON_TOOLTIP);

        chooseDirectoryButton.addActionListener(e -> {

            JFileChooser chooser = new JFileChooser();

            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            int returnVal = chooser.showOpenDialog(this);

            if (returnVal == JFileChooser.APPROVE_OPTION) {

                String selectedDirectory = chooser.getSelectedFile().getAbsolutePath() + Constants.CUSTOM_RECORDINGS_DIRECTORY_PREFIX;

                FileUtils.verifyAndCreateOutputFolder(selectedDirectory);

                Settings.CUSTOM_RECORDINGS_DIRECTORY = selectedDirectory;

                Settings.SAVE_RECORDINGS_TO_DESKTOP = false;
                Settings.SAVE_RECORDINGS_TO_CUSTOM_DIR = true;

                refreshSettingsUIState();

                logToConsole("Saving recordings to: " + selectedDirectory);
            }
        });

        openDirectoryButton = new JButton(GUILabels.OPEN_DIRECTORY_BUTTON);

        openDirectoryButton.setToolTipText(GUILabels.OPEN_DIRECTORY_BUTTON_TOOLTIP);

        refreshSettingsUIState();

        openDirectoryButton.addActionListener(e -> FileUtils.openDestinationFolder());

        dirOutput = new JLabel(GUILabels.DIR_NOT_SET);
        dirOutput.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(saveToDesktopButton, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        panel.add(chooseDirectoryButton, gbc);

        gbc.gridx = 2;
        gbc.gridy = 0;
        panel.add(openDirectoryButton, gbc);

        gbc.gridx = 3;
        gbc.gridy = 0;
        panel.add(dirOutput, gbc);

        return panel;
    }

    private JPanel createAudioControlsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        panel.setBorder(new TitledBorder(GUILabels.AUDIO_CONTROLS_PANEL_TITLE));

        JButton runAudioProbeButton = new JButton(GUILabels.AUDIO_PROBE_BUTTON);
        JButton openPlaybackSettingsButton = new JButton(GUILabels.OPEN_PLAYBACK_SETTINGS_BUTTON);
        JButton openRecordingSettingsButton = new JButton(GUILabels.OPEN_RECORDING_SETTINGS_BUTTON);

        runAudioProbeButton.setToolTipText(GUILabels.AUDIO_PROBE_BUTTON_TOOLTIP);
        openPlaybackSettingsButton.setToolTipText(GUILabels.OPEN_PLAYBACK_SETTINGS_BUTTON_TOOLTIP);
        openRecordingSettingsButton.setToolTipText(GUILabels.OPEN_RECORDING_SETTINGS_BUTTON_TOOLTIP);

        runAudioProbeButton.addActionListener(e -> {

            if (isRunning) {
                log.info(GUILabels.STOP_BEFORE_RUNNING);
                return;
            }

            runAudioProbeButton.setEnabled(false);

            log.info(GUILabels.AUDIO_PROBE_RUNNING);

            new Thread(() -> {
                try {
                    AudioFormatProbe.probeAudioFormats();
                    log.info(GUILabels.AUDIO_PROBE_COMPLETED);

                } finally {
                    SwingUtilities.invokeLater(() -> runAudioProbeButton.setEnabled(true));
                }
            }, "AudioProbeThread").start();
        });

        openPlaybackSettingsButton.addActionListener(e -> SoundSettingsOpener.openPlaybackSettings());

        openRecordingSettingsButton.addActionListener(e -> SoundSettingsOpener.openRecordingSettings());

        panel.add(runAudioProbeButton);
        panel.add(openPlaybackSettingsButton);
        panel.add(openRecordingSettingsButton);

        return panel;
    }

    private JPanel createConsolePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder(GUILabels.CONSOLE_PANEL_TITLE));

        consoleOutput = new JTextArea();
        consoleOutput.setEditable(false);
        consoleOutput.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        consoleOutput.setLineWrap(true);
        consoleOutput.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(consoleOutput);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setPreferredSize(new Dimension(850, 350));

        panel.add(scrollPane, BorderLayout.CENTER);

        JButton clearConsoleButton = new JButton(GUILabels.CLEAR_CONSOLE_BUTTON);
        clearConsoleButton.addActionListener(e -> consoleOutput.setText(""));

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(clearConsoleButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createLoweredBevelBorder());

        statusLabel = new JLabel(GUILabels.STATUS_STOPPED);
        recordingStatusLabel = new JLabel(GUILabels.RECORDING_IDLE);
        listenerStatusLabel = new JLabel(GUILabels.LISTENER_IDLE);

        panel.add(statusLabel);
        panel.add(new JLabel(" | "));
        panel.add(recordingStatusLabel);
        panel.add(new JLabel(" | "));
        panel.add(listenerStatusLabel);

        return panel;
    }

    private void logToConsole(String message) {
        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        SwingUtilities.invokeLater(() -> {
            consoleOutput.append("[" + timestamp + "] " + message + "\n");
            consoleOutput.setCaretPosition(consoleOutput.getDocument().getLength());
        });
    }

    private void startRepeater() {
        updateSettings();

        try {
            processor = new Processor();
            processorThread = new Thread(() -> {
                try {
                    processor.init();
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        logToConsole(GUILabels.ERROR_STARTING_REPEATER + ex.getMessage());
                        log.error(GUILabels.ERROR_STARTING_REPEATER, ex);
                        stopRepeater(); // this sets isRunning = false and resets the UI
                    });
                }
            });

            isRunning = true;
            //startButton.setEnabled(false);
            startButton.setForeground(Color.GREEN);
            //stopButton.setEnabled(true);
            stopButton.setForeground(null);
            statusLabel.setText(GUILabels.STATUS_RUNNING);
            recordingStatusLabel.setText(GUILabels.RECORDING_ACTIVE);
            listenerStatusLabel.setText(GUILabels.LISTENER_ACTIVE);

            processorThread.start();

        } catch (Exception ex) {
            isRunning = false;
            logToConsole(GUILabels.FAILED_TO_START_REPEATER + ex.getMessage());
            log.error(GUILabels.FAILED_TO_START_REPEATER, ex);
        }
    }

    private void stopRepeater() {
        try {
            if (processor != null) {
                processor.shutdown();
            }

            if (processorThread != null && processorThread.isAlive()) {
                processorThread.interrupt();
            }

            isRunning = false;
           // startButton.setEnabled(true);
            startButton.setForeground(null);
            //stopButton.setEnabled(false);
            stopButton.setForeground(stopButtonColor);
            statusLabel.setText("Status: Stopped");
            recordingStatusLabel.setText("Audio Recorder: Idle");
            listenerStatusLabel.setText("Listener: Idle");

        } catch (Exception ex) {
            logToConsole("Error stopping repeater: " + ex.getMessage());
            log.error("Error stopping repeater", ex);
        }
    }

    private void handleExit() {
        if (isRunning) {
            int result = JOptionPane.showConfirmDialog(this, "Repeater is still running. Stop it before exiting?", "Confirm Exit", JOptionPane.YES_NO_CANCEL_OPTION);

            if (result == JOptionPane.YES_OPTION) {
                stopRepeater();
                cleanup();
                System.exit(0);
            } else if (result == JOptionPane.NO_OPTION) {
                cleanup();
                System.exit(0);
            }
        } else {
            cleanup();
            System.exit(0);
        }
    }

    private void loadCurrentSettings() {

        spyModeEnabled.setSelected(Settings.SPY_MODE);
        markerModeEnabled.setSelected(Settings.MARKER_MODE);
        keepRecordingsEnabled.setSelected(Settings.KEEP_RECORDINGS);
        printSettingsEnabled.setSelected(Settings.PRINT_SETTINGS);
        openOSSettingsEnabled.setSelected(Settings.OPEN_OS_RECORDING_SETTINGS);

        easterEggEnabled.setSelected(Settings.EASTER_EGG);
        darkModeEnabled.setSelected(Settings.DARK_MODE);

        refreshSettingsUIState();

        setLookAndFeel(Settings.DARK_MODE);
        updateConsoleColors(Settings.DARK_MODE);

        logToConsole("Settings loaded from configuration");
    }

    private void updateSettings() {

        Settings.SPY_MODE = spyModeEnabled.isSelected();

        if (Settings.SPY_MODE) {
            Settings.MARKER_MODE = false;
            Settings.KEEP_RECORDINGS = true;
        } /*else {
            Settings.MARKER_MODE = markerModeEnabled.isSelected();
            Settings.KEEP_RECORDINGS = keepRecordingsEnabled.isSelected();
        }*/

        Settings.MARKER_MODE = markerModeEnabled.isSelected();
        Settings.KEEP_RECORDINGS = keepRecordingsEnabled.isSelected();

        Settings.OPEN_OS_RECORDING_SETTINGS = openOSSettingsEnabled.isSelected();

        Settings.EASTER_EGG = easterEggEnabled.isSelected();

        Settings.DARK_MODE = darkModeEnabled.isSelected();

        Settings.PRINT_SETTINGS = printSettingsEnabled.isSelected();

        SettingsPersistence.saveSettings();
    }

    private void cleanup() {
        if (guiLogAppender != null) {
            Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            rootLogger.detachAppender(guiLogAppender);
            guiLogAppender.stop();
        }
    }

    public void logMessage(String message) {
        logToConsole(message);
    }

    private class GuiLogAppender extends AppenderBase<ILoggingEvent> {
        @Override
        protected void append(ILoggingEvent event) {
            String logLevel = event.getLevel().toString();
            String loggerName = event.getLoggerName();
            String message = event.getFormattedMessage();

            String simpleName = loggerName.substring(loggerName.lastIndexOf('.') + 1);
            String logEntry = String.format("[%s] %s - %s", logLevel, simpleName, message);
            String timestamp = LocalDateTime.now().format(TIME_FORMATTER);

            SwingUtilities.invokeLater(() -> {
                consoleOutput.append("[" + timestamp + "] " + logEntry + "\n");
                consoleOutput.setCaretPosition(consoleOutput.getDocument().getLength());
            });
        }
    }


    private void setLookAndFeel(boolean dark) {
        try {
            if (dark) {
                UIManager.setLookAndFeel(new FlatDarculaLaf());
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
            }
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception e) {
            log.error("Failed to set look and feel", e);
        }
    }

    private void updateConsoleColors(boolean dark) {
        if (dark) {
            consoleOutput.setBackground(Color.BLACK);
            consoleOutput.setForeground(Color.GREEN);
        } else {
            consoleOutput.setBackground(Color.WHITE);
            consoleOutput.setForeground(Color.BLACK);
        }
    }

    private void refreshSettingsUIState() {

        updatingUI = true;

        try {

            boolean spyMode = Settings.SPY_MODE;

            keepRecordingsEnabled.setEnabled(!spyMode);
            markerModeEnabled.setEnabled(!spyMode);

            if (spyMode) {
                keepRecordingsEnabled.setSelected(true);
                markerModeEnabled.setSelected(false);
            }

            saveToDesktopButton.setEnabled(!Settings.SAVE_RECORDINGS_TO_DESKTOP);

            chooseDirectoryButton.setEnabled(true);

            boolean hasValidOutputDirectory = (Settings.SAVE_RECORDINGS_TO_DESKTOP && Settings.OUTPUT_DESKTOP_FOLDER_PATH != null) || (Settings.SAVE_RECORDINGS_TO_CUSTOM_DIR && Settings.CUSTOM_RECORDINGS_DIRECTORY != null);

            openDirectoryButton.setEnabled(hasValidOutputDirectory);

            if (hasValidOutputDirectory && dirOutput != null) {

                if (Settings.SAVE_RECORDINGS_TO_DESKTOP) {

                    dirOutput.setText("Dir: Desktop");

                } else if (Settings.SAVE_RECORDINGS_TO_CUSTOM_DIR) {

                    dirOutput.setText("Dir: " + SystemUtils.shortenPath(Settings.CUSTOM_RECORDINGS_DIRECTORY, 40));
                }
            }

        } finally {
            updatingUI = false;
        }
    }
}
