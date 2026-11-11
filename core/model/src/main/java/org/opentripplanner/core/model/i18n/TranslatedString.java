package org.opentripplanner.core.model.i18n;

import static java.util.Map.entry;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/**
 * This is for translated strings for which translations are read from OSM or GTFS alerts.
 * <p>
 * This can be translated street names, GTFS alerts etc.
 *
 * @author Hannes Junnila
 */
public class TranslatedString implements I18NString, Serializable {

  /**
   * Store all translations, so we don't get memory overhead for identical strings As this is
   * static, it isn't serialized when saving the graph.
   */
  private static final HashMap<Map<String, String>, I18NString> TRANSLATION_CACHE = new HashMap<>();

  /** ISO 3166 region codes in lower case, to tell a region ({@code pt-br}) from other subtags. */
  private static final Set<String> ISO_REGIONS = Arrays.stream(Locale.getISOCountries())
    .map(region -> region.toLowerCase(Locale.ROOT))
    .collect(Collectors.toUnmodifiableSet());

  /**
   * The script a Chinese text uses in a region: Traditional characters in Taiwan, Hong Kong and
   * Macao, Simplified ones in mainland China, Singapore and Malaysia. Chinese is the only language
   * for which a requested region tells the script apart.
   */
  private static final Map<String, String> CHINESE_SCRIPT_BY_REGION = Map.ofEntries(
    entry("tw", "hant"),
    entry("hk", "hant"),
    entry("mo", "hant"),
    entry("cn", "hans"),
    entry("sg", "hans"),
    entry("my", "hans")
  );

  private final Map<String, String> translations = new HashMap<>();

  private TranslatedString(Map<String, String> translations) {
    for (Map.Entry<String, String> i : translations.entrySet()) {
      this.translations.put(normalizeLanguage(i.getKey()), i.getValue());
    }
  }

  public static I18NString getI18NString(String untranslated, String... translations) {
    if (translations.length % 2 != 0) {
      throw new IllegalStateException("An even number of translations must be supplied.");
    }

    var map = new HashMap<String, String>();
    map.put(null, untranslated);

    for (int i = 0; i < translations.length - 1; i += 2) {
      map.put(translations[i], translations[i + 1]);
    }
    return getI18NString(map, false);
  }

  /**
   * Gets a deduplicated I18NString. If the translations only have a single value, return a
   * NonLocalizedString, otherwise a TranslatedString. The resulting I18NString is interned for
   * memory efficiency.
   * <p>
   * This should be used when calling this method during graph building. This should not be
   * called from a real-time updater as this is not thread-safe and may cause a memory leak.
   *
   * @param translations          A Map of languages and translations, a null language is the
   *                              default translation
   * @param forceTranslatedString Should the language information be kept, even when only a single
   *                              translation is provided. This is useful when the language
   *                              information is important or is presented to the user.
   */
  public static I18NString getDeduplicatedI18NString(
    Map<String, String> translations,
    boolean forceTranslatedString
  ) {
    return getI18NString(translations, true, forceTranslatedString);
  }

  /**
   * Gets a non-deduplicated I18NString. If the translations only have a single value, return a
   * NonLocalizedString, otherwise a TranslatedString. The resulting I18NString is NOT interned.
   * <p>
   * This should be used from real-time updaters to avoid memory leaks. For graph building, use
   * {@link #getDeduplicatedI18NString(Map, boolean)} instead.
   *
   * @param translations          A Map of languages and translations, a null language is the
   *                              default translation
   * @param forceTranslatedString Should the language information be kept, even when only a single
   *                              translation is provided. This is useful when the language
   *                              information is important or is presented to the user.
   */
  public static I18NString getI18NString(
    Map<String, String> translations,
    boolean forceTranslatedString
  ) {
    return getI18NString(translations, false, forceTranslatedString);
  }

  /**
   * Gets an I18NString. If the translations only have a single value, return a NonLocalizedString,
   * otherwise a TranslatedString
   *
   * @param translations          A Map of languages and translations, a null language is the
   *                              default translation
   * @param intern                Should the resulting I18NString be interned. This should be used
   *                              when calling this method during graph building. This should not be
   *                              called from a real-time updater as this is not thread-safe and may
   *                              cause a memory leak.
   * @param forceTranslatedString Should the language information be kept, even when only a single
   *                              translation is provided. This is useful when the language
   *                              information is important or is presented to the user.
   */
  static I18NString getI18NString(
    Map<String, String> translations,
    boolean intern,
    boolean forceTranslatedString
  ) {
    if (translations.isEmpty()) {
      throw new IllegalArgumentException("At least one translation must be provided");
    }
    var t = TRANSLATION_CACHE.get(translations);
    if (t != null) {
      return t;
    } else {
      I18NString ret;
      // Check if we only have one name, even under multiple languages
      boolean allValuesEqual = new HashSet<>(translations.values()).size() == 1;
      var firstLanguage = translations.keySet().iterator().next();
      boolean onlySingleUntranslatedLanguage =
        translations.size() == 1 && (firstLanguage == null || firstLanguage.isBlank());
      if (forceTranslatedString && !onlySingleUntranslatedLanguage) {
        ret = new TranslatedString(translations);
      } else if (allValuesEqual) {
        ret = new NonLocalizedString(translations.values().iterator().next());
      } else {
        ret = new TranslatedString(translations);
      }
      if (intern) {
        TRANSLATION_CACHE.put(translations, ret);
      }
      return ret;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(translations);
  }

  @Override
  public boolean equals(Object other) {
    return (
      other instanceof TranslatedString &&
      this.translations.equals(((TranslatedString) other).translations)
    );
  }

  /**
   * @return The default translation
   */
  @Override
  public String toString() {
    return translations.containsKey(null)
      ? translations.get(null)
      : translations.values().iterator().next();
  }

  /**
   * @return The available languages
   */
  public Collection<String> getLanguages() {
    return translations.keySet();
  }

  /**
   * @return The available translations
   */
  public List<Entry<String, String>> getTranslations() {
    return new ArrayList<>(translations.entrySet());
  }

  /**
   * Returns the translation that best matches the wanted locale.
   * <p>
   * Language codes in the data (GTFS {@code translations.txt}, OSM {@code name:xx}, GTFS-RT
   * alerts) are BCP 47 tags that may carry a script or a region ({@code pt-BR}, {@code zh-Hant}),
   * and so do requested locales. The match goes from the most specific form to the least
   * specific one, like the "lookup" scheme of RFC 4647:
   * <ol>
   *   <li>the full tag ({@code pt-BR}), then the same tag without variant, region or script. For
   *   Chinese, a region without a script implies the script used there ({@code zh-HK} ->
   *   {@code zh-Hant}), and the regions that use the script come next ({@code zh-HK} or
   *   {@code zh-Hant} -> {@code zh-HK}, {@code zh-MO}, {@code zh-TW}): a reader of Traditional
   *   characters gets them rather than a bare {@code zh}, which is Simplified by convention;</li>
   *   <li>the bare language ({@code pt});</li>
   *   <li>if the script is not known, another region of the same language ({@code es} or
   *   {@code es-MX} -> {@code es-ES}), the first in alphabetical order. Keys with a script or a
   *   variant are never used as a fallback: a romanization ({@code ja-Latn}) must not replace a
   *   name in the local script;</li>
   *   <li>the default translation.</li>
   * </ol>
   * With bare language codes only ({@code en}, {@code de}) the result is the same as matching the
   * language of the locale alone.
   *
   * @param locale Wanted locale
   * @return The translation that best matches the wanted locale if one exists, otherwise the
   * default translation
   */
  @Override
  public String toString(Locale locale) {
    if (locale == null) {
      return toString();
    }
    LocaleLookup lookup = LocaleLookup.of(locale);
    if (lookup.language().isEmpty()) {
      return translations.containsKey("") ? translations.get("") : toString();
    }
    List<String> tags = lookup.tags();
    for (int i = 0; i < tags.size(); i++) {
      String translation = translations.get(tags.get(i));
      if (translation != null) {
        return translation;
      }
    }
    if (lookup.script().isEmpty()) {
      String otherRegion = otherRegionOfSameLanguage(lookup.regionPrefix());
      if (otherRegion != null) {
        return translations.get(otherRegion);
      }
    }
    return toString();
  }

  /**
   * The form in which language codes are stored: lower case, {@code _} read as {@code -}, and the
   * canonical BCP 47 tag when Java can parse it ({@code pt_BR} -> {@code pt-br}, {@code iw} ->
   * {@code he}: the same canonical form Java gives to the requested locale). A code Java cannot
   * parse is kept as it is, in lower case. {@code null} (the default translation) and blank codes
   * are kept unchanged.
   */
  @Nullable
  static String normalizeLanguage(@Nullable String language) {
    if (language == null || language.isBlank()) {
      return language;
    }
    String tag = language.trim().replace('_', '-');
    Locale locale = Locale.forLanguageTag(tag);
    String canonical = locale.getLanguage().isEmpty()
      ? tag
      : locale.stripExtensions().toLanguageTag();
    return canonical.toLowerCase(Locale.ROOT);
  }

  /**
   * The first key, in alphabetical order, made of {@code prefix} (the language and a dash) and a
   * region ({@code es-es}, {@code es-419}).
   */
  @Nullable
  private String otherRegionOfSameLanguage(String prefix) {
    String best = null;
    for (String key : translations.keySet()) {
      if (
        key != null &&
        key.startsWith(prefix) &&
        (best == null || key.compareTo(best) < 0) &&
        isRegion(key.substring(prefix.length()))
      ) {
        best = key;
      }
    }
    return best;
  }

  /** An ISO 3166 code ({@code br}) or a UN M.49 area ({@code 419}), in lower case. */
  private static boolean isRegion(String subtag) {
    if (subtag.length() == 2) {
      return ISO_REGIONS.contains(subtag);
    }
    return subtag.length() == 3 && subtag.chars().allMatch(c -> c >= '0' && c <= '9');
  }

  /**
   * What {@link #toString(Locale)} looks for, computed once per requested locale: the language,
   * the script (given, or implied by the region for Chinese), and the tags to try in order, down
   * to the bare language. The cache is bounded, since the locale comes from the request.
   */
  private record LocaleLookup(
    String language,
    String script,
    String regionPrefix,
    List<String> tags
  ) {
    private static final int MAX_CACHED = 1000;
    private static final Map<Locale, LocaleLookup> CACHE = new ConcurrentHashMap<>();

    static LocaleLookup of(Locale locale) {
      LocaleLookup lookup = CACHE.get(locale);
      if (lookup == null) {
        lookup = create(locale);
        if (CACHE.size() < MAX_CACHED) {
          CACHE.put(locale, lookup);
        }
      }
      return lookup;
    }

    private static LocaleLookup create(Locale locale) {
      String language = locale.getLanguage().toLowerCase(Locale.ROOT);
      String region = locale.getCountry().toLowerCase(Locale.ROOT);
      String givenScript = locale.getScript().toLowerCase(Locale.ROOT);
      boolean chinese = "zh".equals(language);
      String script =
        givenScript.isEmpty() && chinese
          ? CHINESE_SCRIPT_BY_REGION.getOrDefault(region, "")
          : givenScript;
      var tags = new LinkedHashSet<String>();
      if (!language.isEmpty()) {
        tags.add(locale.stripExtensions().toLanguageTag().toLowerCase(Locale.ROOT));
        if (!script.isEmpty() && !region.isEmpty()) {
          tags.add(language + "-" + script + "-" + region);
        }
        if (!script.isEmpty()) {
          tags.add(language + "-" + script);
        }
        if (!region.isEmpty()) {
          tags.add(language + "-" + region);
        }
        if (chinese && !script.isEmpty()) {
          CHINESE_SCRIPT_BY_REGION.entrySet()
            .stream()
            .filter(e -> e.getValue().equals(script))
            .map(e -> language + "-" + e.getKey())
            .sorted()
            .forEach(tags::add);
        }
        tags.remove(language);
        tags.add(language);
      }
      return new LocaleLookup(language, script, language + "-", List.copyOf(tags));
    }
  }
}
