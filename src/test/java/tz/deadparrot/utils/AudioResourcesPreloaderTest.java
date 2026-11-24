package tz.deadparrot.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tz.deadparrot.Constants;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AudioResourcesPreloaderTest {

    @TempDir
    Path tempDir;

    @Test
    void copiesBundledAudioResourcesToTemporaryFiles() throws IOException {
        AudioResourcesPreloader preloader = new AudioResourcesPreloader();
        File ping = preloader.copyLeadingPingToTemp();
        File marker = preloader.copyMarkerToTemp();

        try {
            assertResourceMatches(Constants.LEADING_PING_FILE_PATH, ping);
            assertResourceMatches(Constants.MARKER_FILE_PATH, marker);
        } finally {
            Files.deleteIfExists(ping.toPath());
            Files.deleteIfExists(marker.toPath());
        }
    }

    @Test
    void reportsMissingResourcesAsAnIOException() {
        AudioResourcesPreloader preloader = new AudioResourcesPreloader();

        IOException exception = assertThrows(IOException.class,
                () -> preloader.copyResourceToTemp("/missing-test-resource.wav", "missing"));

        assertTrue(exception.getMessage().contains("/missing-test-resource.wav"));
    }

    private void assertResourceMatches(String resourcePath, File tempFile) throws IOException {
        try (InputStream resource = getClass().getResourceAsStream(resourcePath)) {
            assertTrue(tempFile.exists());
            assertTrue(tempFile.length() > 0);
            assertArrayEquals(resource.readAllBytes(), Files.readAllBytes(tempFile.toPath()));
        }
    }
}
