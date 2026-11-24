package tz.deadparrot;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

interface AudioPlaybackProvider {
    AudioInputStream getAudioInputStream(File file)
            throws UnsupportedAudioFileException, IOException;

    Clip getClip() throws LineUnavailableException;

    AudioPlaybackProvider SYSTEM = new AudioPlaybackProvider() {
        @Override
        public AudioInputStream getAudioInputStream(File file)
                throws UnsupportedAudioFileException, IOException {
            return AudioSystem.getAudioInputStream(file);
        }

        @Override
        public Clip getClip() throws LineUnavailableException {
            return AudioSystem.getClip();
        }
    };
}
