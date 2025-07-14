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
        SwingUtilities.invokeLater(() -> new DeadParrotGUI().setVisible(true));
    }
}