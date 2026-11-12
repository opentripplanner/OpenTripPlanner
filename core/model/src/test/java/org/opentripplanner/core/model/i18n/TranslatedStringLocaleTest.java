package org.opentripplanner.core.model.i18n;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Language codes with a region or a script, both in the data (GTFS translations.txt, OSM name:xx)
 * and in the requested locale.
 */
class TranslatedStringLocaleTest {

  /** A GTFS stop name: feed_lang "it", translations with and without region or script. */
  private static final I18NString GTFS = translated(
    "Hub",
    Map.ofEntries(
      entry("it", "Hub"),
      entry("en", "Hub EN"),
      entry("pt-BR", "Hub PT-BR"),
      entry("pt-PT", "Hub PT-PT"),
      entry("zh", "Hub ZH"),
      entry("zh-TW", "Hub ZH-TW"),
      entry("zh-Hant", "Hub ZH-HANT"),
      entry("es-ES", "Hub ES-ES"),
      entry("fr_CA", "Hub FR-CA")
    )
  );

  @ParameterizedTest(name = "{0} -> {1}")
  @CsvSource({
    // bare language codes: same results as before
    "it, Hub",
    "it-IT, Hub",
    "en, Hub EN",
    "en-US, Hub EN",
    "de, Hub",
    // the full tag first, then shorter forms
    "pt-BR, Hub PT-BR",
    "pt-PT, Hub PT-PT",
    "zh, Hub ZH",
    "zh-CN, Hub ZH",
    "zh-TW, Hub ZH-TW",
    "zh-Hant, Hub ZH-HANT",
    "zh-Hant-TW, Hub ZH-HANT",
    "es-ES, Hub ES-ES",
    "fr-CA, Hub FR-CA",
    // a shorter tag never returns a longer one
    "pt, Hub",
    "es, Hub",
    "es-MX, Hub",
    "fr, Hub",
  })
  void gtfsLikeData(String tag, String expected) {
    assertEquals(expected, GTFS.toString(Locale.forLanguageTag(tag)));
  }

  @Test
  void noLocale() {
    assertEquals("Hub", GTFS.toString(null));
    assertEquals("Hub", GTFS.toString(Locale.ROOT));
    assertEquals("Hub", GTFS.toString());
  }

  /**
   * OSM names in Tokyo: the untranslated name is Japanese and has no language key. A
   * romanization must not replace it for a Japanese reader.
   */
  @ParameterizedTest(name = "{0} -> {1}")
  @CsvSource({ "ja, 東京駅", "ja-JP, 東京駅", "ja-Latn, Tōkyō-eki", "en-US, Tokyo Station" })
  void osmRomanization(String tag, String expected) {
    var s = translated(
      "東京駅",
      Map.of("ja-Latn", "Tōkyō-eki", "ja_rm", "Tokyo-eki", "en", "Tokyo Station")
    );
    assertEquals(expected, s.toString(Locale.forLanguageTag(tag)));
  }

  /** Java reads the old code "iw" as "he": the data must be read the same way. */
  @ParameterizedTest(name = "{0}")
  @CsvSource({ "he", "iw", "he-IL" })
  void oldIsoCodeInData(String tag) {
    var s = translated("Station", Map.of("iw", "תחנה"));
    assertEquals("תחנה", s.toString(Locale.forLanguageTag(tag)));
  }

  @Test
  void normalizeLanguage() {
    assertNull(TranslatedString.normalizeLanguage(null));
    assertEquals("", TranslatedString.normalizeLanguage(""));
    assertEquals("en", TranslatedString.normalizeLanguage("EN"));
    assertEquals("pt-BR", TranslatedString.normalizeLanguage("pt_BR"));
    assertEquals("pt-BR", TranslatedString.normalizeLanguage("pt-br"));
    assertEquals("zh-Hant-TW", TranslatedString.normalizeLanguage("zh-hant-tw"));
    assertEquals("he", TranslatedString.normalizeLanguage("iw"));
    assertEquals("etymology:wikidata", TranslatedString.normalizeLanguage("etymology:wikidata"));
  }

  private static I18NString translated(String untranslated, Map<String, String> translations) {
    Map<String, String> map = new HashMap<>(translations);
    map.put(null, untranslated);
    return TranslatedString.getI18NString(map, false);
  }
}
