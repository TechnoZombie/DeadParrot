package tz.deadparrot;

import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.io.IOException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AudioProviderTest {

    @Test
    void systemInputProviderReportsAndOpensTheConfiguredLineWhenAvailable() {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, Settings.AUDIO_FORMAT);

        AudioInputProvider.SYSTEM.isLineSupported(info);
        try {
            TargetDataLine line = AudioInputProvider.SYSTEM.getLine(info);
            line.close();
        } catch (LineUnavailableException | IllegalArgumentException ignored) {
            // Audio hardware is optional in the test environment.
        }
    }

    @Test
    void systemPlaybackProviderReadsBundledWavAndReportsMissingFiles() throws Exception {
        URL resource = getClass().getResource("/leadingPing.wav");
        assertNotNull(resource);
        try (var stream = AudioPlaybackProvider.SYSTEM.getAudioInputStream(
                new java.io.File(resource.toURI()))) {
            assertNotNull(stream.getFormat());
        }

        assertThrows(IOException.class, () -> AudioPlaybackProvider.SYSTEM.getAudioInputStream(
                new java.io.File("missing-audio-file.wav")));
        try {
            AudioPlaybackProvider.SYSTEM.getClip();
        } catch (LineUnavailableException ignored) {
            // Audio output hardware is optional in the test environment.
        }
    }
}
