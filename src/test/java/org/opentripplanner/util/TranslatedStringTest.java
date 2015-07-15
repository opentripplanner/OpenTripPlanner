package org.opentripplanner.util;

import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Locale;

public class TranslatedStringTest extends TestCase {

    public void testGetI18NString() throws Exception {
        HashMap<String, String> translations = new HashMap<>();

        translations.put(null, "Test");
        I18NString string1 = TranslatedString.getI18NString(translations);
        assertEquals("Test", string1.toString());
        assertEquals("Test", string1.toString(Locale.ENGLISH));
        assertTrue(string1 instanceof NonLocalizedString);

        translations.put("en", "Test");
        I18NString string2 = TranslatedString.getI18NString(translations);
        assertEquals("Test", string2.toString());
        assertEquals("Test", string2.toString(Locale.ENGLISH));
        assertTrue(string2 instanceof NonLocalizedString);

        translations.put("fi", "Testi");
        I18NString string3 = TranslatedString.getI18NString(translations);
        assertEquals("Test", string3.toString());
        assertEquals("Test", string3.toString(Locale.ENGLISH));
        assertEquals("Testi", string3.toString(new Locale("fi")));
        assertTrue(string3 instanceof TranslatedString);

        HashMap<String, String> translations2 = new HashMap<>();
        translations2.put(null, "Test");
        translations2.put("en", "Test");
        translations2.put("fi", "Testi");
        I18NString string4 = TranslatedString.getI18NString(translations2);
        assertTrue(string3 == string4);
    }

}