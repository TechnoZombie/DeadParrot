package tz.deadparrot.utils;

import tz.deadparrot.Constants;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * AudioResourcesPreloader
 * Preloads audio resources to temp to avoid path issues when running from a .jar package.
 */
public class AudioResourcesPreloader {

    public File copyLeadingPingToTemp() throws IOException {
        return copyResourceToTemp(Constants.LEADING_PING_FILE_PATH, "tempPing");
    }

    public File copyMarkerToTemp() throws IOException {
        return copyResourceToTemp(Constants.MARKER_FILE_PATH, "tempMarker");
    }

    private File copyResourceToTemp(String resourcePath, String tempPrefix) throws IOException {
        // Get the resource as an InputStream
        InputStream inputStream = getClass().getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new IOException(Constants.FILE_DOESNT_EXIST + ": " + resourcePath);
        }

        // Create a temporary file
        Path tempFile = Files.createTempFile(tempPrefix, Constants.FILENAME_EXTENSION);
        File tempFileAsFile = tempFile.toFile();
        tempFileAsFile.deleteOnExit();

        // Copy the resource to the temporary file
        try (InputStream is = inputStream;
             OutputStream os = Files.newOutputStream(tempFile)) {
            is.transferTo(os);
        }

        return tempFileAsFile;
    }
}