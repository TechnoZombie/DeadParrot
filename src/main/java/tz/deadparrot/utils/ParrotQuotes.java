package tz.deadparrot.utils;

import java.util.Random;

public class ParrotQuotes {
    private static final String OFF = "[OFF]: ";

    private static final String[] PARROT_LINES = {
            "This parrot is no more!",
            "This parrot has ceased to be!",
            "This parrot has expired and gone to meet its maker!",
            "This parrot is a stiff!",
            "Bereft of life, this parrot rests in peace!",
            "This parrot is pushing up the daisies!",
            "This parrot has kicked the bucket!",
            "This is an ex-parrot!",
            "This parrot is pining for the fjords!"
    };

    private static final Random random = new Random();

    public static String getRandomParrotLine() {
        int index = random.nextInt(PARROT_LINES.length);
        return OFF + PARROT_LINES[index];
    }
}