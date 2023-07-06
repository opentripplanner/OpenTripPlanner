package org.opentripplanner.framework.i18n;

import java.util.Locale;

/**
 * This interface is used when providing translations on server side. Sources: OSM tags with
 * multiple languages (like street names), or TranslatedString fields in GTFS-RT Alert messages.
 *
 * @author mabu
 */
public interface I18NString {
  /** true if the given value is not {@code null} or has at least one none white-space character. */
  public static boolean hasValue(I18NString value) {
    return value != null && !value.toString().isBlank();
  }

  /**
   * Returns default translation (english)
   */
  String toString();

  /**
   * Returns wanted translation
   *
   * @param locale Wanted locale
   */
  String toString(Locale locale);

  static I18NString assertHasValue(I18NString value) {
    if (value == null || value.toString().isBlank()) {
      throw new IllegalArgumentException(
        "Value can not be null, empty or just whitespace: " +
        (value == null ? "null" : "'" + value + "'")
      );
    }
    return value;
  }

  /**
   * Create an instance from a string.
   */
  static I18NString of(String value) {
    return new NonLocalizedString(value);
  }
}
