package tz.deadparrot;

import lombok.extern.slf4j.Slf4j;

import javax.swing.*;

/**
 * DeadParrot Ham Radio Repeater
 *
 * @author TechnoZombie
 * @version 1.0
 */

@Slf4j
public class Main {
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--gui")) {
            SwingUtilities.invokeLater(() -> new DeadParrotGUI().setVisible(true));
        } else {
            Processor processor = new Processor();
            processor.init();
        }
    }
}