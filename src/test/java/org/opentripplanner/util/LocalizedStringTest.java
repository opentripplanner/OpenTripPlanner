package org.opentripplanner.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class LocalizedStringTest {

    @Test
    public void locale() {
        assertEquals(
                "corner of First and Second",
                new LocalizedString("corner", new String[] {"First", "Second"}).toString()
        );
    }

    @Test
    public void localeWithTranslation() {
        assertEquals(
                "Kreuzung First mit Second",
                new LocalizedString("corner", new String[] {"First", "Second"}).toString(Locale.GERMANY)
        );
    }

    @Test
    public void localeWithoutTranslation() {
        assertEquals(
                "corner of First and Second",
                new LocalizedString("corner", new String[] {"First", "Second"}).toString(Locale.CHINESE)
        );
    }

    @Test
    public void localeWithoutParams() {
        assertEquals(
                "Destination",
                new LocalizedString("destination").toString()
        );
    }

}