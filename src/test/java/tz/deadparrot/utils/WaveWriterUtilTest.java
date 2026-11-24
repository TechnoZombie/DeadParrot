package tz.deadparrot.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sound.sampled.AudioFormat;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class WaveWriterUtilTest {

    @TempDir
    Path tempDir;

    private final WaveWriterUtil writer = new WaveWriterUtil();
    private final AudioFormat format = new AudioFormat(8000, 16, 1, true, false);

    @Test
    void writesAStandardPcmWaveHeaderInLittleEndianOrder() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        writer.startWriteWavHeader(output, format);

        byte[] header = output.toByteArray();
        assertEquals(44, header.length);
        assertArrayEquals("RIFF".getBytes(), slice(header, 0, 4));
        assertEquals(36, readInt(header, 4));
        assertArrayEquals("WAVEfmt ".getBytes(), slice(header, 8, 16));
        assertEquals(16, readInt(header, 16));
        assertEquals(1, readShort(header, 20));
        assertEquals(1, readShort(header, 22));
        assertEquals(8000, readInt(header, 24));
        assertEquals(16000, readInt(header, 28));
        assertEquals(2, readShort(header, 32));
        assertEquals(16, readShort(header, 34));
        assertArrayEquals("data".getBytes(), slice(header, 36, 40));
        assertEquals(0, readInt(header, 40));
    }

    @Test
    void updatesFileAndDataSizesAfterAudioIsWritten() throws Exception {
        Path wavFile = tempDir.resolve("recording.wav");
        try (OutputStream output = Files.newOutputStream(wavFile)) {
            writer.startWriteWavHeader(output, format);
            output.write(new byte[123]);
        }

        writer.startUpdateWavHeader(wavFile.toFile(), 123);

        byte[] wav = Files.readAllBytes(wavFile);
        assertEquals(123, wav.length - 44);
        assertEquals(159, readInt(wav, 4));
        assertEquals(123, readInt(wav, 40));
    }

    private byte[] slice(byte[] bytes, int from, int to) {
        return java.util.Arrays.copyOfRange(bytes, from, to);
    }

    private int readShort(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF) | ((bytes[offset + 1] & 0xFF) << 8);
    }

    private int readInt(byte[] bytes, int offset) {
        return readShort(bytes, offset) | (readShort(bytes, offset + 2) << 16);
    }
}
