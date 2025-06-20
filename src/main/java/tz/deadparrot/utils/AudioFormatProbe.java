package tz.deadparrot.utils;

import javax.sound.sampled.*;

public class AudioFormatProbe {
    public static void main(String[] args) {
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();

        for (Mixer.Info mixerInfo : mixers) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            Line.Info[] lineInfos = mixer.getTargetLineInfo();

            if (lineInfos.length == 0) continue; // Not an input mixer

            System.out.println("🎤 Mixer: " + mixerInfo.getName());

            for (Line.Info lineInfo : lineInfos) {
                if (!TargetDataLine.class.isAssignableFrom(lineInfo.getLineClass())) continue;

                try {
                    TargetDataLine line = (TargetDataLine) mixer.getLine(lineInfo);
                    for (AudioFormat format : getCommonFormats()) {
                        try {
                            line.open(format);
                            System.out.println("  ✅ Supported: " + format);
                            line.close();
                        } catch (Exception e) {
                            System.out.println("  ❌ Not supported: " + format);
                        }
                    }
                } catch (LineUnavailableException e) {
                    System.out.println("  ❌ Line unavailable: " + e.getMessage());
                }
            }
        }
    }

    private static AudioFormat[] getCommonFormats() {
        return new AudioFormat[]{
                new AudioFormat(44100f, 16, 1, true, false),
                new AudioFormat(44100f, 16, 2, true, false),
                new AudioFormat(22050f, 16, 1, true, false),
                new AudioFormat(16000f, 16, 1, true, false),
                new AudioFormat(8000f, 16, 1, true, false),
                new AudioFormat(44100f, 8, 1, true, false)
        };
    }
}
