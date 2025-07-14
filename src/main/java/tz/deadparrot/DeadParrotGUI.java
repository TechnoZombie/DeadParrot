package tz.deadparrot;

import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.slf4j.LoggerFactory;

@Slf4j
public class DeadParrotGUI extends JFrame {
    private JTextArea consoleOutput;
    private JButton startButton;
    private JButton stopButton;
    private JScrollPane scrollPane;
    private GuiLogAppender guiLogAppender;

    // Toggle switches for various options
    private JCheckBox spyModeEnabled;
    private JCheckBox markerModeEnabled;
    private JCheckBox keepRecordingsEnabled;
    private JCheckBox saveToDesktopEnabled;
    private JCheckBox openOSSettingsEnabled;
    private JCheckBox easterEggEnabled;

    // Status indicators
    private JLabel statusLabel;
    private JLabel recordingStatusLabel;
    private JLabel listenerStatusLabel;

    // Program state
    private boolean isRunning = false;
    private Processor processor;
    private Thread processorThread;

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

        // Handle window closing
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleExit();
            }
        });

        // Create main layout
        setLayout(new BorderLayout());

        // Create top panel with controls
        JPanel topPanel = createControlPanel();
        add(topPanel, BorderLayout.NORTH);

        // Create console output area
        JPanel consolePanel = createConsolePanel();
        add(consolePanel, BorderLayout.CENTER);

        // Create status bar
        JPanel statusPanel = createStatusPanel();
        add(statusPanel, BorderLayout.SOUTH);
    }

    private void setupLogAppender() {
        // Create and configure the custom log appender
        guiLogAppender = new GuiLogAppender();
        guiLogAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        guiLogAppender.start();

        // Add appender to root logger
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(guiLogAppender);
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Repeater Controls"));

        // Start/Stop buttons panel
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

        startButton.addActionListener(new StartButtonListener());
        stopButton.addActionListener(new StopButtonListener());

        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);

        panel.add(buttonPanel, BorderLayout.NORTH);

        // Options panel
        JPanel optionsPanel = createOptionsPanel();
        panel.add(optionsPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createOptionsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder("Settings"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Initialize toggle switches based on your Settings class
        spyModeEnabled = new JCheckBox("Spy Mode");
        markerModeEnabled = new JCheckBox("Marker Mode");
        keepRecordingsEnabled = new JCheckBox("Keep Recordings");
        saveToDesktopEnabled = new JCheckBox("Save to Desktop");
        openOSSettingsEnabled = new JCheckBox("Open OS Recording Settings");
        easterEggEnabled = new JCheckBox("Easter Egg");

        // Add tooltips for better UX
        spyModeEnabled.setToolTipText("Enable spy mode - automatically enables keep recordings and disables marker mode");
        markerModeEnabled.setToolTipText("Enable marker mode for audio marking");
        keepRecordingsEnabled.setToolTipText("Keep recorded audio files");
        saveToDesktopEnabled.setToolTipText("Save recordings to desktop folder");
        openOSSettingsEnabled.setToolTipText("Open OS recording settings on startup");
        easterEggEnabled.setToolTipText("Enable Easter egg messages");

        // Add action listeners to update settings and log changes
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

        // Layout components
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

        scrollPane = new JScrollPane(consoleOutput);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setPreferredSize(new Dimension(850, 350));

        panel.add(scrollPane, BorderLayout.CENTER);

        // Add clear console button
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton clearButton = new JButton("Clear Console");
        clearButton.addActionListener(e -> consoleOutput.setText(""));
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
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String logEntry = "[" + timestamp + "] " + message + "\n";

        SwingUtilities.invokeLater(() -> {
            consoleOutput.append(logEntry);
            consoleOutput.setCaretPosition(consoleOutput.getDocument().getLength());
        });
    }

    private void loadCurrentSettings() {
        // Load current settings from your Settings class
        spyModeEnabled.setSelected(Settings.SPY_MODE);
        markerModeEnabled.setSelected(Settings.MARKER_MODE);
        keepRecordingsEnabled.setSelected(Settings.KEEP_RECORDINGS);
        saveToDesktopEnabled.setSelected(Settings.SAVE_RECORDINGS_TO_DESKTOP);
        openOSSettingsEnabled.setSelected(Settings.OPEN_OS_RECORDING_SETTINGS);
        easterEggEnabled.setSelected(Settings.EASTER_EGG);

        logToConsole("Settings loaded from configuration");
    }

    private void updateSettings() {
        // Update your Settings class with current GUI values
        Settings.SPY_MODE = spyModeEnabled.isSelected();
        Settings.MARKER_MODE = markerModeEnabled.isSelected();
        Settings.KEEP_RECORDINGS = keepRecordingsEnabled.isSelected();
        Settings.SAVE_RECORDINGS_TO_DESKTOP = saveToDesktopEnabled.isSelected();
        Settings.OPEN_OS_RECORDING_SETTINGS = openOSSettingsEnabled.isSelected();
        Settings.EASTER_EGG = easterEggEnabled.isSelected();
    }

    private class StartButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (!isRunning) {
                startRepeater();
            }
        }
    }

    private class StopButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (isRunning) {
                stopRepeater();
            }
        }
    }

    private void startRepeater() {
        // Update settings before starting
        updateSettings();

        try {
            processor = new Processor();

            // Run processor in separate thread to avoid blocking GUI
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
            // CANCEL_OPTION or closed dialog - do nothing
        } else {
            cleanup();
            System.exit(0);
        }
    }

    private void cleanup() {
        // Remove the log appender
        if (guiLogAppender != null) {
            Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            rootLogger.detachAppender(guiLogAppender);
            guiLogAppender.stop();
        }
    }

    // Method to allow external logging to the GUI console
    public void logMessage(String message) {
        logToConsole(message);
    }

    // Custom log appender to redirect SLF4J logs to GUI
    private class GuiLogAppender extends AppenderBase<ILoggingEvent> {
        @Override
        protected void append(ILoggingEvent event) {
            // Format the log message
            String logLevel = event.getLevel().toString();
            String loggerName = event.getLoggerName();
            String message = event.getFormattedMessage();

            // Simplify logger name for display
            String simpleName = loggerName.substring(loggerName.lastIndexOf('.') + 1);

            // Create formatted log entry
            String logEntry = String.format("[%s] %s - %s", logLevel, simpleName, message);

            // Add to GUI console
            SwingUtilities.invokeLater(() -> {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                String fullEntry = "[" + timestamp + "] " + logEntry + "\n";
                consoleOutput.append(fullEntry);
                consoleOutput.setCaretPosition(consoleOutput.getDocument().getLength());
            });
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                log.error("Failed to set look and feel", e);
            }

            new tz.deadparrot.DeadParrotGUI().setVisible(true);
        });
    }
}