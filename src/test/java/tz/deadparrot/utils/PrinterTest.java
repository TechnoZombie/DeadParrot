package tz.deadparrot.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PrinterTest {

    @Test
    void printsCurrentSettings() {
        assertDoesNotThrow(Printer::printCurrentSettings);
        assertDoesNotThrow(Printer::printCurrentSettingsV2);
        assertDoesNotThrow(SettingsPersistence::new);
    }
}
