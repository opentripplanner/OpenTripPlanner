package org.opentripplanner.util;

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
  private static final HashMap<Map<String, String>, I18NString> intern = new HashMap<>();

  private final Map<String, String> translations = new HashMap<>();

  private TranslatedString(Map<String, String> translations) {
    for (Map.Entry<String, String> i : translations.entrySet()) {
      if (i.getKey() == null) {
        this.translations.put(null, i.getValue());
      } else {
        this.translations.put(i.getKey().toLowerCase(), i.getValue());
      }
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
    return getI18NString(map);
  }

  /**
   * Gets an interned I18NString. If the translations only have a single value, and no language set, return a
   * NonTranslatedString, otherwise a TranslatedString
   *
   * @param translations A Map of languages and translations, a null language is the default
   * translation
   */
  public static I18NString getI18NString(Map<String, String> translations) {
    if (intern.containsKey(translations)) {
      return intern.get(translations);
    } else {
      I18NString ret;
      // Check if we only have one name, even under multiple languages
      if (
        new HashSet<>(translations.values()).size() < 2 &&
        translations.keySet().iterator().next() == null
      ) {
        ret = new NonLocalizedString(translations.values().iterator().next());
      } else {
        ret = new TranslatedString(translations);
      }
      intern.put(translations, ret);

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
