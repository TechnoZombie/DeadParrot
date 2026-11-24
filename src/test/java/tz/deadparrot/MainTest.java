package tz.deadparrot;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MainTest {

    private final boolean originalUseGui = Settings.USE_GUI;
    private final Runnable originalGuiLauncher = Main.guiLauncher;
    private final Runnable originalProcessorLauncher = Main.processorLauncher;

    @AfterEach
    void restoreState() {
        Settings.USE_GUI = originalUseGui;
        Main.guiLauncher = originalGuiLauncher;
        Main.processorLauncher = originalProcessorLauncher;
    }

    @Test
    void launchesTheSelectedApplicationMode() {
        AtomicInteger guiLaunches = new AtomicInteger();
        AtomicInteger processorLaunches = new AtomicInteger();
        Main.guiLauncher = guiLaunches::incrementAndGet;
        Main.processorLauncher = processorLaunches::incrementAndGet;

        Settings.USE_GUI = true;
        Main.main(new String[0]);
        Settings.USE_GUI = false;
        Main.main(new String[]{"ignored"});

        assertEquals(1, guiLaunches.get());
        assertEquals(1, processorLaunches.get());
    }
}
