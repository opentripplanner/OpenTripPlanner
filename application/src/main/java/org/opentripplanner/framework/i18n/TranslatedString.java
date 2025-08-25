package org.opentripplanner.framework.i18n;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

/**
 * This is for translated strings for which translations are read from OSM or GTFS alerts.
 * <p>
 * This can be translated street names, GTFS alerts and notes.
 *
 * @author Hannes Junnila
 */
public class TranslatedString implements I18NString, Serializable {

  /**
   * Store all translations, so we don't get memory overhead for identical strings As this is
   * static, it isn't serialized when saving the graph.
   */
  private static final HashMap<Map<String, String>, I18NString> translationCache = new HashMap<>();

  private final Map<String, String> translations = new HashMap<>();
  private final int hashCode;

  private TranslatedString(Map<String, String> translations) {
    for (Map.Entry<String, String> i : translations.entrySet()) {
      if (i.getKey() == null) {
        this.translations.put(null, i.getValue());
      } else {
        this.translations.put(i.getKey().toLowerCase(), i.getValue());
      }
    }
    this.hashCode = Objects.hash(translations);
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
    return getI18NString(map, false, false);
  }

  /**
   * Gets an I18NString. If the translations only have a single value, return a NonTranslatedString,
   * otherwise a TranslatedString
   *
   * @param translations A Map of languages and translations, a null language is the default
   *                     translation
   * @param intern Should the resulting I18NString be interned. This should be used when calling
   *               this method during graph building, or when the string will be retained until the
   *               instance is shut down, as it will cause a memory leak otherwise.
   * @param forceTranslatedString Should the language information be kept, even when only a single
   *                              translation is provided. This is useful when the language
   *                              information is important or is presented to the user.
   */
  public static I18NString getI18NString(
    Map<String, String> translations,
    boolean intern,
    boolean forceTranslatedString
  ) {
    if (translations.isEmpty()) {
      throw new IllegalArgumentException("At least one translation must be provided");
    }
    if (translationCache.containsKey(translations)) {
      return translationCache.get(translations);
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
        translationCache.put(translations, ret);
      }
      return ret;
    }
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  public boolean equals(Object other) {
    return (
      (other instanceof TranslatedString) &&
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
   * @param locale Wanted locale
   * @return The translation in the wanted language if it exists, otherwise the default translation
   */
  @Override
  public String toString(Locale locale) {
    String language = null;
    if (locale != null) {
      language = locale.getLanguage().toLowerCase();
    }
    return translations.containsKey(language) ? translations.get(language) : toString();
  }
}
