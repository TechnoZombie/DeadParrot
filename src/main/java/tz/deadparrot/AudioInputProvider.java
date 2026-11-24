package tz.deadparrot;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

interface AudioInputProvider {
    boolean isLineSupported(DataLine.Info lineInfo);

    TargetDataLine getLine(DataLine.Info lineInfo) throws LineUnavailableException;

    AudioInputProvider SYSTEM = new AudioInputProvider() {
        @Override
        public boolean isLineSupported(DataLine.Info lineInfo) {
            return AudioSystem.isLineSupported(lineInfo);
        }

        @Override
        public TargetDataLine getLine(DataLine.Info lineInfo) throws LineUnavailableException {
            return (TargetDataLine) AudioSystem.getLine(lineInfo);
        }
    };
}
