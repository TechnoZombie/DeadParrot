package tz.deadparrot.utils;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ParrotQuotesTest {

    private static final Set<String> QUOTES = Set.of(
            "This parrot is no more!",
            "This parrot has ceased to be!",
            "This parrot has expired and gone to meet its maker!",
            "This parrot is a stiff!",
            "Bereft of life, this parrot rests in peace!",
            "This parrot is pushing up the daisies!",
            "This parrot has kicked the bucket!",
            "This is an ex-parrot!",
            "This parrot is pining for the fjords!"
    );

    @Test
    void returnsAFormattedParrotQuote() {
        String quote = ParrotQuotes.getRandomParrotLine();

        assertTrue(quote.startsWith("[OFF]: "));
        assertTrue(QUOTES.contains(quote.substring("[OFF]: ".length())));
        assertTrue(new ParrotQuotes() != null);
    }
}
