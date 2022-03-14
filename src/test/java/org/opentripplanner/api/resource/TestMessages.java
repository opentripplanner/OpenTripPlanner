package org.opentripplanner.api.resource;

import org.junit.jupiter.api.Test;
import org.opentripplanner.api.common.Message;

import java.util.Locale;

import static java.util.Locale.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class TestMessages {

    @Test
    public void testLanguages() {
        Locale es = forLanguageTag("es");
        Locale hu = forLanguageTag("hu");
        Locale nl = forLanguageTag("nl");


        for (var m : Message.values()) {
            String name = m.name();
            System.out.println(name);
            
            assertFalse(m.get(ENGLISH).isBlank(), "Message code missing(ENGLISH): " + name);
            assertFalse(m.get(GERMANY).isBlank(), "Message code missing(GERMANY): " + name);
            assertFalse(m.get(FRENCH).isBlank(), "Message code missing(FRENCH): " + name);
            assertFalse(m.get(es).isBlank(), "Message code missing(ES): " + name);
            assertFalse(m.get(hu).isBlank(), "Message code missing(HU): " + name);
            assertFalse(m.get(nl).isBlank(), "Message code missing(NL): " + name);
        }
    }

    @Test
    public void testDefaultLanguage() {
        // Keep default locale so we can set it back after the test is done
        Locale sysDefaultLocale = Locale.getDefault();
        try {
            // Force default to make test work on non-US machines
            Locale.setDefault(Locale.GERMANY);

            for (var m : Message.values()) {
                assertEquals(m.get(), m.get(GERMANY));
            }
        }
        finally {
            Locale.setDefault(sysDefaultLocale);
        }
    }
}
