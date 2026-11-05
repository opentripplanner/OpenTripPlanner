package org.opentripplanner.core.model.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ResourceBundleSingletonTest {

  private static final String KEY = "unnamedStreet";

  @Test
  void translatesToRequestedLocale() {
    assertEquals(
      "unbekannte Strasse",
      ResourceBundleSingleton.INSTANCE.localize(KEY, Locale.GERMAN)
    );
  }

  @ParameterizedTest
  @ValueSource(strings = { "no", "no-NO", "nb", "nb-NO", "nn", "nn-NO" })
  void norwegianVariantsUseNorwegianBundle(String languageTag) {
    var locale = Locale.forLanguageTag(languageTag);
    assertEquals("gate uten navn", ResourceBundleSingleton.INSTANCE.localize(KEY, locale));
  }

  @Test
  void usesRootBundleForNullLocale() {
    assertEquals("unnamed", ResourceBundleSingleton.INSTANCE.localize(KEY, null));
  }

  @Test
  void doesNotFallBackToDefaultLocale() {
    var defaultLocale = Locale.getDefault();
    try {
      Locale.setDefault(Locale.GERMAN);
      assertEquals("unnamed", ResourceBundleSingleton.INSTANCE.localize(KEY, Locale.CHINESE));
    } finally {
      Locale.setDefault(defaultLocale);
    }
  }
}
