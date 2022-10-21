package org.opentripplanner.transit.model.basic;

import java.util.Locale;

/**
 * This interface is used when providing translations on server side. Sources: OSM tags with
 * multiple languages (like street names), or TranslatedString fields in GTFS-RT Alert messages.
 *
 * @author mabu
 */
public interface I18NString {
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

  public static I18NString assertHasValue(I18NString value) {
    if (value == null || value.toString().isBlank()) {
      throw new IllegalArgumentException(
        "Value can not be null, empty or just whitespace: " +
        (value == null ? "null" : "'" + value + "'")
      );
    }
    return value;
  }
}
