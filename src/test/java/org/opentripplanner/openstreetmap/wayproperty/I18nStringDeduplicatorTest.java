package org.opentripplanner.openstreetmap.wayproperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.LocalizedString;
import org.opentripplanner.framework.i18n.TranslatedString;

class I18nStringDeduplicatorTest {

  static List<Supplier<I18NString>> testCases() {
    return List.of(
      I18nStringDeduplicatorTest::nonLocalizedString,
      I18nStringDeduplicatorTest::localizedString,
      I18nStringDeduplicatorTest::translatedString
    );
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void deduplicate(Supplier<I18NString> makeString) {
    var cache = new I18nStringDeduplicator();

    var s1 = makeString.get();
    var s2 = makeString.get();
    assertEquals(s1, s2);
    assertNotSame(s1, s2);

    var d1 = cache.deduplicate(s1);
    assertSame(s1, d1);

    var d2 = cache.deduplicate(s2);
    assertEquals(s1, d2);

    assertSame(s1, d2);
    assertEquals(d1, d2);
    assertSame(d1, d2);

    var s3 = I18NString.of("Rue de Helmut Kohl");

    assertNotSame(s1, s3);
    assertNotSame(s2, s3);

    var d3 = cache.deduplicate(s3);
    assertEquals(d3, s3);
    assertSame(d3, s3);
  }

  @Test
  void nullString() {
    var cache = new I18nStringDeduplicator();
    assertNull(cache.deduplicate(null));
  }

  static I18NString nonLocalizedString() {
    return I18NString.of("test123");
  }

  static I18NString localizedString() {
    return new LocalizedString("name.sidewalk");
  }

  static I18NString translatedString() {
    return TranslatedString.getI18NString(
      "Helmut-Kohl-Straße",
      "de",
      "Helmut-Kohl-Straße",
      "se",
      "Helmuth Kohl Gattan "
    );
  }
}
