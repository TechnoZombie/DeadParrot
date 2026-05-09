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
import tz.deadparrot.utils.AudioFormatProbe;

@Slf4j
public class DeadParrotGUI extends JFrame {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    // GUI components
    private JTextArea consoleOutput;
    private JButton startButton;
    private JButton stopButton;
    private JButton runAudioProbeButton;
    private JButton chooseDirectoryButton;

    private JCheckBox spyModeEnabled;
    private JCheckBox markerModeEnabled;
    private JCheckBox keepRecordingsEnabled;
    private JCheckBox saveToDesktopEnabled;
    private JCheckBox customDirectoryEnabled;
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
        setTitle("DeadParrot Ham Radio Repeater v1.0");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(900, 650);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setIconImage(new ImageIcon(getClass().getResource("/img/DeadParrotSmall.png")).getImage());
        
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleExit();
            }
        });

        add(createControlPanel(), BorderLayout.NORTH);
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
        panel.setBorder(new TitledBorder("Repeater Controls"));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        startButton = new JButton("▶ START");
        stopButton = new JButton("■ STOP");
        runAudioProbeButton = new JButton("Run Audio Probe");

        runAudioProbeButton.setToolTipText("Probe available audio formats and devices");

        stopButton.setEnabled(false);

        startButton.addActionListener(e -> {
            if (!isRunning) startRepeater();
        });

        stopButton.addActionListener(e -> {
            if (isRunning) stopRepeater();
        });

        runAudioProbeButton.addActionListener(e -> {
            if (!isRunning) {
                AudioFormatProbe.probeAudioFormats();
            } else {
                logToConsole("Stop the repeater before running the audio probe.");
            }
        });

        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(runAudioProbeButton);
        panel.add(buttonPanel, BorderLayout.NORTH);
        panel.add(createOptionsPanel(), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createOptionsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder("Settings"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        spyModeEnabled = new JCheckBox("Spy Mode");
        markerModeEnabled = new JCheckBox("Marker Mode");
        keepRecordingsEnabled = new JCheckBox("Keep Recordings");
        saveToDesktopEnabled = new JCheckBox("Save to Desktop");
        customDirectoryEnabled = new JCheckBox("Custom Directory");
        openOSSettingsEnabled = new JCheckBox("Open OS Recording Settings");
        easterEggEnabled = new JCheckBox("Easter Egg");
        darkModeEnabled = new JCheckBox("Dark Mode");

        spyModeEnabled.setToolTipText("Enable spy mode - automatically enables keep recordings and disables marker mode");
        markerModeEnabled.setToolTipText("Enable marker mode for audio marking");
        keepRecordingsEnabled.setToolTipText("Keep recorded audio files");
        saveToDesktopEnabled.setToolTipText("Save recordings to desktop folder");
        openOSSettingsEnabled.setToolTipText("Open OS recording settings on startup");
        easterEggEnabled.setToolTipText("Enable Easter egg messages");
        darkModeEnabled.setToolTipText("Enable dark mode for the interface");

        spyModeEnabled.addActionListener(e -> {
            updateSettings();
            logToConsole("Spy Mode: " + spyModeEnabled.isSelected());
            if (spyModeEnabled.isSelected()) {
                keepRecordingsEnabled.setSelected(true);
                markerModeEnabled.setSelected(false);
                logToConsole("Spy mode enabled - Keep recordings ON, Marker mode OFF");
            }
        });

        markerModeEnabled.addActionListener(e -> {
            updateSettings();
            logToConsole("Marker Mode: " + markerModeEnabled.isSelected());
            if (markerModeEnabled.isSelected()) {
                spyModeEnabled.setSelected(false);
                logToConsole("Marker mode enabled - Spy mode OFF");
            }
        });

        keepRecordingsEnabled.addActionListener(e -> {
            updateSettings();
            logToConsole("Keep Recordings: " + keepRecordingsEnabled.isSelected());
        });

        saveToDesktopEnabled.addActionListener(e -> {
            updateSettings();
            logToConsole("Save to Desktop: " + saveToDesktopEnabled.isSelected());
        });

        openOSSettingsEnabled.addActionListener(e -> {
            updateSettings();
            logToConsole("Open OS Settings: " + openOSSettingsEnabled.isSelected());
        });

        easterEggEnabled.addActionListener(e -> {
            updateSettings();
            logToConsole("Easter Egg: " + easterEggEnabled.isSelected());
        });

        darkModeEnabled.addActionListener(e -> {
            updateSettings();
            logToConsole("Dark Mode: " + darkModeEnabled.isSelected());
            setLookAndFeel(darkModeEnabled.isSelected());
            updateConsoleColors(darkModeEnabled.isSelected());
        });

        // TODO: CHANGE LOGIC SO THAT CUSTOM DIR AND DESKTOP ARE MUTUALLY EXCLUSIVE
        // TODO: CHANGE RECORDING CLASS TO USE EITHER CUSTOM CLASS OR DESKTOP
        chooseDirectoryButton = new JButton("Choose Directory");
        chooseDirectoryButton.setToolTipText("Choose a custom directory for recordings");
        chooseDirectoryButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int returnVal = chooser.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                Constants.CUSTOM_RECORDINGS_DIRECTORY = chooser.getSelectedFile().getAbsolutePath();
                saveToDesktopEnabled.setSelected(false);
                Settings.SAVE_RECORDINGS_TO_DESKTOP = false;
                Settings.SAVE_RECORDINGS_TO_CUSTOM_DIR = true;
                customDirectoryEnabled.setSelected(true);
                logToConsole("Custom recordings directory set to: " + Constants.CUSTOM_RECORDINGS_DIRECTORY);
            }
        });

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(spyModeEnabled, gbc);
        gbc.gridx = 1;
        panel.add(markerModeEnabled, gbc);
        gbc.gridx = 2;
        panel.add(keepRecordingsEnabled, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(saveToDesktopEnabled, gbc);
        gbc.gridx = 1;
        panel.add(customDirectoryEnabled, gbc);
        gbc.gridx = 2;
        panel.add(openOSSettingsEnabled, gbc);
        gbc.gridx = 3;
        panel.add(easterEggEnabled, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(darkModeEnabled, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(chooseDirectoryButton, gbc);

        return panel;
    }

    private JPanel createConsolePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Console Output"));

        consoleOutput = new JTextArea();
        consoleOutput.setEditable(false);
        consoleOutput.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        consoleOutput.setLineWrap(true);
        consoleOutput.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(consoleOutput);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setPreferredSize(new Dimension(850, 350));

        panel.add(scrollPane, BorderLayout.CENTER);

        JButton clearButton = new JButton("Clear Console");
        clearButton.addActionListener(e -> consoleOutput.setText(""));

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(clearButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createLoweredBevelBorder());

        statusLabel = new JLabel("Status: Stopped");
        recordingStatusLabel = new JLabel("Audio Recorder: Idle");
        listenerStatusLabel = new JLabel("Listener: Idle");

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
                        logToConsole("Error starting repeater: " + ex.getMessage());
                        log.error("Error starting repeater", ex);
                        stopRepeater();
                    });
                }
            });

            processorThread.start();

            isRunning = true;
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            statusLabel.setText("Status: Running");
            recordingStatusLabel.setText("Audio Recorder: Active");
            listenerStatusLabel.setText("Listener: Active");

        } catch (Exception ex) {
            logToConsole("Failed to start repeater: " + ex.getMessage());
            log.error("Failed to start repeater", ex);
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
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
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
            int result = JOptionPane.showConfirmDialog(
                    this,
                    "Repeater is still running. Stop it before exiting?",
                    "Confirm Exit",
                    JOptionPane.YES_NO_CANCEL_OPTION
            );

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
        saveToDesktopEnabled.setSelected(Settings.SAVE_RECORDINGS_TO_DESKTOP);
        openOSSettingsEnabled.setSelected(Settings.OPEN_OS_RECORDING_SETTINGS);
        easterEggEnabled.setSelected(Settings.EASTER_EGG);
        darkModeEnabled.setSelected(Settings.DARK_MODE);

        // Ensure look and feel is set correctly based on loaded settings
        setLookAndFeel(Settings.DARK_MODE);
        updateConsoleColors(Settings.DARK_MODE);

        logToConsole("Settings loaded from configuration");
    }

    private void updateSettings() {
        Settings.SPY_MODE = spyModeEnabled.isSelected();
        Settings.MARKER_MODE = markerModeEnabled.isSelected();
        Settings.KEEP_RECORDINGS = keepRecordingsEnabled.isSelected();
        Settings.SAVE_RECORDINGS_TO_DESKTOP = saveToDesktopEnabled.isSelected();
        Settings.OPEN_OS_RECORDING_SETTINGS = openOSSettingsEnabled.isSelected();
        Settings.EASTER_EGG = easterEggEnabled.isSelected();
        Settings.DARK_MODE = darkModeEnabled.isSelected();
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
            this.pack();
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
}
