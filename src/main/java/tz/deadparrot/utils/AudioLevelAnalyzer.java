package tz.deadparrot.utils;

import javax.sound.sampled.AudioFormat;

public class AudioLevelAnalyzer {

    public static boolean detectSound(byte[] audioData, int length, int sensitivity) {
        for (int i = 0; i < length - 1; i += 2) {
            int sample = (audioData[i + 1] << 8) | (audioData[i] & 0xff);
            if (Math.abs(sample) > sensitivity) {
                return true;
            }
        }
        return false;
    }

    public static double calculateAudioLevel(byte[] buffer, int bytesRead, AudioFormat format) {
        if (format.getSampleSizeInBits() == 16) {
            long sum = 0;
            int sampleCount = 0;

            for (int i = 0; i < bytesRead - 1; i += 2) {
                short sample;
                if (format.isBigEndian()) {
                    sample = (short) ((buffer[i] << 8) | (buffer[i + 1] & 0xFF));
                } else {
                    sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));
                }
                sum += Math.abs(sample);
                sampleCount++;
            }

            return sampleCount > 0 ? (double) sum / sampleCount : 0;
        } else if (format.getSampleSizeInBits() == 8) {
            long sum = 0;
            for (int i = 0; i < bytesRead; i++) {
                sum += Math.abs(Byte.toUnsignedInt(buffer[i]) - 128);
            }
            return bytesRead > 0 ? (double) sum / bytesRead : 0;
        }

        return 0;
    }
}
