package tz.deadparrot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConstantsSettingsTest {

    @Test
    void initializesApplicationConstantsAndDefaults() {
        assertNotNull(Constants.TIMESTAMP_FORMAT);
        assertNotNull(GUILabels.START_BUTTON);
        assertNotNull(Settings.AUDIO_FORMAT);
        assertNotNull(new Constants());
        assertNotNull(new GUILabels());
        assertNotNull(new Settings());
    }
}
