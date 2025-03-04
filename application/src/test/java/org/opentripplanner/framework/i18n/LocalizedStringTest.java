package org.opentripplanner.framework.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class LocalizedStringTest {

  @Test
  public void locale() {
    assertEquals(
      "corner of First and Second",
      new LocalizedString(
        "corner",
        TranslatedString.getI18NString("First", "de", "erste"),
        TranslatedString.getI18NString("Second", "de", "zweite")
      ).toString()
    );
  }

  @Test
  public void localeWithTranslation() {
    assertEquals(
      "Kreuzung Erste mit Zweite",
      new LocalizedString(
        "corner",
        TranslatedString.getI18NString("First", "de", "Erste"),
        TranslatedString.getI18NString("Second", "de", "Zweite")
      ).toString(Locale.GERMANY)
    );
  }

  @Test
  public void localeWithoutTranslation() {
    assertEquals(
      "corner of First and Second",
      new LocalizedString(
        "corner",
        TranslatedString.getI18NString("First", "de", "erste"),
        TranslatedString.getI18NString("Second", "de", "zweite")
      ).toString(Locale.CHINESE)
    );
  }

  @Test
  public void localeWithoutParams() {
    assertEquals("Destination", new LocalizedString("destination").toString());
  }

  @Test
  public void hungarianResource() {
    assertEquals("névtelen", new LocalizedString("unnamedStreet").toString(new Locale("hu")));
    assertEquals(
      "A (B része)",
      new LocalizedString(
        "partOf",
        new NonLocalizedString("A"),
        new NonLocalizedString("B")
      ).toString(new Locale("hu"))
    );
  }
}
