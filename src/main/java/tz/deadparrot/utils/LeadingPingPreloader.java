package tz.deadparrot.utils;

import tz.deadparrot.Constants;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class LeadingPingPreloader {

    public File copyLeadingPingToTemp(String resourcePath) throws IOException {
        // Get the resource as an InputStream
        InputStream inputStream = getClass().getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new IOException(Constants.FILE_DOESNT_EXIST + ": " + resourcePath);
        }

        // Create a temporary file
        Path tempFile = Files.createTempFile("tempPing", ".wav");
        File tempFileAsFile = tempFile.toFile();
        tempFileAsFile.deleteOnExit(); // Delete the temp file when the JVM exits

        // Copy the resource to the temporary file
        try (OutputStream outputStream = new FileOutputStream(tempFileAsFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        return tempFileAsFile;
    }
}
