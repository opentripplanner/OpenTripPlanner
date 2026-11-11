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
 * Regional and script variants of the language codes, both in the data (GTFS translations.txt,
 * OSM name:xx) and in the requested locale.
 */
class TranslatedStringLocaleTest {

  /** A GTFS stop name: feed_lang "it", translations with and without region or script. */
  private static final I18NString GTFS = translated(
    "Hub",
    Map.ofEntries(
      entry("it", "Hub"),
      entry("en", "Hub EN"),
      entry("pt", "Hub PT"),
      entry("pt-BR", "Hub PT-BR"),
      entry("ptbr", "Hub PTBR"),
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
    "en-GB, Hub EN",
    "de, Hub",
    "ja-JP, Hub",
    // the most specific form wins
    "pt, Hub PT",
    "pt-BR, Hub PT-BR",
    "pt-PT, Hub PT",
    "ptbr, Hub PTBR",
    "zh, Hub ZH",
    "zh-TW, Hub ZH-TW",
    "zh-Hant, Hub ZH-HANT",
    "zh-Hant-TW, Hub ZH-HANT",
    // the script used in the region
    "zh-HK, Hub ZH-HANT",
    "zh-MO, Hub ZH-HANT",
    "zh-CN, Hub ZH",
    "zh-Hans, Hub ZH",
    // another region of the same language
    "es, Hub ES-ES",
    "es-MX, Hub ES-ES",
    "es-419, Hub ES-ES",
    "es-ES, Hub ES-ES",
    // '_' in the data
    "fr, Hub FR-CA",
    "fr-CA, Hub FR-CA",
    "fr-FR, Hub FR-CA",
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

  /** Only regions, no script: a script in the request picks a region that uses it. */
  @ParameterizedTest(name = "{0} -> {1}")
  @CsvSource({ "zh-Hant, TW", "zh-HK, TW", "zh-Hans, CN", "zh-SG, CN", "zh-TW, TW", "zh, CN" })
  void chineseRegionsOnly(String tag, String expected) {
    var s = translated("Hub", Map.of("zh-TW", "TW", "zh-CN", "CN"));
    assertEquals(expected, s.toString(Locale.forLanguageTag(tag)));
  }

  /**
   * A bare "zh" is usually Simplified Chinese: a request in the Traditional script, or from a
   * region that uses it, prefers a Traditional region to the bare language.
   */
  @ParameterizedTest(name = "{0} -> {1}")
  @CsvSource({ "zh, ZH", "zh-CN, ZH", "zh-Hans, ZH", "zh-TW, TW", "zh-HK, TW", "zh-Hant, TW" })
  void chineseScriptBeforeBareLanguage(String tag, String expected) {
    var s = translated("Hub", Map.of("zh", "ZH", "zh-TW", "TW"));
    assertEquals(expected, s.toString(Locale.forLanguageTag(tag)));
  }

  /**
   * OSM names in Tokyo: the untranslated name is Japanese and has no language key. A
   * romanization must not replace it for a Japanese reader: ja-Latn is a script and "rm" in the
   * old ja_rm tag is not a region.
   */
  @ParameterizedTest(name = "{0} -> {1}")
  @CsvSource({ "ja, 東京駅", "ja-JP, 東京駅", "ja-Latn, Tōkyō-eki", "en-US, Tokyo Station" })
  void osmRomanizationIsNoFallback(String tag, String expected) {
    var s = translated(
      "東京駅",
      Map.of("ja-Latn", "Tōkyō-eki", "ja_rm", "Tokyo-eki", "en", "Tokyo Station")
    );
    assertEquals(expected, s.toString(Locale.forLanguageTag(tag)));
  }

  @ParameterizedTest(name = "{0} -> {1}")
  @CsvSource({ "sr, Београд", "sr-RS, Београд", "sr-Latn, Beograd", "sr-Latn-RS, Beograd" })
  void serbianScripts(String tag, String expected) {
    var s = translated("Београд", Map.of("sr-Latn", "Beograd"));
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
    assertEquals("pt-br", TranslatedString.normalizeLanguage("pt_BR"));
    assertEquals("zh-hant-tw", TranslatedString.normalizeLanguage("zh-Hant-TW"));
    assertEquals("he", TranslatedString.normalizeLanguage("iw"));
    assertEquals("ptbr", TranslatedString.normalizeLanguage("ptbr"));
    assertEquals("etymology:wikidata", TranslatedString.normalizeLanguage("etymology:wikidata"));
  }

  private static I18NString translated(String untranslated, Map<String, String> translations) {
    Map<String, String> map = new HashMap<>(translations);
    map.put(null, untranslated);
    return TranslatedString.getI18NString(map, false);
  }
}
