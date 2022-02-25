package org.opentripplanner.util;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}