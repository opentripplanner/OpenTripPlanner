package org.opentripplanner.framework.time;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;

public class LocalDateUtils {

  private LocalDateUtils() {}

  /**
   * Parse a string as a local date or a duration relative to the given local date.
   * <p>
   * If today is {@code 2022-11-30}, then:
   * <ul>
   * <li>{@code 2022-01-15 is 2022-01-15} (absolute)</li>
   * <li>{@code "0d" or "P0d" is 2022-11-30}</li>
   * <li>{@code "3d" or "P3d" is 2022-12-03}</li>
   * <li>{@code "-P1Y1D" is 2021-11-29} (minus one year and one day)</li>
   * <li>{@code "P1Y-1D" is 2023-11-29} (plus one year, minus one day)</li>
   * </ul>
   *
   * The Period is parsed using {@link Period#parse(CharSequence)}.
   */
  public static LocalDate asRelativeLocalDate(String text, LocalDate timeZero)
    throws DateTimeParseException {
    if (text.startsWith("-") || text.startsWith("P")) {
      return timeZero.plus(Period.parse(text));
    } else {
      return LocalDate.parse(text);
    }
  }
}
