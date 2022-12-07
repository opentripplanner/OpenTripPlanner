package org.opentripplanner.framework.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class TranslatedStringTest {

  @Test
  public void testGetI18NString() {
    HashMap<String, String> translations = new HashMap<>();

    translations.put(null, "Test");
    I18NString string1 = TranslatedString.getI18NString(translations, false, false);
    assertEquals("Test", string1.toString());
    assertEquals("Test", string1.toString(Locale.ENGLISH));
    assertTrue(string1 instanceof NonLocalizedString);

    translations.put("en", "Test");
    I18NString string2 = TranslatedString.getI18NString(translations, false, false);
    assertEquals("Test", string2.toString());
    assertEquals("Test", string2.toString(Locale.ENGLISH));
    assertTrue(string2 instanceof NonLocalizedString);

    translations.put("fi", "Testi");
    I18NString string3 = TranslatedString.getI18NString(translations, true, false);
    assertEquals("Test", string3.toString());
    assertEquals("Test", string3.toString(Locale.ENGLISH));
    assertEquals("Testi", string3.toString(new Locale("fi")));
    assertTrue(string3 instanceof TranslatedString);

    HashMap<String, String> translations2 = new HashMap<>();
    translations2.put(null, "Test");
    translations2.put("en", "Test");
    translations2.put("fi", "Testi");
    I18NString string4 = TranslatedString.getI18NString(translations2, true, false);
    assertSame(string3, string4);
  }

  @Test
  public void testForceTransltaedString() {
    HashMap<String, String> translations = new HashMap<>();

    translations.put("en", "Test");

    I18NString translatedString = TranslatedString.getI18NString(translations, false, true);
    assertEquals("Test", translatedString.toString());
    assertEquals("Test", translatedString.toString(Locale.ENGLISH));
    assertTrue(translatedString instanceof TranslatedString);

    I18NString emptyLanguageString = TranslatedString.getI18NString(
      Map.of("", "Test"),
      false,
      true
    );
    assertEquals("Test", emptyLanguageString.toString());
    assertTrue(emptyLanguageString instanceof NonLocalizedString);
  }
}
