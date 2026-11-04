package org.opentripplanner.core.model.resources;

import static com.google.common.truth.Truth.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class ResourceBundleSingletonTest {

  private static final String KEY = "unnamedStreet";

  @Test
  void translatesToRequestedLocale() {
    assertThat(ResourceBundleSingleton.INSTANCE.localize(KEY, Locale.GERMAN)).isEqualTo(
      "unbekannte Strasse"
    );
  }

  @Test
  void usesRootBundleForNullLocale() {
    assertThat(ResourceBundleSingleton.INSTANCE.localize(KEY, null)).isEqualTo("unnamed");
  }

  @Test
  void doesNotFallBackToDefaultLocale() {
    var defaultLocale = Locale.getDefault();
    try {
      Locale.setDefault(Locale.GERMAN);
      assertThat(ResourceBundleSingleton.INSTANCE.localize(KEY, Locale.CHINESE)).isEqualTo(
        "unnamed"
      );
    } finally {
      Locale.setDefault(defaultLocale);
    }
  }
}
