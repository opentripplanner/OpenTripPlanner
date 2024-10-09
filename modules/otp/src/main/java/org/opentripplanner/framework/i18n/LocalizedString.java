package org.opentripplanner.framework.i18n;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import org.opentripplanner.framework.resources.ResourceBundleSingleton;

/**
 * This is used to localize strings for which localization are known beforehand. Those are local
 * names for: unanamedStreet, corner of x and y, path, bike_path etc.
 * <p>
 * Translations are in src/main/resources/WayProperties_lang.properties and
 * internals_lang.properties
 * <p>
 * locale is set in request.
 *
 * @author mabu
 */
public class LocalizedString implements I18NString, Serializable {

  private static final Pattern patternMatcher = Pattern.compile("\\{(.*?)}");

  //Key which specifies translation
  private final String key;
  //Values with which tagNames are replaced in translations.
  private final I18NString[] params;

  /**
   * Creates String which can be localized
   *
   * @param key key of translation for this way set in OSM and translations read from properties
   *            files
   */
  public LocalizedString(String key) {
    this.key = key;
    this.params = null;
  }

  /**
   * Creates String which can be localized
   *
   * @param key    key of translation for this way set in OSM and translations read from properties
   *               files
   * @param params Values with which tagNames are replaced in translations.
   */
  public LocalizedString(String key, I18NString... params) {
    this.key = key;
    this.params = params;
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(key);
    result = 31 * result + Arrays.hashCode(params);
    return result;
  }

  @Override
  public boolean equals(Object other) {
    return (
      other instanceof LocalizedString &&
      key.equals(((LocalizedString) other).key) &&
      Arrays.equals(params, ((LocalizedString) other).params)
    );
  }

  /**
   * Returns translated string in default locale with tag_names replaced with values
   * <p>
   * Default locale is defaultLocale from {@link ResourceBundleSingleton}
   */
  @Override
  public String toString() {
    return this.toString(null);
  }

  /**
   * Returns translated string in wanted locale with tag_names replaced with values
   */
  @Override
  public String toString(Locale locale) {
    if (this.key == null) {
      return null;
    }
    //replaces {name}, {ref} etc with %s to be used as parameters
    //in string formatting with values from way tags values
    String translation = ResourceBundleSingleton.INSTANCE.localize(this.key, locale);
    if (this.params != null) {
      translation = patternMatcher.matcher(translation).replaceAll("%s");
      return String.format(
        translation,
        Arrays.stream(params).map(i -> i.toString(locale)).toArray(Object[]::new)
      );
    } else {
      return translation;
    }
  }
}
