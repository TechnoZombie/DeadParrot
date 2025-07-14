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

@Slf4j
public class DeadParrotGUI extends JFrame {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    // GUI components
    private JTextArea consoleOutput;
    private JButton startButton;
    private JButton stopButton;

    private JCheckBox spyModeEnabled;
    private JCheckBox markerModeEnabled;
    private JCheckBox keepRecordingsEnabled;
    private JCheckBox saveToDesktopEnabled;
    private JCheckBox openOSSettingsEnabled;
    private JCheckBox easterEggEnabled;

    private JLabel statusLabel;
    private JLabel recordingStatusLabel;
    private JLabel listenerStatusLabel;

    // Runtime state
    private boolean isRunning = false;
    private Processor processor;
    private Thread processorThread;
    private GuiLogAppender guiLogAppender;

    public DeadParrotGUI() {
        initializeGUI();
        setupLogAppender();
        loadCurrentSettings();
    }

    private void initializeGUI() {
        setTitle("DeadParrot Ham Radio Repeater v1.0");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(900, 650);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

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

        JPanel buttonPanel = new JPanel(new FlowLayout());
        startButton = new JButton("START");
        stopButton = new JButton("STOP");

        startButton.setBackground(new Color(76, 175, 80));
        startButton.setForeground(Color.WHITE);
        startButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));

        stopButton.setBackground(new Color(244, 67, 54));
        stopButton.setForeground(Color.WHITE);
        stopButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        stopButton.setEnabled(false);

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
        panel.setBorder(new TitledBorder("Settings"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        spyModeEnabled = new JCheckBox("Spy Mode");
        markerModeEnabled = new JCheckBox("Marker Mode");
        keepRecordingsEnabled = new JCheckBox("Keep Recordings");
        saveToDesktopEnabled = new JCheckBox("Save to Desktop");
        openOSSettingsEnabled = new JCheckBox("Open OS Recording Settings");
        easterEggEnabled = new JCheckBox("Easter Egg");

        spyModeEnabled.setToolTipText("Enable spy mode - automatically enables keep recordings and disables marker mode");
        markerModeEnabled.setToolTipText("Enable marker mode for audio marking");
        keepRecordingsEnabled.setToolTipText("Keep recorded audio files");
        saveToDesktopEnabled.setToolTipText("Save recordings to desktop folder");
        openOSSettingsEnabled.setToolTipText("Open OS recording settings on startup");
        easterEggEnabled.setToolTipText("Enable Easter egg messages");

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

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(spyModeEnabled, gbc);
        gbc.gridx = 1;
        panel.add(markerModeEnabled, gbc);
        gbc.gridx = 2;
        panel.add(keepRecordingsEnabled, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(saveToDesktopEnabled, gbc);
        gbc.gridx = 1;
        panel.add(openOSSettingsEnabled, gbc);
        gbc.gridx = 2;
        panel.add(easterEggEnabled, gbc);

        return panel;
    }

    private JPanel createConsolePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Console Output"));

        consoleOutput = new JTextArea();
        consoleOutput.setEditable(false);
        consoleOutput.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        consoleOutput.setBackground(Color.BLACK);
        consoleOutput.setForeground(Color.GREEN);
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

        statusLabel.setForeground(Color.RED);

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
            statusLabel.setForeground(Color.GREEN);
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
            statusLabel.setForeground(Color.RED);
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

        logToConsole("Settings loaded from configuration");
    }

    private void updateSettings() {
        Settings.SPY_MODE = spyModeEnabled.isSelected();
        Settings.MARKER_MODE = markerModeEnabled.isSelected();
        Settings.KEEP_RECORDINGS = keepRecordingsEnabled.isSelected();
        Settings.SAVE_RECORDINGS_TO_DESKTOP = saveToDesktopEnabled.isSelected();
        Settings.OPEN_OS_RECORDING_SETTINGS = openOSSettingsEnabled.isSelected();
        Settings.EASTER_EGG = easterEggEnabled.isSelected();
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                if (log != null) log.error("Failed to set look and feel", e);
            }
            new DeadParrotGUI().setVisible(true);
        });
    }
}
