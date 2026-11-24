package tz.deadparrot;

import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.util.function.Supplier;

/**
 * DeadParrot Ham Radio Repeater
 *
 * @author TechnoZombie
 * @version 2.0
 */

@Slf4j
public class Main {
    static Runnable guiLauncher =
            () -> SwingUtilities.invokeLater(() -> new DeadParrotGUI().setVisible(true));
    static Supplier<Processor> processorFactory = Processor::new;
    static Runnable processorLauncher = () -> {
        Processor processor = processorFactory.get();
        processor.init();
    };

    public static void main(String[] args) {

        if (Settings.USE_GUI) {
            guiLauncher.run();
        } else {
            processorLauncher.run();
        }
    }
}