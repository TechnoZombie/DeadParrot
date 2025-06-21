package tz.deadparrot.utils;

import javax.sound.sampled.AudioFormat;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

public class WaveWriterUtil {

    public void startWriteWavHeader(OutputStream out, AudioFormat format) throws IOException {
        writeWavHeader(out, format);
    }

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

    public void startUpdateWavHeader(File file, long dataSize) throws IOException {
        updateWavHeader(file, dataSize);
    }
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

    private void writeInt(OutputStream out, int value) throws IOException {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 24) & 0xFF);
    }

    private void writeShort(OutputStream out, int value) throws IOException {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
    }

    private void writeInt(RandomAccessFile raf, int value) throws IOException {
        raf.write(value & 0xFF);
        raf.write((value >> 8) & 0xFF);
        raf.write((value >> 16) & 0xFF);
        raf.write((value >> 24) & 0xFF);
    }
}
