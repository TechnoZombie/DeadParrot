package tz.deadparrot.utils;

import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioLevelAnalyzerTest {

    @Test
    void utilityClassCanBeInstantiated() {
        assertNotNull(new AudioLevelAnalyzer());
    }

    @Test
    void detectsSamplesAboveSensitivityAndIgnoresValuesAtOrBelowIt() {
        assertFalse(AudioLevelAnalyzer.detectSound(new byte[]{(byte) 0x88, 0x13}, 2, 5000));
        assertTrue(AudioLevelAnalyzer.detectSound(new byte[]{(byte) 0x89, 0x13}, 2, 5000));
        assertTrue(AudioLevelAnalyzer.detectSound(new byte[]{0x77, (byte) 0xEC}, 2, 5000));
    }

    @Test
    void ignoresIncompleteSampleAndStopsAfterFindingSound() {
        assertFalse(AudioLevelAnalyzer.detectSound(new byte[]{0x00, 0x00, 0x7F}, 3, 0));
        assertFalse(AudioLevelAnalyzer.detectSound(new byte[0], 0, 0));
        assertTrue(AudioLevelAnalyzer.detectSound(
                new byte[]{0x00, 0x00, 0x01, 0x00, 0x00, 0x00}, 6, 0));
    }

    @Test
    void calculatesAverageAbsolute16BitSamplesInLittleEndianFormat() {
        AudioFormat format = pcmFormat(16, false);

        assertEquals(16.0, AudioLevelAnalyzer.calculateAudioLevel(
                new byte[]{0x10, 0x00, (byte) 0xF0, (byte) 0xFF}, 4, format));
    }

    @Test
    void calculatesAverageAbsolute16BitSamplesInBigEndianFormat() {
        AudioFormat format = pcmFormat(16, true);

        assertEquals(16.0, AudioLevelAnalyzer.calculateAudioLevel(
                new byte[]{0x00, 0x10, (byte) 0xFF, (byte) 0xF0}, 4, format));
    }

    @Test
    void calculatesUnsigned8BitLevelAndHandlesEmptyOrUnsupportedFormats() {
        assertEquals(85.0, AudioLevelAnalyzer.calculateAudioLevel(
                new byte[]{0, (byte) 128, (byte) 255}, 3, pcmFormat(8, false)));
        assertEquals(0.0, AudioLevelAnalyzer.calculateAudioLevel(
                new byte[0], 0, pcmFormat(8, false)));
        assertEquals(0.0, AudioLevelAnalyzer.calculateAudioLevel(
                new byte[]{0x01}, 1, pcmFormat(16, false)));
        assertEquals(0.0, AudioLevelAnalyzer.calculateAudioLevel(
                new byte[]{0x01, 0x02, 0x03}, 3, pcmFormat(24, false)));
    }

    private AudioFormat pcmFormat(int bitsPerSample, boolean bigEndian) {
        return new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 8000, bitsPerSample,
                1, bitsPerSample / 8, 8000, bigEndian);
    }
}
