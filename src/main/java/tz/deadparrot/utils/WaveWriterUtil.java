package tz.deadparrot.utils;

import javax.sound.sampled.AudioFormat;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

/**
 * Utility class for writing WAV audio file headers.
 *
 * <p>This class provides functionality to write standard WAV file headers
 * in little-endian format and update file size information after audio
 * data has been written. It supports PCM audio format encoding.</p>
 */
public class WaveWriterUtil {

    /**
     * Writes a WAV file header to the specified output stream.
     *
     * <p>This method writes a standard 44-byte WAV header with placeholder
     * values for file size and data size that should be updated later using
     * {@link #startUpdateWavHeader(File, long)}.</p>
     *
     * @param out the output stream to write the header to
     * @param format the audio format containing sample rate, channels, and bit depth
     * @throws IOException if an I/O error occurs while writing to the stream
     * @throws IllegalArgumentException if the audio format is not supported
     */
    public void startWriteWavHeader(OutputStream out, AudioFormat format) throws IOException {
        writeWavHeader(out, format);
    }

    /**
     * Writes the WAV header data to the output stream.
     *
     * @param out the output stream to write to
     * @param format the audio format specification
     * @throws IOException if writing to the stream fails
     */
    private void writeWavHeader(OutputStream out, AudioFormat format) throws IOException {
        int sampleRate = (int) format.getSampleRate();
        int channels = format.getChannels();
        int bitsPerSample = format.getSampleSizeInBits();
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        int blockAlign = channels * bitsPerSample / 8;

        // WAV header (44 bytes)
        out.write("RIFF".getBytes());
        writeInt(out, 36); // File size - 8 (will be updated later)
        out.write("WAVE".getBytes());
        out.write("fmt ".getBytes());
        writeInt(out, 16); // PCM header size
        writeShort(out, 1); // PCM format
        writeShort(out, channels);
        writeInt(out, sampleRate);
        writeInt(out, byteRate);
        writeShort(out, blockAlign);
        writeShort(out, bitsPerSample);
        out.write("data".getBytes());
        writeInt(out, 0); // Data size (will be updated later)
    }

    /**
     * Updates the WAV file header with the actual data size.
     *
     * <p>This method should be called after all audio data has been written
     * to update the file size and data size fields in the WAV header. It
     * modifies the file in-place using random access.</p>
     *
     * @param file     the WAV file to update
     * @param dataSize the actual size of the audio data in bytes
     * @throws IOException              if an I/O error occurs while updating the file
     * @throws IllegalArgumentException if dataSize is negative or exceeds maximum file size
     */
    public void startUpdateWavHeader(File file, long dataSize) throws IOException {
        updateWavHeader(file, dataSize);
    }

    /**
     * Updates the WAV header with actual file and data sizes.
     *
     * @param file     the file to update
     * @param dataSize the size of the audio data
     * @throws IOException if file access fails
     */
    private void updateWavHeader(File file, long dataSize) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            // Update file size at offset 4
            raf.seek(4);
            writeInt(raf, (int) (dataSize + 36));

            // Update data size at offset 40
            raf.seek(40);
            writeInt(raf, (int) dataSize);
        }
    }

    /**
     * Writes a 32-bit integer to the output stream in little-endian format.
     *
     * @param out   the output stream to write to
     * @param value the integer value to write
     * @throws IOException if writing to the stream fails
     */
    private void writeInt(OutputStream out, int value) throws IOException {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 24) & 0xFF);
    }

    /**
     * Writes a 16-bit short integer to the output stream in little-endian format.
     *
     * @param out   the output stream to write to
     * @param value the short value to write
     * @throws IOException if writing to the stream fails
     */
    private void writeShort(OutputStream out, int value) throws IOException {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
    }

    /**
     * Writes a 32-bit integer to the random access file in little-endian format.
     *
     * @param raf   the random access file to write to
     * @param value the integer value to write
     * @throws IOException if writing to the file fails
     */
    private void writeInt(RandomAccessFile raf, int value) throws IOException {
        raf.write(value & 0xFF);
        raf.write((value >> 8) & 0xFF);
        raf.write((value >> 16) & 0xFF);
        raf.write((value >> 24) & 0xFF);
    }
}