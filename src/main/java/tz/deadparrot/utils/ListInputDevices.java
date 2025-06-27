package tz.deadparrot.utils;

import javax.sound.sampled.*;

/**
 * A utility class to list all available audio input devices (mixers)
 * on the system using the Java Sound API.
 * <p>
 * The class queries the available {@link Mixer} instances from the
 * {@link AudioSystem} and prints out the name and description of each mixer
 * that supports target lines (used for capturing audio input).
 * </p>
 */
public class ListInputDevices {

    /**
     * The main method retrieves and lists all audio input devices.
     * <p>
     * It uses {@link AudioSystem#getMixerInfo()} to retrieve all mixers,
     * then filters out only those that have target lines (i.e., devices
     * capable of capturing audio input such as microphones).
     * </p>
     *
     * @param args Command-line arguments (not used).
     */
    public static void main(String[] args) {
        // Get all available mixer info objects
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();

        // Iterate through each mixer info
        for (Mixer.Info info : mixerInfos) {
            // Get the corresponding mixer from the info
            Mixer mixer = AudioSystem.getMixer(info);

            // Get target line information (input lines)
            Line.Info[] targetLineInfos = mixer.getTargetLineInfo();

            // Only list mixers that support input (target) lines
            if (targetLineInfos.length > 0) {
                System.out.println("Mixer: " + info.getName() + " - " + info.getDescription());
                for (Line.Info lineInfo : targetLineInfos) {
                    System.out.println("  Line: " + lineInfo);
                }
            }
        }
    }
}
